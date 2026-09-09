package com.sunmoon.platform.infrastructure.messaging;

import com.sunmoon.platform.domain.order.Order;
import com.sunmoon.platform.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What the rest of the system hears when an order moves.
 *
 * <p>Carries {@code storeId} because the subscriber routes on it: an
 * order belongs to one shop's counter, and a screen in another branch
 * must never see it.
 *
 * <p>Carries {@code previousStatus} so a subscriber can tell a genuine
 * transition from a replay: "it is ACCEPTED now" is not the same fact as
 * "it just became ACCEPTED", and the terminals care about the second one.
 * Null means the order was just placed.
 */
public record OrderEvent(
        Long orderId,
        String storeId,
        String customerId,
        String menuName,
        BigDecimal amount,
        OrderStatus status,
        OrderStatus previousStatus,
        String acceptedBy,
        Instant occurredAt) {

    public static OrderEvent of(Order order, OrderStatus previousStatus) {
        return new OrderEvent(
                order.id(), order.storeId(), order.customerId(), order.menuName(), order.amount(),
                order.status(), previousStatus, order.acceptedBy(), Instant.now());
    }
}
