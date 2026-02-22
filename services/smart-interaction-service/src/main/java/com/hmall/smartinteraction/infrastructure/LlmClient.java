package com.hmall.smartinteraction.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofMinutes(5))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.MINUTES)));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

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
