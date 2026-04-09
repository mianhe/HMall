package com.hmall.user.domain;

import com.hmall.user.application.UserBadRequestException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record SegmentCondition(
    Set<String> levelsIn,
    Set<String> tagsAny,
    Set<String> tagsAll,
    Set<String> excludeTags
) {
    public SegmentCondition {
        levelsIn = normalize(levelsIn);
        tagsAny = normalize(tagsAny);
        tagsAll = normalize(tagsAll);
        excludeTags = normalize(excludeTags);
        validate(levelsIn, tagsAny, tagsAll, excludeTags);
    }

    public boolean matches(String userLevel, Set<String> userTags) {
        return mismatchReasons(userLevel, userTags).isEmpty();
    }

    public List<String> mismatchReasons(String userLevel, Set<String> userTags) {
        Set<String> normalizedTags = normalize(userTags);
        List<String> reasons = new ArrayList<>();
        if (!levelsIn.isEmpty() && (userLevel == null || !levelsIn.contains(userLevel.trim()))) {
            reasons.add("LEVEL_NOT_MATCH");
        }
        if (!tagsAny.isEmpty() && normalizedTags.stream().noneMatch(tagsAny::contains)) {
            reasons.add("MISSING_ANY_TAG");
        }
        if (!tagsAll.isEmpty() && !normalizedTags.containsAll(tagsAll)) {
            reasons.add("MISSING_ALL_TAGS");
        }
        if (!excludeTags.isEmpty() && normalizedTags.stream().anyMatch(excludeTags::contains)) {
            reasons.add("EXCLUDED_TAG_HIT");
        }
        return reasons;
    }

    private static Set<String> normalize(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : input) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return Set.copyOf(normalized);
    }

    private static void validate(Set<String> levelsIn, Set<String> tagsAny, Set<String> tagsAll, Set<String> excludeTags) {
        if (levelsIn.isEmpty() && tagsAny.isEmpty() && tagsAll.isEmpty() && excludeTags.isEmpty()) {
            throw new UserBadRequestException("圈选规则至少包含一种条件");
        }
        boolean conflict = tagsAny.stream().anyMatch(excludeTags::contains);
        if (conflict) {
            throw new UserBadRequestException("tagsAny 与 excludeTags 存在冲突");
        }
    }
}
