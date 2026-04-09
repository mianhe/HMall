package com.hmall.user.api.dto;

import java.util.Set;

public record SegmentConditionDto(
    Set<String> levelsIn,
    Set<String> tagsAny,
    Set<String> tagsAll,
    Set<String> excludeTags
) {
}
