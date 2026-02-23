package com.hmall.smartinteraction.api.dto;

import java.util.List;

public record CreateSkillRequest(
    String name,
    String description,
    String systemPrompt,
    List<String> allowedTools,
    String audience
) {}
