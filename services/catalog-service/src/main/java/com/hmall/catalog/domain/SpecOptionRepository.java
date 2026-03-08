package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * 规格选项仓储。
 */
public interface SpecOptionRepository {

    SpecOption save(SpecOption option);

    Optional<SpecOption> findById(Long id);

    List<SpecOption> findByIdIn(List<Long> ids);

    List<SpecOption> findBySpecDimensionId(Long specDimensionId);

    boolean existsBySpecDimensionIdAndOptionValue(Long specDimensionId, String optionValue);

    void deleteById(Long id);
}
