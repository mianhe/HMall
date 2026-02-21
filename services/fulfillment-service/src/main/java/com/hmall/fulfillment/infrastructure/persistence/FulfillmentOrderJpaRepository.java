package com.hmall.fulfillment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FulfillmentOrderJpaRepository extends JpaRepository<FulfillmentOrderEntity, Long> {

    List<FulfillmentOrderEntity> findByOrderId(Long orderId);
}
