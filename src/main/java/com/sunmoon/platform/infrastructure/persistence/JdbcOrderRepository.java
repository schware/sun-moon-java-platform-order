package com.sunmoon.platform.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.platform.domain.order.Order;
import com.sunmoon.platform.domain.order.OrderRepository;
import com.sunmoon.platform.domain.order.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Postgres + JSONB — see the umbrella repo's ADR-0001. The whole Order is
 * one JSONB blob keyed by a Postgres bigserial id.
 *
 * <p>{@code customer_id} and {@code status} are pulled out as plain
 * indexed columns because both are queried: terminals ask "what is
 * PLACED", and that must not become a scan over parsed JSON.
 */
@Repository
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<Order> mapper;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.mapper = (rs, rowNum) -> readJson(rs.getString("data"));
    }

    @Override
    public Order save(Order order) {
        Long id = jdbcTemplate.queryForObject("SELECT nextval('orders_id_seq')", Long.class);
        Order withId = order.withId(id);
        jdbcTemplate.update(
                "INSERT INTO orders (id, store_id, customer_id, status, business_date, data) "
                        + "VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                withId.id(), withId.storeId(), withId.customerId(), withId.status().name(),
                withId.businessDate() == null ? null : java.sql.Date.valueOf(withId.businessDate()),
                writeJson(withId));
        return withId;
    }

    @Override
    public List<Order> findAll() {
        return jdbcTemplate.query("SELECT data FROM orders ORDER BY id DESC", mapper);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jdbcTemplate.query(
                "SELECT data FROM orders WHERE status = ? ORDER BY id", mapper, status.name());
    }

    @Override
    public Optional<Order> find(long id) {
        return jdbcTemplate.query("SELECT data FROM orders WHERE id = ?", mapper, id)
                .stream().findFirst();
    }

    @Override
    public boolean update(Order order) {
        return jdbcTemplate.update(
                "UPDATE orders SET status = ?, data = ?::jsonb WHERE id = ?",
                order.status().name(), writeJson(order), order.id()) > 0;
    }

    private String writeJson(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Order " + order.id(), e);
        }
    }

    private Order readJson(String json) {
        try {
            return objectMapper.readValue(json, Order.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize Order row", e);
        }
    }
}
