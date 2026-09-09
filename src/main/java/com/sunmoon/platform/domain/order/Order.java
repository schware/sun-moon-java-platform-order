package com.sunmoon.platform.domain.order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code acceptedBy} is the device id of the POS terminal that took the
 * order — null until one does. Kept here rather than in a separate table
 * because "who accepted this" is a property of the order, and answering
 * it should not need a join.
 */
public record Order(
        Long id,
        String storeId,
        String customerId,
        String menuName,
        BigDecimal amount,
        OrderStatus status,
        String acceptedBy,
        Instant placedAt,
        Instant updatedAt) {

    /**
     * A newly placed order. {@code storeId} is required — an order that
     * does not say which shop it is for cannot be routed to a counter, and
     * defaulting it would mean guessing.
     */
    public static Order placed(String storeId, String customerId, String menuName, BigDecimal amount) {
        Instant now = Instant.now();
        return new Order(null, storeId, customerId, menuName, amount, OrderStatus.PLACED, null, now, now);
    }

    public Order withId(long newId) {
        return new Order(newId, storeId, customerId, menuName, amount, status, acceptedBy, placedAt, updatedAt);
    }

    /**
     * Moves the order on, refusing anything the state machine does not
     * allow. {@code deviceId} is recorded only on the move that a terminal
     * makes — later transitions come from KDS and Delivery, which are not
     * terminals.
     */
    public Order movedTo(OrderStatus next, String deviceId) {
        if (!status.canMoveTo(next)) {
            throw new IllegalOrderTransitionException(id, status, next);
        }
        String accepter = next == OrderStatus.ACCEPTED ? deviceId : acceptedBy;
        return new Order(id, storeId, customerId, menuName, amount, next, accepter, placedAt, Instant.now());
    }
}
