package com.hmall.smartinteraction.api.dto;

import com.hmall.smartinteraction.domain.Settings;

import java.time.Instant;

public record SettingsDto(
    String adminBasePrompt,
    String consumerBasePrompt,
    Instant updatedAt
) {
    public static SettingsDto from(Settings settings) {
        return new SettingsDto(
            settings.getAdminBasePrompt(),
            settings.getConsumerBasePrompt(),
            settings.getUpdatedAt()
        );
    }
}
