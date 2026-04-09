package com.hmall.user.api.dto;

public record CreateSegmentRuleRequestDto(String name, SegmentConditionDto conditions) {
}
