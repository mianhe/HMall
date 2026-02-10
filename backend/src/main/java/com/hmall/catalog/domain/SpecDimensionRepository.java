package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * 规格维度仓储。
 */
public interface SpecDimensionRepository {

    SpecDimension save(SpecDimension dimension);

    Optional<SpecDimension> findById(Long id);

    List<SpecDimension> findBySpuId(Long spuId);

    boolean existsBySpuIdAndName(Long spuId, String name);
}
