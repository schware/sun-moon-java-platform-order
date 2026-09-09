package com.sunmoon.platform.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order events on the {@code order-events} Redis channel, where
 * the Device Server picks them up and pushes them to terminals.
 *
 * <p>Pub/Sub, deliberately: a terminal that is offline when an order is
 * placed should <em>not</em> receive it on reconnect as if it were new —
 * it asks Order for the current list instead. Fire-and-forget matches
 * that. Anything needing durability belongs in the database, which is
 * where the order already is.
 *
 * <p>Only active when {@code sun-moon.events.redis=true}, so a machine
 * with no Redis still runs the service on the logging publisher.
 */
@Component
@ConditionalOnProperty(name = "sun-moon.events.redis", havingValue = "true")
public class RedisEventPublisher implements EventPublisher {

    public static final String CHANNEL = "order-events";

    private static final Logger log = LoggerFactory.getLogger(RedisEventPublisher.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisEventPublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(OrderEvent event) {
        try {
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event));
            log.debug("published to {}: order {} -> {}", CHANNEL, event.orderId(), event.status());
        } catch (Exception e) {
            // Rethrown so OrderService's circuit breaker sees it; the order
            // itself is already committed either way.
            throw new IllegalStateException("Failed to publish order event", e);
        }
    }
}
