package com.hmall.smartinteraction.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class McpToolBridge {

    private static final Logger log = LoggerFactory.getLogger(McpToolBridge.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private volatile List<Map<String, Object>> cachedTools;

    public McpToolBridge(LlmProviderConfig config) {
        this.webClient = WebClient.builder()
            .baseUrl(config.mcp().url())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/event-stream")
            .build();
    }

    public List<Map<String, Object>> getTools() {
        if (cachedTools != null) return cachedTools;
        synchronized (this) {
            if (cachedTools != null) return cachedTools;
            initializeAndDiscoverTools();
            return cachedTools != null ? cachedTools : List.of();
        }
    }

    public String executeTool(String toolName, Object arguments) {
        try {
            ObjectNode params = objectMapper.createObjectNode();
            params.put("name", toolName);
            params.set("arguments", objectMapper.valueToTree(arguments));

            JsonNode response = jsonRpcCall("tools/call", params);
            JsonNode content = response.path("result").path("content");
            if (content.isArray() && !content.isEmpty()) {
                return content.get(0).path("text").asText("");
            }
            return response.path("result").toString();
        } catch (Exception e) {
            log.error("MCP tool execution failed: {}", toolName, e);
            return "Tool execution error: " + e.getMessage();
        }
    }

    public void invalidateCache() {
        synchronized (this) {
            cachedTools = null;
            sessionId.set(null);
        }
    }

    public void refreshTools() {
        invalidateCache();
        getTools();
    }

    private void initializeAndDiscoverTools() {
        try {
            ObjectNode initParams = objectMapper.createObjectNode();
            ObjectNode clientInfo = initParams.putObject("clientInfo");
            clientInfo.put("name", "hmall-smart-interaction");
            clientInfo.put("version", "1.0.0");
            initParams.put("protocolVersion", "2025-03-26");
            initParams.putObject("capabilities").putObject("tools");

            jsonRpcCall("initialize", initParams);
            jsonRpcCall("notifications/initialized", null);

            JsonNode toolsResponse = jsonRpcCall("tools/list", objectMapper.createObjectNode());
            JsonNode toolsArray = toolsResponse.path("result").path("tools");

            List<Map<String, Object>> tools = new ArrayList<>();
            if (toolsArray.isArray()) {
                for (JsonNode tool : toolsArray) {
                    tools.add(toOpenAiFunctionTool(tool));
                }
            }
            cachedTools = tools;
            log.info("Discovered {} MCP tools", tools.size());
        } catch (Exception e) {
            log.warn("Failed to initialize MCP connection: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toOpenAiFunctionTool(JsonNode mcpTool) throws Exception {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", mcpTool.path("name").asText(),
                "description", mcpTool.path("description").asText(""),
                "parameters", objectMapper.treeToValue(mcpTool.path("inputSchema"), Map.class)
            )
        );
    }

    private JsonNode jsonRpcCall(String method, ObjectNode params) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        if (params != null) request.set("params", params);
        if (!method.startsWith("notifications/")) request.put("id", System.nanoTime());

        WebClient.RequestBodySpec spec = webClient.post().uri("");
        String sid = sessionId.get();
        if (sid != null) spec = spec.header("mcp-session-id", sid);

        String responseBody = spec
            .bodyValue(request.toString())
            .retrieve()
            .toEntity(String.class)
            .doOnNext(entity -> {
                String newSid = entity.getHeaders().getFirst("mcp-session-id");
                if (newSid != null) sessionId.set(newSid);
            })
            .map(entity -> entity.getBody() != null ? entity.getBody() : "{}")
            .block();

        try {
            return objectMapper.readTree(extractJson(responseBody));
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private String extractJson(String body) {
        if (body == null || body.isBlank()) return "{}";
        if (body.trim().startsWith("{")) return body;
        for (String line : body.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                String data = trimmed.substring(5).trim();
                if (data.startsWith("{")) return data;
            }
        }
        return "{}";
    }
}
