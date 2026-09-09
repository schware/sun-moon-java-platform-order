package com.sunmoon.platform.domain.order;

import jakarta.validation.constraints.NotNull;

/**
 * {@code deviceId} is recorded only for the terminal's own move
 * (PLACED → ACCEPTED); the later transitions come from KDS and Delivery,
 * which are services rather than terminals.
 */
public record ChangeStatusRequest(@NotNull OrderStatus status, String deviceId) {
}
