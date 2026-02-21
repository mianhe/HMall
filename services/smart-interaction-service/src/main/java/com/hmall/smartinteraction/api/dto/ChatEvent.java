package com.hmall.smartinteraction.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatEvent(
    String type,
    String content,
    String id,
    String name,
    Object arguments,
    String result,
    String message
) {
    public static ChatEvent delta(String content) {
        return new ChatEvent("delta", content, null, null, null, null, null);
    }

    public static ChatEvent toolCall(String id, String name, Object arguments) {
        return new ChatEvent("tool_call", null, id, name, arguments, null, null);
    }

    public static ChatEvent toolResult(String id, String name, String result) {
        return new ChatEvent("tool_result", null, id, name, null, result, null);
    }

    public static ChatEvent done() {
        return new ChatEvent("done", null, null, null, null, null, null);
    }

    public static ChatEvent error(String message) {
        return new ChatEvent("error", null, null, null, null, null, message);
    }
}
