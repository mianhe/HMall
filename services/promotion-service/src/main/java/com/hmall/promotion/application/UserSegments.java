package com.hmall.promotion.application;

import java.util.Set;

public record UserSegments(String level, Set<String> tags) {
    public UserSegments {
        level = level == null || level.isBlank() ? "L1" : level;
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
