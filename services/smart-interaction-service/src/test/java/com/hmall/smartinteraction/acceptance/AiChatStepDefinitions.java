package com.hmall.smartinteraction.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hmall.smartinteraction.acceptance.config.SseEvent;
import com.hmall.smartinteraction.infrastructure.McpToolBridge;
import io.cucumber.java.Before;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.那么;
import io.cucumber.java.zh_cn.并且;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AiChatStepDefinitions {

    @LocalServerPort
    private int port;

    @Autowired @Qualifier("llmWireMock")
    private WireMockServer llmWireMock;

    @Autowired @Qualifier("mcpWireMock")
    private WireMockServer mcpWireMock;

    @Autowired
    private McpToolBridge mcpToolBridge;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final List<String> registeredTools = new ArrayList<>();
    private final Map<String, String> mcpToolResults = new LinkedHashMap<>();

    private final List<LlmRoundSetup> llmRounds = new ArrayList<>();

    private List<SseEvent> receivedEvents;
    private HttpResponse<String> lastResponse;

    @Before
    public void reset() {
        llmWireMock.resetAll();
        mcpWireMock.resetAll();
        registeredTools.clear();
        mcpToolResults.clear();
        llmRounds.clear();
        receivedEvents = null;
        lastResponse = null;
        mcpToolBridge.invalidateCache();
    }

    // ─── Given: LLM setup ───

    @假如("LLM 会流式返回文本 {string}")
    public void llm会流式返回文本(String text) {
        llmRounds.add(LlmRoundSetup.textReply(text));
    }

    @假如("LLM 会返回 tool call {string} 参数 {string}")
    public void llm会返回toolCall(String toolName, String args) {
        llmRounds.add(LlmRoundSetup.toolCall(toolName, args));
    }

    @并且("LLM 收到工具结果后会回复 {string}")
    public void llm收到工具结果后会回复(String text) {
        llmRounds.add(LlmRoundSetup.textReply(text));
    }

    @假如("LLM 第 {int} 轮会返回 tool call {string} 参数 {string}")
    public void llm第n轮会返回toolCall(int round, String toolName, String args) {
        llmRounds.add(LlmRoundSetup.toolCall(toolName, args));
    }

    @并且("LLM 第 {int} 轮会回复 {string}")
    public void llm第n轮会回复(int round, String text) {
        llmRounds.add(LlmRoundSetup.textReply(text));
    }

    // ─── Given: MCP setup ───

    @假如("MCP 已注册工具 {string}")
    public void mcp已注册工具(String toolName) {
        registeredTools.add(toolName);
    }

    @并且("MCP 执行 {string} 将返回 {string}")
    public void mcp执行将返回(String toolName, String result) {
        mcpToolResults.put(toolName, result);
    }

    // ─── When ───

    @当("用户发送消息 {string}")
    public void 用户发送消息(String content) throws Exception {
        setupMcpStubs();
        setupLlmStubs();

        String body = objectMapper.writeValueAsString(Map.of(
            "messages", List.of(Map.of("role", "user", "content", content)),
            "provider", "qwen"
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/ai/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        receivedEvents = parseSseEvents(response.body());
    }

    @当("用户请求可用模型列表")
    public void 用户请求可用模型列表() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/ai/models"))
            .GET()
            .build();

        lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ─── Then ───

    @那么("SSE 流应包含 delta 事件")
    public void sse流应包含delta事件() {
        assertThat(receivedEvents).anyMatch(e -> "delta".equals(e.event()));
    }

    @并且("拼接所有 delta 内容为 {string}")
    public void 拼接所有delta内容为(String expected) throws Exception {
        String allContent = receivedEvents.stream()
            .filter(e -> "delta".equals(e.event()))
            .map(e -> {
                try {
                    return objectMapper.readTree(e.data()).path("content").asText("");
                } catch (Exception ex) { return ""; }
            })
            .collect(Collectors.joining());
        assertThat(allContent).isEqualTo(expected);
    }

    @并且("SSE 流应以 done 事件结束")
    public void sse流应以done事件结束() {
        assertThat(receivedEvents).last().satisfies(e ->
            assertThat(e.event()).isEqualTo("done")
        );
    }

    @那么("SSE 流应包含 tool_call 事件 工具名为 {string}")
    public void sse流应包含toolCall事件(String toolName) throws Exception {
        boolean found = receivedEvents.stream()
            .filter(e -> "tool_call".equals(e.event()))
            .anyMatch(e -> {
                try {
                    return toolName.equals(objectMapper.readTree(e.data()).path("name").asText());
                } catch (Exception ex) { return false; }
            });
        assertThat(found).as("Expected tool_call event for " + toolName).isTrue();
    }

    @并且("SSE 流应包含 tool_result 事件 工具名为 {string}")
    public void sse流应包含toolResult事件(String toolName) throws Exception {
        boolean found = receivedEvents.stream()
            .filter(e -> "tool_result".equals(e.event()))
            .anyMatch(e -> {
                try {
                    return toolName.equals(objectMapper.readTree(e.data()).path("name").asText());
                } catch (Exception ex) { return false; }
            });
        assertThat(found).as("Expected tool_result event for " + toolName).isTrue();
    }

    @那么("应返回模型列表包含 {string}")
    public void 应返回模型列表包含(String modelId) throws Exception {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
        var json = objectMapper.readTree(lastResponse.body());
        boolean found = false;
        for (var model : json.path("models")) {
            if (modelId.equals(model.path("id").asText())) { found = true; break; }
        }
        assertThat(found).as("Expected model " + modelId + " in response").isTrue();
    }

    // ─── Stub helpers ───

    private void setupMcpStubs() throws Exception {
        mcpWireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/mcp"))
            .withRequestBody(WireMock.containing("\"method\":\"initialize\""))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("mcp-session-id", "test-session")
                .withBody("""
                    {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-03-26","capabilities":{"tools":{}},"serverInfo":{"name":"test-mcp","version":"0.1.0"}}}
                    """)));

        mcpWireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/mcp"))
            .withRequestBody(WireMock.containing("\"method\":\"notifications/initialized\""))
            .willReturn(WireMock.aResponse().withStatus(202)));

        List<Map<String, Object>> tools = registeredTools.stream()
            .map(name -> Map.<String, Object>of(
                "name", name,
                "description", "Test tool " + name,
                "inputSchema", Map.of("type", "object", "properties", Map.of())
            ))
            .toList();
        String toolsJson = objectMapper.writeValueAsString(
            Map.of("jsonrpc", "2.0", "id", 1, "result", Map.of("tools", tools))
        );
        mcpWireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/mcp"))
            .withRequestBody(WireMock.containing("\"method\":\"tools/list\""))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("mcp-session-id", "test-session")
                .withBody(toolsJson)));

        for (var entry : mcpToolResults.entrySet()) {
            String resultJson = objectMapper.writeValueAsString(
                Map.of("jsonrpc", "2.0", "id", 1, "result",
                    Map.of("content", List.of(Map.of("type", "text", "text", entry.getValue()))))
            );
            mcpWireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/mcp"))
                .withRequestBody(WireMock.containing("\"method\":\"tools/call\""))
                .withRequestBody(WireMock.containing("\"name\":\"" + entry.getKey() + "\""))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("mcp-session-id", "test-session")
                    .withBody(resultJson)));
        }
    }

    private void setupLlmStubs() {
        String scenarioName = "llm-conversation";
        String currentState = "Started";

        for (int i = 0; i < llmRounds.size(); i++) {
            LlmRoundSetup round = llmRounds.get(i);
            String nextState = "round-" + (i + 1);
            String sseBody = round.isToolCall
                ? buildToolCallSseResponse(round.toolName, round.toolArgs, "call_" + i)
                : buildTextSseResponse(round.text);

            var stub = WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(i == 0 ? com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED : currentState)
                .willSetStateTo(nextState)
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/event-stream")
                    .withBody(sseBody));

            llmWireMock.stubFor(stub);
            currentState = nextState;
        }
    }

    private String buildTextSseResponse(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("data: ").append(sseChunk(text, null, null, null)).append("\n\n");
        sb.append("data: ").append(sseChunkFinish("stop")).append("\n\n");
        sb.append("data: [DONE]\n\n");
        return sb.toString();
    }

    private String buildToolCallSseResponse(String toolName, String toolArgs, String callId) {
        StringBuilder sb = new StringBuilder();
        sb.append("data: ").append(sseChunkToolCall(callId, toolName, "")).append("\n\n");
        sb.append("data: ").append(sseChunkToolCallArgs(toolArgs)).append("\n\n");
        sb.append("data: ").append(sseChunkFinish("tool_calls")).append("\n\n");
        sb.append("data: [DONE]\n\n");
        return sb.toString();
    }

    private String sseChunk(String content, String toolCallId, String fnName, String fnArgs) {
        try {
            Map<String, Object> delta = new LinkedHashMap<>();
            if (content != null) delta.put("content", content);
            Map<String, Object> choice = Map.of("index", 0, "delta", delta, "finish_reason", JSONNull.INSTANCE);
            Map<String, Object> root = Map.of("choices", List.of(choice));
            return objectMapper.writeValueAsString(root).replace("\"__null__\"", "null");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String sseChunkToolCall(String id, String name, String args) {
        return """
            {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"%s","type":"function","function":{"name":"%s","arguments":"%s"}}]},"finish_reason":null}]}""".formatted(id, name, args);
    }

    private String sseChunkToolCallArgs(String args) {
        String escaped = args.replace("\"", "\\\"");
        return """
            {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"%s"}}]},"finish_reason":null}]}""".formatted(escaped);
    }

    private String sseChunkFinish(String reason) {
        return """
            {"choices":[{"index":0,"delta":{},"finish_reason":"%s"}]}""".formatted(reason);
    }

    // ─── SSE parsing ───

    private List<SseEvent> parseSseEvents(String body) {
        List<SseEvent> events = new ArrayList<>();
        String currentEvent = null;
        for (String line : body.split("\n")) {
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
            } else if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                events.add(new SseEvent(currentEvent, data));
                currentEvent = null;
            }
        }
        return events;
    }

    // ─── Internal types ───

    private record LlmRoundSetup(boolean isToolCall, String text, String toolName, String toolArgs) {
        static LlmRoundSetup textReply(String text) { return new LlmRoundSetup(false, text, null, null); }
        static LlmRoundSetup toolCall(String name, String args) { return new LlmRoundSetup(true, null, name, args); }
    }

    private enum JSONNull { INSTANCE;
        @Override public String toString() { return "__null__"; }
    }
}
