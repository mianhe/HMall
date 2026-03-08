package com.hmall.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkuStockJpaRepository extends JpaRepository<SkuStockEntity, Long> {

    List<SkuStockEntity> findBySkuIdIn(List<Long> skuIds);
}
