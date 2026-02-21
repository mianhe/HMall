package com.hmall.smartinteraction.api.dto;

import java.util.List;

public record ChatRequest(
    List<Message> messages,
    Context context,
    String provider
) {
    public record Message(String role, String content) {}

    public record Context(String page) {}

    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
    }
}
