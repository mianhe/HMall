package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecOptionJpaRepository extends JpaRepository<SpecOptionEntity, Long> {

    List<SpecOptionEntity> findBySpecDimensionIdOrderBySortOrderAscIdAsc(Long specDimensionId);

    boolean existsBySpecDimensionIdAndOptionValue(Long specDimensionId, String optionValue);
}
