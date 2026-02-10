package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkuSpecValueJpaRepository extends JpaRepository<SkuSpecValueEntity, Long> {

    List<SkuSpecValueEntity> findBySkuIdOrderByIdAsc(Long skuId);

    void deleteBySkuId(Long skuId);
}
