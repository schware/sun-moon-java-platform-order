package com.sunmoon.platform.domain.sales;

import com.sunmoon.platform.domain.order.Order;

import java.time.LocalDate;
import java.util.List;

/**
 * A read model over the same {@code orders} table
 * {@link com.sunmoon.platform.domain.order.OrderRepository} writes to —
 * separate because it asks a different kind of question. That one fetches
 * an order to move it on; this one never writes and never loads a row it
 * does not need to count.
 */
public interface SalesRepository {

    /** 일자별 매장별 집계. Both dates inclusive; {@code storeId} null means every store. */
    List<SalesSummary> summarize(LocalDate from, LocalDate to, String storeId);

    /** The orders behind one cell of that summary — the drill-down. */
    List<Order> findByBusinessDate(LocalDate businessDate, String storeId);
}
