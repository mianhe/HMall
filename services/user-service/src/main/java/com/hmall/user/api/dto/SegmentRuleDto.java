package com.hmall.user.api.dto;

import com.hmall.user.domain.SegmentRuleStatus;

import java.time.Instant;

public record SegmentRuleDto(
    Long ruleId,
    String name,
    SegmentRuleStatus status,
    SegmentConditionDto conditions,
    Long lastPreviewCount,
    Instant updatedAt
) {
}
