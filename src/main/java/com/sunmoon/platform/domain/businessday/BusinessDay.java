package com.sunmoon.platform.domain.businessday;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 하루치 영업. Open from 개점 until 마감 — which may be after midnight, so
 * {@code businessDate} is not the calendar date of everything stamped
 * with it.
 *
 * <p>{@code openedBy} / {@code closedBy} are 단말기 번호, because 개점 and
 * 마감 are pressed on a terminal and the sales key is
 * (매출일자, 매장, 단말기번호, 거래번호).
 */
public record BusinessDay(
        String storeId,
        LocalDate businessDate,
        Instant openedAt,
        String openedBy,
        Instant closedAt,
        String closedBy) {

    public static BusinessDay opened(String storeId, LocalDate businessDate, String deviceId) {
        return new BusinessDay(storeId, businessDate, Instant.now(), deviceId, null, null);
    }

    public boolean isOpen() {
        return closedAt == null;
    }

    /** True once the shop's own calendar has moved past the day it is still trading on. */
    public boolean isStale(LocalDate today) {
        return isOpen() && businessDate.isBefore(today);
    }
}
