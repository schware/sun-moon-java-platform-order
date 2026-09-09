package com.sunmoon.platform.domain.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Expires orders nobody accepted in time.
 *
 * <p>This lives in Order rather than in the Device Server because the
 * timeout has to hold whether or not any terminal — or the Device Server
 * itself — is running. An order left PLACED forever because the one
 * service watching the clock was down is exactly the failure this is
 * supposed to prevent.
 *
 * <p>It sweeps rather than schedules a timer per order: the state and the
 * timestamp are already in the database, so a sweep survives a restart
 * with no extra machinery, and a few seconds of imprecision costs nothing
 * here.
 */
@Component
public class AcceptanceTimeout {

    private static final Logger log = LoggerFactory.getLogger(AcceptanceTimeout.class);

    private final OrderService orderService;
    private final Duration limit;

    public AcceptanceTimeout(OrderService orderService,
                             @Value("${sun-moon.order.accept-timeout:PT3M}") Duration limit) {
        this.orderService = orderService;
        this.limit = limit;
    }

    @Scheduled(fixedDelayString = "${sun-moon.order.accept-timeout-sweep:PT10S}")
    public void expireUnaccepted() {
        Instant cutoff = Instant.now().minus(limit);
        List<Order> stale = orderService.list(OrderStatus.PLACED).stream()
                .filter(order -> order.placedAt().isBefore(cutoff))
                .toList();

        for (Order order : stale) {
            try {
                orderService.moveTo(order.id(), OrderStatus.EXPIRED, null);
                log.info("order {} expired: not accepted within {}", order.id(), limit);
            } catch (RuntimeException e) {
                // Someone accepted it between the read and the write. That
                // is the right outcome, not an error — the sweep just lost.
                log.debug("order {} was no longer PLACED when the sweep reached it", order.id());
            }
        }
    }
}
