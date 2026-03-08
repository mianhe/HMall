package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecDimensionJpaRepository extends JpaRepository<SpecDimensionEntity, Long> {

    List<SpecDimensionEntity> findBySpuIdOrderBySortOrderAscIdAsc(Long spuId);

    List<SpecDimensionEntity> findByIdIn(List<Long> ids);

    boolean existsBySpuIdAndName(Long spuId, String name);
}
