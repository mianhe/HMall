package com.hmall.smartinteraction.api;

import com.hmall.smartinteraction.api.dto.ChatRequest;
import com.hmall.smartinteraction.domain.AiChatService;
import com.hmall.smartinteraction.infrastructure.LlmProviderConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService chatService;
    private final LlmProviderConfig config;

    public AiChatController(AiChatService chatService, LlmProviderConfig config) {
        this.chatService = chatService;
        this.config = config;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = new SseEmitter(600_000L);
        emitter.onTimeout(emitter::complete);

        Thread.startVirtualThread(() -> chatService.chat(request, emitter));

        return emitter;
    }

    @GetMapping("/models")
    public Map<String, Object> models() {
        List<Map<String, Object>> models = config.providers().entrySet().stream()
            .map(entry -> {
                String id = entry.getKey();
                String displayName = switch (id) {
                    case "qwen" -> "通义千问";
                    case "deepseek" -> "DeepSeek";
                    default -> id;
                };
                boolean isDefault = id.equals(config.defaultProvider());
                return Map.<String, Object>of(
                    "id", id,
                    "name", displayName,
                    "default", isDefault
                );
            })
            .toList();
        return Map.of("models", models);
    }
}
