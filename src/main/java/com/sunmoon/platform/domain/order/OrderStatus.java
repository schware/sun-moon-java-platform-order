package com.sunmoon.platform.domain.order;

import java.util.Set;

/**
 * The order's life, and the only moves it is allowed to make.
 *
 * <p>Every transition here is driven by something outside this service —
 * a terminal accepting, the kitchen finishing, a rider delivering — which
 * is exactly why the rule lives in one place instead of in whichever
 * caller happens to arrive. A caller that asks for an illegal move is
 * told no, rather than quietly rewriting history.
 */
public enum OrderStatus {

    /** Placed through the order channel; no terminal has seen it yet. */
    PLACED,
    /** A POS terminal took it. */
    ACCEPTED,
    /** A POS terminal refused it, or no terminal was connected to offer it to. Terminal state. */
    REJECTED,
    /**
     * Nobody accepted it in time.
     *
     * <p>Separate from REJECTED on purpose: "the store said no" and "the
     * store never answered" are different facts, and an operator looking
     * at yesterday's orders needs to tell them apart. Terminal state.
     */
    EXPIRED,
    /** The kitchen finished it (KDS). */
    PRODUCED,
    /** Handed to delivery and on its way. */
    DELIVERING,
    /** Delivered. Terminal state. */
    COMPLETED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PLACED -> Set.of(ACCEPTED, REJECTED, EXPIRED);
            case ACCEPTED -> Set.of(PRODUCED);
            case PRODUCED -> Set.of(DELIVERING);
            case DELIVERING -> Set.of(COMPLETED);
            case REJECTED, EXPIRED, COMPLETED -> Set.of();
        };
    }

    public boolean canMoveTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
