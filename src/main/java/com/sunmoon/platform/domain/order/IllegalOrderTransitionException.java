package com.sunmoon.platform.domain.order;

/** Thrown when a caller asks for a move the order's current state does not allow. Answered as 409. */
public class IllegalOrderTransitionException extends RuntimeException {

    public IllegalOrderTransitionException(Long orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot move from " + from + " to " + to
                + " (allowed: " + from.allowedNext() + ")");
    }
}
