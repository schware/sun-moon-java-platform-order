package com.sunmoon.platform.domain.businessday;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BusinessDayRepository {

    Optional<BusinessDay> findOpen(String storeId);

    List<BusinessDay> findAllOpen();

    Optional<BusinessDay> find(String storeId, LocalDate businessDate);

    void insert(BusinessDay day);

    /** Clears the 마감 on a day that was closed, leaving 개점 as it was. */
    boolean reopen(String storeId, LocalDate businessDate);

    boolean close(String storeId, LocalDate businessDate, Instant closedAt, String closedBy);
}
