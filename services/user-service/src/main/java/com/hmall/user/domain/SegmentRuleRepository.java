package com.hmall.user.domain;

import java.util.List;
import java.util.Optional;

public interface SegmentRuleRepository {
    SegmentRule save(SegmentRule rule);

    Optional<SegmentRule> findById(Long id);

    List<SegmentRule> findAll();
}
