package com.sunmoon.platform.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.platform.domain.order.Order;
import com.sunmoon.platform.domain.sales.SalesRepository;
import com.sunmoon.platform.domain.sales.SalesSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Counting is done by Postgres, not by loading every order and summing in
 * Java. {@code business_date} and {@code store_id} are real indexed columns
 * (V4, V5) precisely so this query can group on them; only {@code amount}
 * comes out of the JSONB, and only for the rows that survive the filter.
 */
@Repository
public class JdbcSalesRepository implements SalesRepository {

    /** The statuses that mean the store took the order. Anything else is not a sale. */
    private static final String ACCEPTED_STATUSES = "'ACCEPTED', 'PRODUCED', 'DELIVERING', 'COMPLETED'";

    private static final RowMapper<SalesSummary> SUMMARY_MAPPER = (rs, rowNum) -> new SalesSummary(
            rs.getDate("business_date").toLocalDate(),
            rs.getString("store_id"),
            rs.getLong("order_count"),
            rs.getLong("accepted_count"),
            rs.getLong("rejected_count"),
            rs.getLong("expired_count"),
            rs.getBigDecimal("sales_amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("sales_amount"));

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Order> orderMapper;

    public JdbcSalesRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderMapper = (rs, rowNum) -> {
            try {
                return objectMapper.readValue(rs.getString("data"), Order.class);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize Order row", e);
            }
        };
    }

    @Override
    public List<SalesSummary> summarize(LocalDate from, LocalDate to, String storeId) {
        // business_date IS NULL is not "an unknown day" to be shown as a
        // blank row — it is an order placed before 영업일 existed at all
        // (V5). Those predate the sales model and are left out of it.
        StringBuilder sql = new StringBuilder("""
                SELECT business_date,
                       store_id,
                       COUNT(*) AS order_count,
                       COUNT(*) FILTER (WHERE status IN (%s)) AS accepted_count,
                       COUNT(*) FILTER (WHERE status = 'REJECTED') AS rejected_count,
                       COUNT(*) FILTER (WHERE status = 'EXPIRED') AS expired_count,
                       COALESCE(SUM((data->>'amount')::numeric)
                                FILTER (WHERE status IN (%s)), 0) AS sales_amount
                  FROM orders
                 WHERE business_date BETWEEN ? AND ?
                """.formatted(ACCEPTED_STATUSES, ACCEPTED_STATUSES));

        List<Object> args = new ArrayList<>(List.of(Date.valueOf(from), Date.valueOf(to)));
        // Appended rather than passed as a nullable parameter: Postgres
        // cannot infer the type of a bare `? IS NULL` and refuses the
        // statement outright.
        if (storeId != null && !storeId.isBlank()) {
            sql.append(" AND store_id = ?");
            args.add(storeId);
        }
        sql.append(" GROUP BY business_date, store_id ORDER BY business_date DESC, store_id");

        return jdbcTemplate.query(sql.toString(), SUMMARY_MAPPER, args.toArray());
    }

    @Override
    public List<Order> findByBusinessDate(LocalDate businessDate, String storeId) {
        StringBuilder sql = new StringBuilder("SELECT data FROM orders WHERE business_date = ?");
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(businessDate)));
        if (storeId != null && !storeId.isBlank()) {
            sql.append(" AND store_id = ?");
            args.add(storeId);
        }
        sql.append(" ORDER BY id");

        return jdbcTemplate.query(sql.toString(), orderMapper, args.toArray());
    }
}
