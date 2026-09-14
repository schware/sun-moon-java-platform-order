package com.sunmoon.platform.domain.sales;

import com.sunmoon.platform.domain.businessday.BusinessDayService;
import com.sunmoon.platform.domain.order.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 매출 조회, by 영업일자.
 *
 * <p>"Today" comes from {@link BusinessDayService} rather than from
 * {@code LocalDate.now()}: the same Asia/Seoul calendar that stamps an
 * order has to be the one that defaults this screen's date range, or a
 * query run at 08:00 KST on a UTC host would open on yesterday.
 */
@Service
public class SalesService {

    /** A fortnight back is enough to find last week's day without paging. */
    private static final int DEFAULT_DAYS_BACK = 13;

    private final SalesRepository repository;
    private final BusinessDayService businessDays;

    public SalesService(SalesRepository repository, BusinessDayService businessDays) {
        this.repository = repository;
        this.businessDays = businessDays;
    }

    public List<SalesSummary> summarize(LocalDate from, LocalDate to, String storeId) {
        LocalDate end = to != null ? to : businessDays.today();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS_BACK);
        // Swapped dates would silently return nothing, which reads as "no
        // sales that week" rather than as the mistake it is.
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from must not be after to: " + start + " > " + end);
        }
        return repository.summarize(start, end, storeId);
    }

    public List<Order> detail(LocalDate businessDate, String storeId) {
        return repository.findByBusinessDate(businessDate, storeId);
    }
}
