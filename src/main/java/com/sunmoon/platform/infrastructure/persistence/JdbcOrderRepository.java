package com.sunmoon.platform.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.platform.domain.order.Order;
import com.sunmoon.platform.domain.order.OrderRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

// Postgres + JSONB — see docs/adr in the parent sun-moon-java-platform
// repo. The whole Order is stored as one JSONB blob keyed by a
// Postgres-assigned bigserial id, with customer_id pulled out as a plain
// indexed column for lookups — same access pattern a document store
// would give.
@Repository
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Order save(Order order) {
        Long id = jdbcTemplate.queryForObject("SELECT nextval('orders_id_seq')", Long.class);
        Order withId = order.withId(id);
        jdbcTemplate.update(
                "INSERT INTO orders (id, customer_id, data) VALUES (?, ?, ?::jsonb)",
                withId.id(), withId.customerId(), writeJson(withId));
        return withId;
    }

    @Override
    public List<Order> findAll() {
        return jdbcTemplate.query("SELECT data FROM orders ORDER BY id", (rs, rowNum) -> readJson(rs.getString("data")));
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
