package com.sunmoon.platform.domain.order;

import com.sunmoon.platform.domain.businessday.BusinessDay;
import com.sunmoon.platform.domain.businessday.BusinessDayService;
import com.sunmoon.platform.domain.businessday.StoreClosedException;
import com.sunmoon.platform.infrastructure.messaging.EventPublisher;
import com.sunmoon.platform.infrastructure.messaging.OrderEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final BusinessDayService businessDays;
    private final EventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository, BusinessDayService businessDays, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.businessDays = businessDays;
        this.eventPublisher = eventPublisher;
    }

    /**
     * An order can only be taken by a shop that has 개점'd, and it is
     * stamped with that shop's 영업일자 — the 매출일자 every sale is
     * counted under.
     *
     * @throws StoreClosedException if the store is 마감 or never opened
     */
    public Order place(String storeId, String customerId, String menuName, BigDecimal amount) {
        BusinessDay day = businessDays.current(storeId)
                .orElseThrow(() -> new StoreClosedException(storeId));
        Order saved = orderRepository.save(
                Order.placed(storeId, customerId, menuName, amount, day.businessDate()));
        publish(OrderEvent.of(saved, null));
        return saved;
    }

    public List<Order> list(OrderStatus status) {
        return status == null ? orderRepository.findAll() : orderRepository.findByStatus(status);
    }

    public Optional<Order> read(long id) {
        return orderRepository.find(id);
    }

    /**
     * The one way an order changes state. Everything downstream — the
     * terminals, KDS, delivery — comes back through here, so the state
     * machine cannot be gone around.
     *
     * @throws IllegalOrderTransitionException if the move is not allowed
     */
    public Order moveTo(long id, OrderStatus next, String deviceId) {
        Order current = orderRepository.find(id)
                .orElseThrow(() -> new NoSuchElementException("No such order: " + id));
        Order moved = current.movedTo(next, deviceId);
        orderRepository.update(moved);
        publish(OrderEvent.of(moved, current.status()));
        return moved;
    }

    /**
     * Publishing is best-effort: the order is already persisted, and a
     * lost notification must not fail the call that caused it. A
     * production system would write to an outbox table here instead.
     */
    @CircuitBreaker(name = "orderEvents", fallbackMethod = "publishFallback")
    @Retry(name = "orderEvents")
    void publish(OrderEvent event) {
        eventPublisher.publish(event);
    }

    @SuppressWarnings("unused")
    private void publishFallback(OrderEvent event, Throwable ex) {
        // Deliberately silent — see publish().
    }
}
