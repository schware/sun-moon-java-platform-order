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
        String customerId,
        BigDecimal amount,
        OrderStatus status,
        String acceptedBy,
        Instant placedAt,
        Instant updatedAt) {

    /** A newly placed order: the channel supplies who and how much, nothing else. */
    public static Order placed(String customerId, BigDecimal amount) {
        Instant now = Instant.now();
        return new Order(null, customerId, amount, OrderStatus.PLACED, null, now, now);
    }

    public Order withId(long newId) {
        return new Order(newId, customerId, amount, status, acceptedBy, placedAt, updatedAt);
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
        return new Order(id, customerId, amount, next, accepter, placedAt, Instant.now());
    }
}
