package com.hmall.smartinteraction.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class McpToolBridge {

    private static final Logger log = LoggerFactory.getLogger(McpToolBridge.class);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private volatile List<Map<String, Object>> cachedTools;
    private volatile List<McpResource> cachedResources;

    public McpToolBridge(LlmProviderConfig config) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS)));
        this.webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
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

    @SuppressWarnings("unchecked")
    public List<String> getToolNames() {
        return getTools().stream()
                .map(tool -> ((Map<String, Object>) tool.get("function")).get("name").toString())
                .toList();
    }

    public String executeTool(String toolName, Object arguments) {
        try {
            log.info("Executing MCP tool: {} with session: {}", toolName, sessionId.get());
            ObjectNode params = objectMapper.createObjectNode();
            params.put("name", toolName);
            params.set("arguments", objectMapper.valueToTree(arguments));

            JsonNode response = callWithSessionRetry("tools/call", params);
            JsonNode content = response.path("result").path("content");
            if (content.isArray() && !content.isEmpty()) {
                String result = content.get(0).path("text").asText("");
                log.info("MCP tool {} completed, result length: {}", toolName, result.length());
                return result;
            }
            JsonNode error = response.path("error");
            if (!error.isMissingNode()) {
                String errorMsg = error.path("message").asText("Unknown MCP error");
                log.warn("MCP tool {} returned error: {}", toolName, errorMsg);
                return "工具执行错误：" + errorMsg;
            }
            return response.path("result").toString();
        } catch (Exception e) {
            log.error("MCP tool execution failed: {}", toolName, e);
            return "工具执行错误：" + e.getMessage();
        }
    }

    private JsonNode callWithSessionRetry(String method, ObjectNode params) {
        try {
            return jsonRpcCall(method, params);
        } catch (WebClientResponseException.BadRequest e) {
            log.warn("MCP session likely expired (400), re-initializing...");
            invalidateCache();
            initializeAndDiscoverTools();
            return jsonRpcCall(method, params);
        }
    }

    public record McpResource(String uri, String name, String description) {}

    public List<McpResource> getResources() {
        if (cachedResources != null) return cachedResources;
        synchronized (this) {
            if (cachedResources != null) return cachedResources;
            getTools();
            discoverResources();
            return cachedResources != null ? cachedResources : List.of();
        }
    }

    public String readResource(String uri) {
        try {
            ObjectNode params = objectMapper.createObjectNode();
            params.put("uri", uri);
            JsonNode response = callWithSessionRetry("resources/read", params);
            JsonNode contents = response.path("result").path("contents");
            if (contents.isArray() && !contents.isEmpty()) {
                return contents.get(0).path("text").asText("");
            }
            return "";
        } catch (Exception e) {
            log.warn("Failed to read MCP resource {}: {}", uri, e.getMessage());
            return "";
        }
    }

    public void invalidateCache() {
        synchronized (this) {
            cachedTools = null;
            cachedResources = null;
            sessionId.set(null);
        }
    }

    public void refreshTools() {
        invalidateCache();
        getTools();
    }

    private void discoverResources() {
        try {
            JsonNode response = jsonRpcCall("resources/list", objectMapper.createObjectNode());
            JsonNode resourcesArray = response.path("result").path("resources");
            List<McpResource> resources = new ArrayList<>();
            if (resourcesArray.isArray()) {
                for (JsonNode r : resourcesArray) {
                    resources.add(new McpResource(
                        r.path("uri").asText(),
                        r.path("name").asText(),
                        r.path("description").asText("")
                    ));
                }
            }
            cachedResources = resources;
            log.info("Discovered {} MCP resources", resources.size());
        } catch (Exception e) {
            log.warn("Failed to discover MCP resources: {}", e.getMessage());
            cachedResources = List.of();
        }
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
            .block(CALL_TIMEOUT);

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
