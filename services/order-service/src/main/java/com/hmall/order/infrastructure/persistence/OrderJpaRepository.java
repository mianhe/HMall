package com.hmall.order.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    List<OrderEntity> findByUserIdAndStatus(Long userId, String status);
}
