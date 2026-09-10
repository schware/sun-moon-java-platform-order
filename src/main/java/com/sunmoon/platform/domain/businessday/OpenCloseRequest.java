package com.sunmoon.platform.domain.businessday;

import jakarta.validation.constraints.NotBlank;

/** {@code deviceId} is the 단말기 번호 that pressed 개점 or 마감. */
public record OpenCloseRequest(@NotBlank String storeId, @NotBlank String deviceId) {
}
