package com.sunmoon.platform.domain.businessday;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDayServiceTest {

    /** 2026-09-10 09:00 KST — a shop opening in the morning. */
    private static final Instant MORNING = Instant.parse("2026-09-10T00:00:00Z");

    /** 2026-09-11 01:00 KST — still the same shift, but the calendar has turned. */
    private static final Instant AFTER_MIDNIGHT = Instant.parse("2026-09-10T16:00:00Z");

    private InMemoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
    }

    private BusinessDayService serviceAt(Instant now) {
        return new BusinessDayService(repository, Clock.fixed(now, BusinessDayService.STORE_ZONE));
    }

    @Test
    void opensOnTheStoresOwnCalendarNotTheServersUtcDate() {
        // 2026-09-10T16:00Z is already 2026-09-11 in Seoul.
        BusinessDayService service = serviceAt(AFTER_MIDNIGHT);

        BusinessDayService.OpenResult result = service.open("store-01", "pos-01");

        assertThat(result.outcome()).isEqualTo(BusinessDayService.Outcome.OPENED);
        assertThat(result.day().businessDate()).isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    void refusesASecondOpenOnTheSameBusinessDate() {
        BusinessDayService service = serviceAt(MORNING);
        service.open("store-01", "pos-01");

        BusinessDayService.OpenResult again = service.open("store-01", "pos-02");

        assertThat(again.outcome()).isEqualTo(BusinessDayService.Outcome.ALREADY_OPEN);
        assertThat(repository.rows).hasSize(1);
    }

    @Test
    void openingAfterMidnightKeepsServingTheOldDayUntilSomeonePressesOpen() {
        // Opened in the morning, still open at 01:00 the next day.
        serviceAt(MORNING).open("store-01", "pos-01");

        BusinessDay stillOpen = repository.findOpen("store-01").orElseThrow();
        assertThat(stillOpen.businessDate()).isEqualTo(LocalDate.of(2026, 9, 10));

        // Nothing closed it on its own — no timer, by design.
        assertThat(stillOpen.isOpen()).isTrue();
        assertThat(stillOpen.isStale(LocalDate.of(2026, 9, 11))).isTrue();
    }

    @Test
    void openingOnANewDateClosesTheStaleOneAndSaysSo() {
        serviceAt(MORNING).open("store-01", "pos-01");

        BusinessDayService.OpenResult rolled = serviceAt(AFTER_MIDNIGHT).open("store-01", "pos-01");

        assertThat(rolled.outcome()).isEqualTo(BusinessDayService.Outcome.ROLLED);
        assertThat(rolled.closedDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(rolled.day().businessDate()).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(repository.findOpen("store-01").orElseThrow().businessDate())
                .isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    void closingLeavesNoOpenDay() {
        BusinessDayService service = serviceAt(MORNING);
        service.open("store-01", "pos-01");

        Optional<BusinessDay> closed = service.close("store-01", "pos-01");

        assertThat(closed).isPresent();
        assertThat(closed.orElseThrow().isOpen()).isFalse();
        assertThat(service.current("store-01")).isEmpty();
    }

    @Test
    void closingAStoreThatWasNotOpenIsRefused() {
        assertThat(serviceAt(MORNING).close("store-01", "pos-01")).isEmpty();
    }

    @Test
    void openingAgainOnTheSameDayResumesThatDayRatherThanStartingASecondOne() {
        BusinessDayService service = serviceAt(MORNING);
        service.open("store-01", "pos-01");
        service.close("store-01", "pos-01");

        BusinessDayService.OpenResult again = service.open("store-01", "pos-01");

        assertThat(again.outcome()).isEqualTo(BusinessDayService.Outcome.REOPENED);
        assertThat(again.day().businessDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(again.day().isOpen()).isTrue();
        // One row per (매장, 영업일자) — the takings stay under one 매출일자.
        assertThat(repository.rows).hasSize(1);
        assertThat(service.current("store-01")).isPresent();
    }

    private static final class InMemoryRepository implements BusinessDayRepository {
        private final List<BusinessDay> rows = new ArrayList<>();

        @Override
        public Optional<BusinessDay> findOpen(String storeId) {
            return rows.stream().filter(d -> d.storeId().equals(storeId) && d.isOpen()).findFirst();
        }

        @Override
        public List<BusinessDay> findAllOpen() {
            return rows.stream().filter(BusinessDay::isOpen).toList();
        }

        @Override
        public Optional<BusinessDay> find(String storeId, LocalDate businessDate) {
            return rows.stream()
                    .filter(d -> d.storeId().equals(storeId) && d.businessDate().equals(businessDate))
                    .findFirst();
        }

        @Override
        public void insert(BusinessDay day) {
            rows.add(day);
        }

        @Override
        public boolean reopen(String storeId, LocalDate businessDate) {
            for (int i = 0; i < rows.size(); i++) {
                BusinessDay d = rows.get(i);
                if (d.storeId().equals(storeId) && d.businessDate().equals(businessDate)) {
                    rows.set(i, new BusinessDay(
                            d.storeId(), d.businessDate(), d.openedAt(), d.openedBy(), null, null));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean close(String storeId, LocalDate businessDate, Instant closedAt, String closedBy) {
            for (int i = 0; i < rows.size(); i++) {
                BusinessDay d = rows.get(i);
                if (d.storeId().equals(storeId) && d.businessDate().equals(businessDate) && d.isOpen()) {
                    rows.set(i, new BusinessDay(
                            d.storeId(), d.businessDate(), d.openedAt(), d.openedBy(), closedAt, closedBy));
                    return true;
                }
            }
            return false;
        }
    }
}
