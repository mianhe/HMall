package com.hmall.user.api.dto;

import java.util.Set;

public record UserSegmentsDto(Long userId, String level, Set<String> tags) {}
