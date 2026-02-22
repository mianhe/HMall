package com.hmall.smartinteraction.api.dto;

import java.util.List;

public record UpdateSkillRequest(
    String name,
    String description,
    String systemPrompt,
    List<String> allowedTools
) {}
