package com.hmall.smartinteraction.api.dto;

import java.util.List;

public record ChatRequest(
    List<Message> messages,
    Context context,
    String provider,
    Long skillId,
    String skillMode,
    String clientType,
    Integer maxToolCallRounds
) {
    public record Message(String role, String content) {}

    public record Context(String page, List<CanvasPanel> canvasPanels) {}

    public record CanvasPanel(String type, String title) {}

    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
    }
}
