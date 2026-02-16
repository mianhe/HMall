package com.hmall.order.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long orderId);

    List<Order> findByUserId(Long userId, int page, int size);

    long countByUserId(Long userId);
}
