package com.hmall.smartinteraction.api.dto;

public record UpdateSettingsRequest(
    String adminBasePrompt,
    String consumerBasePrompt
) {}
