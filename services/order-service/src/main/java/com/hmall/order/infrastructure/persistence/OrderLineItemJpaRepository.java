package com.hmall.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderLineItemJpaRepository extends JpaRepository<OrderLineItemEntity, Long> {

    List<OrderLineItemEntity> findByOrderIdOrderByIdAsc(Long orderId);
}
