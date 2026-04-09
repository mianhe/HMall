package com.hmall.user.api.dto;

import java.util.List;

public record SegmentRulePreviewResponseDto(
    Long ruleId,
    long hitCount,
    List<Long> sampleUserIds,
    List<ReasonStatDto> reasonStats
) {
}
