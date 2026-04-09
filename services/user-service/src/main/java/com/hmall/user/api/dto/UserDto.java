package com.hmall.user.api.dto;

import java.util.Set;

public record UserDto(Long id, String username, String level, Set<String> tags) {}
