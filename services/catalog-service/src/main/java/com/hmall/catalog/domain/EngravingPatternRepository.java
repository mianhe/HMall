package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

public interface EngravingPatternRepository {
    EngravingPattern save(EngravingPattern pattern);
    Optional<EngravingPattern> findById(Long id);
    List<EngravingPattern> findAllOrderBySortOrder(Boolean enabledFilter);
    void deleteById(Long id);
}
