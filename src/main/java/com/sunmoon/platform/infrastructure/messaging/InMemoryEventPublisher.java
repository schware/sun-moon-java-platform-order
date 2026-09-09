package com.sunmoon.platform.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs the event and nothing else.
 *
 * <p>This is a placeholder, and the flow it is standing in for is real:
 * a placed order has to reach the POS terminals, and an accepted one has
 * to reach KDS. Redis Pub/Sub is the intended carrier — Redis already runs
 * on the host — and this stays the default until the Device Server exists
 * to subscribe, so that nothing depends on a channel with no listener.
 */
@Component
public class InMemoryEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    @Override
    public void publish(OrderEvent event) {
        log.info("order-event: {} {} -> {} (order {})",
                event.customerId(), event.previousStatus(), event.status(), event.orderId());
    }
}
