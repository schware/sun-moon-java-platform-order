package com.sunmoon.platform.domain.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The state machine is the one piece of this service that is genuinely
 * business logic rather than plumbing, so it is tested directly rather
 * than only through HTTP.
 */
class OrderStatusTest {

    @Test
    void walksTheHappyPathAllTheWay() {
        Order order = Order.placed("cust-1", new BigDecimal("42.50")).withId(1L);
        assertEquals(OrderStatus.PLACED, order.status());

        order = order.movedTo(OrderStatus.ACCEPTED, "pos-01");
        assertEquals("pos-01", order.acceptedBy());

        order = order.movedTo(OrderStatus.PRODUCED, null);
        order = order.movedTo(OrderStatus.DELIVERING, null);
        order = order.movedTo(OrderStatus.COMPLETED, null);

        assertTrue(order.status().isFinal());
        // The terminal that accepted it is still recorded five moves later.
        assertEquals("pos-01", order.acceptedBy());
    }

    @Test
    void refusesToSkipAhead() {
        Order placed = Order.placed("cust-1", BigDecimal.ONE).withId(1L);

        assertThrows(IllegalOrderTransitionException.class,
                () -> placed.movedTo(OrderStatus.PRODUCED, null));
        assertThrows(IllegalOrderTransitionException.class,
                () -> placed.movedTo(OrderStatus.COMPLETED, null));
    }

    @Test
    void refusesToGoBackwards() {
        Order accepted = Order.placed("cust-1", BigDecimal.ONE).withId(1L)
                .movedTo(OrderStatus.ACCEPTED, "pos-01");

        assertThrows(IllegalOrderTransitionException.class,
                () -> accepted.movedTo(OrderStatus.PLACED, null));
    }

    @Test
    void aRejectedOrderIsOverForGood() {
        Order rejected = Order.placed("cust-1", BigDecimal.ONE).withId(1L)
                .movedTo(OrderStatus.REJECTED, "pos-01");

        assertTrue(rejected.status().isFinal());
        for (OrderStatus next : OrderStatus.values()) {
            assertFalse(rejected.status().canMoveTo(next), "REJECTED should not reach " + next);
        }
    }

    @Test
    void rejectingIsOnlyPossibleBeforeAcceptance() {
        Order accepted = Order.placed("cust-1", BigDecimal.ONE).withId(1L)
                .movedTo(OrderStatus.ACCEPTED, "pos-01");

        assertThrows(IllegalOrderTransitionException.class,
                () -> accepted.movedTo(OrderStatus.REJECTED, "pos-01"));
    }

    @Test
    void onlyTheAcceptanceRecordsATerminal() {
        Order produced = Order.placed("cust-1", BigDecimal.ONE).withId(1L)
                .movedTo(OrderStatus.ACCEPTED, "pos-01")
                // KDS is a service, not a terminal — it must not overwrite this.
                .movedTo(OrderStatus.PRODUCED, "kds-99");

        assertEquals("pos-01", produced.acceptedBy());
    }
}
