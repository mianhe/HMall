package com.hmall.smartinteraction.api.dto;

import com.hmall.smartinteraction.domain.Skill;

import java.time.Instant;
import java.util.List;

public record SkillDto(
    Long id,
    String name,
    String description,
    String systemPrompt,
    List<String> allowedTools,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt
) {
    public static SkillDto from(Skill skill) {
        return new SkillDto(
            skill.getId(),
            skill.getName(),
            skill.getDescription(),
            skill.getSystemPrompt(),
            skill.getAllowedTools(),
            skill.isDefault(),
            skill.getCreatedAt(),
            skill.getUpdatedAt()
        );
    }
}
