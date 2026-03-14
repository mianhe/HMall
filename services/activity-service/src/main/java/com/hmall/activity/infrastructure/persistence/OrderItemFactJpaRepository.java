package com.hmall.activity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemFactJpaRepository extends JpaRepository<OrderItemFactEntity, Long> {

    List<OrderItemFactEntity> findByOrderId(Long orderId);

    @Modifying
    @Query("DELETE FROM OrderItemFactEntity e WHERE e.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT e FROM OrderItemFactEntity e WHERE e.createdDate >= :from AND e.createdDate <= :to")
    List<OrderItemFactEntity> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
