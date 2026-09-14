package com.sunmoon.platform.domain.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One 영업일자 × 매장's worth of orders, counted.
 *
 * <p>{@code salesAmount} deliberately counts only orders that were
 * accepted — REJECTED ("the store said no") and EXPIRED ("the store never
 * answered") are orders that happened but not sales that happened, and an
 * operator reading a day's total needs those to be different numbers. They
 * are still returned as their own counts so the day can be read honestly:
 * a store that refused half of what came in should be able to see that.
 *
 * <p>PLACED is counted in {@code orderCount} but nowhere else. It is not
 * yet either outcome — it is still sitting on a POS screen.
 *
 * <p>This is the shape BO's 매출 조회 screen reads. It is <em>not</em> the
 * {@code Tr_header/Detail} structure the 설계서 calls for — that needs the
 * 거래번호 that nothing mints yet. This is a read model over the orders
 * that exist today, and it keeps working once those tables arrive.
 */
public record SalesSummary(
        LocalDate businessDate,
        String storeId,
        long orderCount,
        long acceptedCount,
        long rejectedCount,
        long expiredCount,
        BigDecimal salesAmount) {
}
