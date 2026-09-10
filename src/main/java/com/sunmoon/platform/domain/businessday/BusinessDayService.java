package com.sunmoon.platform.domain.businessday;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * 개점 and 마감, and the 영업일자 every order is stamped with.
 *
 * <p>There is deliberately no scheduled job that closes a store at
 * midnight. A shop still serving at 00:30 has not finished its day, and a
 * timer that decided otherwise would split one evening's takings across
 * two 매출일자. The day rolls when a person presses a button: 마감, or
 * 개점 on a day that has already gone stale.
 */
@Service
public class BusinessDayService {

    /** The shop's calendar, not the server's — this host runs on UTC. */
    static final ZoneId STORE_ZONE = ZoneId.of("Asia/Seoul");

    private final BusinessDayRepository repository;
    private final Clock clock;

    // Two constructors means Spring cannot pick one on its own, and the
    // failure is at startup rather than at compile time — this annotation
    // is load-bearing.
    @Autowired
    public BusinessDayService(BusinessDayRepository repository) {
        this(repository, Clock.system(STORE_ZONE));
    }

    /** Visible for tests, which need to control what "today" is. */
    BusinessDayService(BusinessDayRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(STORE_ZONE));
    }

    public Optional<BusinessDay> current(String storeId) {
        return repository.findOpen(storeId);
    }

    public List<BusinessDay> allOpen() {
        return repository.findAllOpen();
    }

    /**
     * Opens today. If the store was left open on an earlier 영업일자, that
     * day is closed first and the answer says so — the spec's "개점이 되어
     * 있는 상태에서 날짜가 지나면 마감", carried out by the person opening
     * up rather than by a timer nobody watched.
     */
    public OpenResult open(String storeId, String deviceId) {
        LocalDate today = today();
        Optional<BusinessDay> openDay = repository.findOpen(storeId);

        if (openDay.isPresent()) {
            BusinessDay day = openDay.get();
            if (!day.isStale(today)) {
                return new OpenResult(Outcome.ALREADY_OPEN, day, null);
            }
            repository.close(storeId, day.businessDate(), Instant.now(clock), deviceId);
            BusinessDay fresh = BusinessDay.opened(storeId, today, deviceId);
            repository.insert(fresh);
            return new OpenResult(Outcome.ROLLED, fresh, day.businessDate());
        }

        // 마감 pressed by mistake, or a break in the middle of the day:
        // opening again on the same 영업일자 resumes it rather than
        // starting a second one. There is only ever one row per
        // (매장, 영업일자), and the takings stay under that one date.
        Optional<BusinessDay> closedToday = repository.find(storeId, today);
        if (closedToday.isPresent()) {
            repository.reopen(storeId, today);
            BusinessDay resumed = closedToday.get();
            return new OpenResult(Outcome.REOPENED, new BusinessDay(
                    resumed.storeId(), resumed.businessDate(),
                    resumed.openedAt(), resumed.openedBy(), null, null), null);
        }

        BusinessDay fresh = BusinessDay.opened(storeId, today, deviceId);
        repository.insert(fresh);
        return new OpenResult(Outcome.OPENED, fresh, null);
    }

    public Optional<BusinessDay> close(String storeId, String deviceId) {
        Optional<BusinessDay> openDay = repository.findOpen(storeId);
        if (openDay.isEmpty()) {
            return Optional.empty();
        }
        BusinessDay day = openDay.get();
        Instant now = Instant.now(clock);
        repository.close(storeId, day.businessDate(), now, deviceId);
        return Optional.of(new BusinessDay(
                day.storeId(), day.businessDate(), day.openedAt(), day.openedBy(), now, deviceId));
    }

    public enum Outcome { OPENED, REOPENED, ROLLED, ALREADY_OPEN }

    /** {@code closedDate} is the stale day that was closed to make room, or null. */
    public record OpenResult(Outcome outcome, BusinessDay day, LocalDate closedDate) {
    }
}
