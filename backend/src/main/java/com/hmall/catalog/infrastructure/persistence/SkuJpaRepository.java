package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkuJpaRepository extends JpaRepository<SkuEntity, Long> {

    List<SkuEntity> findBySpuIdOrderByIdAsc(Long spuId);
}
