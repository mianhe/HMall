package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceBindingJpaRepository extends JpaRepository<ServiceBindingEntity, Long> {
    List<ServiceBindingEntity> findByTargetSpuId(Long targetSpuId);
    List<ServiceBindingEntity> findByServiceSkuId(Long serviceSkuId);
}
