package com.hmall.user.application;

import java.util.List;

public record SegmentRulePreviewResult(
    Long ruleId,
    long hitCount,
    List<Long> sampleUserIds,
    List<ReasonStat> reasonStats
) {
}
