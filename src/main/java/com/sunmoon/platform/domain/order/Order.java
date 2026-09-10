package com.sunmoon.platform.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * {@code acceptedBy} is the device id of the POS terminal that took the
 * order — null until one does. Kept here rather than in a separate table
 * because "who accepted this" is a property of the order, and answering
 * it should not need a join.
 *
 * <p>{@code businessDate} is the 영업일자 that was open when the order
 * arrived, which is not necessarily the calendar date of {@code placedAt}
 * — a shop trading past midnight is still on yesterday's business day.
 * Orders placed before 영업일 existed have none.
 */
public record Order(
        Long id,
        String storeId,
        String customerId,
        String menuName,
        BigDecimal amount,
        OrderStatus status,
        String acceptedBy,
        LocalDate businessDate,
        Instant placedAt,
        Instant updatedAt) {

    /**
     * A newly placed order. {@code storeId} is required — an order that
     * does not say which shop it is for cannot be routed to a counter, and
     * defaulting it would mean guessing.
     */
    public static Order placed(
            String storeId, String customerId, String menuName, BigDecimal amount, LocalDate businessDate) {
        Instant now = Instant.now();
        return new Order(
                null, storeId, customerId, menuName, amount, OrderStatus.PLACED, null, businessDate, now, now);
    }

    public Order withId(long newId) {
        return new Order(
                newId, storeId, customerId, menuName, amount, status, acceptedBy, businessDate, placedAt, updatedAt);
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
        return new Order(
                id, storeId, customerId, menuName, amount, next, accepter, businessDate, placedAt, Instant.now());
    }
}
