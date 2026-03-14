package com.hmall.activity.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderFactRepository {

    void save(OrderFact fact);

    Optional<OrderFact> findByOrderId(Long orderId);

    void saveItems(List<OrderItemFact> items);

    List<OrderItemFact> findItemsByOrderId(Long orderId);

    List<OrderFact> findByDateRange(LocalDate from, LocalDate to);

    List<OrderItemFact> findItemsByDateRange(LocalDate from, LocalDate to);

    List<OrderFact> findByFilters(LocalDate from, LocalDate to,
                                  Boolean hasEngraving, Boolean hasWarranty,
                                  String currentStage, Long userId, int limit);

    void deleteItemsByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    void deleteAll();
}
