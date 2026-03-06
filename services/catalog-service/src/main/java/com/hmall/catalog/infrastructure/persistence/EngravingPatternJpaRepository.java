package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EngravingPatternJpaRepository extends JpaRepository<EngravingPatternEntity, Long> {
    List<EngravingPatternEntity> findAllByOrderBySortOrderAsc();
    List<EngravingPatternEntity> findByEnabledTrueOrderBySortOrderAsc();
}
