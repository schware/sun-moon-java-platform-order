package com.sunmoon.platform.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Logs the event and nothing else.
 *
 * <p>The fallback when Redis is not configured, so a machine with only a
 * JDK and a database still runs this service. Set
 * {@code sun-moon.events.redis=true} to publish for real — see
 * {@link RedisEventPublisher}.
 */
@Component
@ConditionalOnProperty(name = "sun-moon.events.redis", havingValue = "false", matchIfMissing = true)
public class InMemoryEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    @Override
    public void publish(OrderEvent event) {
        log.info("order-event: {} {} -> {} (order {})",
                event.customerId(), event.previousStatus(), event.status(), event.orderId());
    }
}
