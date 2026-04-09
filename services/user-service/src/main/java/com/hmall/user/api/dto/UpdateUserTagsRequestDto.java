package com.hmall.user.api.dto;

import java.util.Set;

public record UpdateUserTagsRequestDto(Set<String> tags) {
}
