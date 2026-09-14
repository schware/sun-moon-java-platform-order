package com.sunmoon.platform.domain.sales;

import com.sunmoon.platform.domain.businessday.BusinessDay;
import com.sunmoon.platform.domain.businessday.BusinessDayRepository;
import com.sunmoon.platform.domain.businessday.BusinessDayService;
import com.sunmoon.platform.domain.order.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesServiceTest {

    private static final LocalDate TODAY_IN_SEOUL = LocalDate.of(2026, 9, 14);

    private final RecordingRepository repository = new RecordingRepository();

    /**
     * The point of the stub: the default range must come from the shop's
     * own calendar, not from {@code LocalDate.now()} on a UTC host — which
     * at 08:00 KST would open this screen on yesterday.
     */
    private final SalesService service = new SalesService(
            repository,
            new BusinessDayService(new NoBusinessDays()) {
                @Override
                public LocalDate today() {
                    return TODAY_IN_SEOUL;
                }
            });

    @Test
    void defaultsToTheLastFortnightOfTheStoresOwnCalendar() {
        service.summarize(null, null, null);

        assertThat(repository.to).isEqualTo(TODAY_IN_SEOUL);
        assertThat(repository.from).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void keepsTheRangeItWasGiven() {
        service.summarize(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), "store-01");

        assertThat(repository.from).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(repository.to).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(repository.storeId).isEqualTo("store-01");
    }

    @Test
    void refusesABackwardsRangeRatherThanReturningNothing() {
        assertThatThrownBy(() ->
                service.summarize(LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class RecordingRepository implements SalesRepository {
        private LocalDate from;
        private LocalDate to;
        private String storeId;

        @Override
        public List<SalesSummary> summarize(LocalDate from, LocalDate to, String storeId) {
            this.from = from;
            this.to = to;
            this.storeId = storeId;
            return List.of(new SalesSummary(to, "store-01", 3, 2, 1, 0, new BigDecimal("20000")));
        }

        @Override
        public List<Order> findByBusinessDate(LocalDate businessDate, String storeId) {
            return List.of();
        }
    }

    /** The sales screen never opens or closes a day; it only borrows the calendar. */
    private static final class NoBusinessDays implements BusinessDayRepository {
        @Override
        public Optional<BusinessDay> findOpen(String storeId) {
            return Optional.empty();
        }

        @Override
        public List<BusinessDay> findAllOpen() {
            return List.of();
        }

        @Override
        public Optional<BusinessDay> find(String storeId, LocalDate businessDate) {
            return Optional.empty();
        }

        @Override
        public void insert(BusinessDay day) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean reopen(String storeId, LocalDate businessDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean close(String storeId, LocalDate businessDate, Instant closedAt, String closedBy) {
            throw new UnsupportedOperationException();
        }
    }
}
