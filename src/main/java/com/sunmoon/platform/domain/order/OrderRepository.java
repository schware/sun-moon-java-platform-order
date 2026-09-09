package com.sunmoon.platform.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    List<Order> findAll();

    /** Terminals poll for what they can act on, so filtering by status is a first-class query. */
    List<Order> findByStatus(OrderStatus status);

    Optional<Order> find(long id);

    boolean update(Order order);
}
