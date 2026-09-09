package com.sunmoon.platform.infrastructure.messaging;

public interface EventPublisher {

    void publish(OrderEvent event);
}
