package com.hmall.smartinteraction.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Flux<JsonNode> streamChat(LlmProviderConfig.Provider provider,
                                     List<Map<String, Object>> messages,
                                     List<Map<String, Object>> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", provider.model());
        body.put("stream", true);

        ArrayNode messagesNode = body.putArray("messages");
        for (Map<String, Object> msg : messages) {
            messagesNode.add(objectMapper.valueToTree(msg));
        }

        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsNode = body.putArray("tools");
            for (Map<String, Object> tool : tools) {
                toolsNode.add(objectMapper.valueToTree(tool));
            }
        }

        String url = provider.baseUrl().replaceAll("/+$", "") + "/chat/completions";

        return webClient.post()
            .uri(url)
            .header("Authorization", "Bearer " + provider.apiKey())
            .header("Content-Type", "application/json")
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .mapNotNull(line -> {
                try {
                    String data = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) return null;
                    return objectMapper.readTree(data);
                } catch (Exception e) {
                    log.debug("Skipping unparseable SSE line: {}", line);
                    return null;
                }
            });
    }
}
