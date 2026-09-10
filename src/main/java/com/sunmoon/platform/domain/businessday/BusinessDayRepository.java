package com.sunmoon.platform.domain.businessday;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BusinessDayRepository {

    Optional<BusinessDay> findOpen(String storeId);

    List<BusinessDay> findAllOpen();

    void insert(BusinessDay day);

    boolean close(String storeId, LocalDate businessDate, Instant closedAt, String closedBy);
}
