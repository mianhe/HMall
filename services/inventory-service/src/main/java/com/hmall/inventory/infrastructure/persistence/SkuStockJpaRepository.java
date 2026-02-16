package com.hmall.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuStockJpaRepository extends JpaRepository<SkuStockEntity, Long> {
}
