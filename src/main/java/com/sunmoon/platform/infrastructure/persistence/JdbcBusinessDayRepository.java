package com.sunmoon.platform.infrastructure.persistence;

import com.sunmoon.platform.domain.businessday.BusinessDay;
import com.sunmoon.platform.domain.businessday.BusinessDayRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBusinessDayRepository implements BusinessDayRepository {

    private static final String COLUMNS =
            "store_id, business_date, opened_at, opened_by, closed_at, closed_by";

    private static final RowMapper<BusinessDay> MAPPER = (rs, rowNum) -> new BusinessDay(
            rs.getString("store_id"),
            rs.getDate("business_date").toLocalDate(),
            rs.getTimestamp("opened_at").toInstant(),
            rs.getString("opened_by"),
            rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toInstant(),
            rs.getString("closed_by"));

    private final JdbcTemplate jdbcTemplate;

    public JdbcBusinessDayRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<BusinessDay> findOpen(String storeId) {
        return jdbcTemplate.query(
                        "SELECT " + COLUMNS + " FROM store_business_days "
                                + "WHERE store_id = ? AND closed_at IS NULL", MAPPER, storeId)
                .stream().findFirst();
    }

    @Override
    public List<BusinessDay> findAllOpen() {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM store_business_days "
                        + "WHERE closed_at IS NULL ORDER BY store_id", MAPPER);
    }

    @Override
    public void insert(BusinessDay day) {
        jdbcTemplate.update(
                "INSERT INTO store_business_days (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)",
                day.storeId(),
                Date.valueOf(day.businessDate()),
                Timestamp.from(day.openedAt()),
                day.openedBy(),
                day.closedAt() == null ? null : Timestamp.from(day.closedAt()),
                day.closedBy());
    }

    @Override
    public boolean close(String storeId, LocalDate businessDate, Instant closedAt, String closedBy) {
        return jdbcTemplate.update(
                "UPDATE store_business_days SET closed_at = ?, closed_by = ? "
                        + "WHERE store_id = ? AND business_date = ? AND closed_at IS NULL",
                Timestamp.from(closedAt), closedBy, storeId, Date.valueOf(businessDate)) > 0;
    }
}
