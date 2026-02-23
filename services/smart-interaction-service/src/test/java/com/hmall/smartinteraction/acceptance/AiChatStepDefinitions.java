package com.hmall.smartinteraction.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hmall.smartinteraction.acceptance.config.SseEvent;
import com.hmall.smartinteraction.application.SkillApplicationService;
import com.hmall.smartinteraction.domain.Skill;
import com.hmall.smartinteraction.domain.SkillRepository;
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
import java.util.Arrays;
import java.util.HashMap;
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

    @Autowired
    private SkillApplicationService skillService;

    @Autowired
    private SkillRepository skillRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final List<String> registeredTools = new ArrayList<>();
    private final Map<String, String> mcpToolResults = new LinkedHashMap<>();

    private final List<LlmRoundSetup> llmRounds = new ArrayList<>();
    private boolean llmAlwaysToolCall = false;

    private List<SseEvent> receivedEvents;
    private HttpResponse<String> lastResponse;
    private Long currentSkillId;
    private int overrideMaxRounds = -1;

    private final Map<String, Long> skillNameToId = new HashMap<>();
    private List<String> routingMatchedSkillNames = null;
    private String overrideSkillMode = null;
    private String overrideClientType = null;
    private Long overrideUserId = null;
    private boolean explicitlyNoUserId = false;

    @Before("not @skill")
    public void reset() {
        llmWireMock.resetAll();
        mcpWireMock.resetAll();
        registeredTools.clear();
        mcpToolResults.clear();
        llmRounds.clear();
        llmAlwaysToolCall = false;
        receivedEvents = null;
        lastResponse = null;
        currentSkillId = null;
        overrideMaxRounds = -1;
        skillNameToId.clear();
        routingMatchedSkillNames = null;
        overrideSkillMode = null;
        overrideClientType = null;
        overrideUserId = null;
        explicitlyNoUserId = false;
        mcpToolBridge.invalidateCache();
        skillRepository.findAllOrderByCreatedAtDesc().forEach(s -> skillRepository.deleteById(s.getId()));
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

    @假如("LLM 每轮都会返回 tool call {string} 参数 {string}")
    public void llm每轮都会返回toolCall(String toolName, String args) {
        llmAlwaysToolCall = true;
        llmRounds.add(LlmRoundSetup.toolCall(toolName, args));
    }

    // ─── Given: Skill setup ───

    @假如("已创建 Skill {string} systemPrompt {string} allowedTools {string}")
    public void 已创建Skill(String name, String systemPrompt, String allowedTools) {
        Skill skill = new Skill(name, name, systemPrompt,
                Arrays.asList(allowedTools.split("\\s*,\\s*")));
        skill = skillRepository.save(skill);
        currentSkillId = skill.getId();
    }

    @假如("已创建默认 Skill {string} systemPrompt {string}")
    public void 已创建默认Skill(String name, String systemPrompt) {
        Skill skill = new Skill(name, name, systemPrompt, List.of("*"));
        skill = skillRepository.save(skill);
        skill.setAsDefault();
        skillRepository.save(skill);
        currentSkillId = skill.getId();
    }

    @假如("已创建 Skill {string} 描述 {string} systemPrompt {string} allowedTools {string}")
    public void 已创建SkillWithDesc(String name, String description, String systemPrompt, String allowedTools) {
        已创建SkillWithDescAndAudience(name, description, systemPrompt, allowedTools, "all");
    }

    @假如("已创建 Skill {string} 描述 {string} systemPrompt {string} allowedTools {string} audience {string}")
    public void 已创建SkillWithDescAndAudience(String name, String description, String systemPrompt, String allowedTools, String audience) {
        Skill skill = new Skill(name, description, systemPrompt,
                Arrays.asList(allowedTools.split("\\s*,\\s*")), audience);
        skill = skillRepository.save(skill);
        skillNameToId.put(name, skill.getId());
        currentSkillId = skill.getId();
    }

    @假如("系统中无任何 Skill")
    public void 系统中无任何Skill() {
        skillRepository.findAllOrderByCreatedAtDesc().forEach(s -> skillRepository.deleteById(s.getId()));
    }

    // ─── Given: LLM routing setup ───

    @假如("LLM 路由会将消息匹配到 Skill {string}")
    public void llm路由匹配单个Skill(String skillName) {
        routingMatchedSkillNames = List.of(skillName);
    }

    @假如("LLM 路由会将消息匹配到 Skill {string} 和 {string}")
    public void llm路由匹配多个Skill(String skillName1, String skillName2) {
        routingMatchedSkillNames = List.of(skillName1, skillName2);
    }

    @假如("LLM 路由未匹配到任何 Skill")
    public void llm路由未匹配() {
        routingMatchedSkillNames = List.of();
    }

    @假如("已设置 maxToolCallRounds 为 {int}")
    public void 已设置maxToolCallRounds为(int rounds) {
        overrideMaxRounds = rounds;
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
        sendChat(content, null);
    }

    @当("用户使用 Skill {string} 发送消息 {string}")
    public void 用户使用Skill发送消息(String skillName, String content) throws Exception {
        Long skillId = skillNameToId.getOrDefault(skillName, currentSkillId);
        sendChat(content, skillId);
    }

    private void sendChat(String content, Long skillId) throws Exception {
        setupMcpStubs();
        setupLlmStubs();
        llmWireMock.resetRequests();

        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("messages", List.of(Map.of("role", "user", "content", content)));
        bodyMap.put("provider", "qwen");
        if (skillId != null) bodyMap.put("skillId", skillId);
        if (overrideSkillMode != null) bodyMap.put("skillMode", overrideSkillMode);
        if (overrideClientType != null) bodyMap.put("clientType", overrideClientType);
        if (overrideMaxRounds > 0) bodyMap.put("maxToolCallRounds", overrideMaxRounds);

        String body = objectMapper.writeValueAsString(bodyMap);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/ai/chat"))
            .header("Content-Type", "application/json");

        if (overrideUserId != null) {
            requestBuilder.header("X-User-Id", String.valueOf(overrideUserId));
        } else if ("consumer".equals(overrideClientType) && !explicitlyNoUserId) {
            requestBuilder.header("X-User-Id", "1");
        }

        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        lastResponse = response;
        if (response.statusCode() == 200) {
            receivedEvents = parseSseEvents(response.body());
        } else {
            receivedEvents = List.of();
        }
    }

    @当("用户不指定 Skill 发送消息 {string}")
    public void 用户不指定Skill发送消息(String content) throws Exception {
        sendChat(content, null);
    }

    @当("用户以无 Skill 模式发送消息 {string}")
    public void 用户以无Skill模式发送消息(String content) throws Exception {
        overrideSkillMode = "none";
        sendChat(content, null);
    }

    @当("消费者端用户发送消息 {string}")
    public void 消费者端用户发送消息(String content) throws Exception {
        overrideClientType = "consumer";
        sendChat(content, null);
    }

    @当("消费者端用户 {long} 发送消息 {string}")
    public void 消费者端用户n发送消息(long userId, String content) throws Exception {
        overrideClientType = "consumer";
        overrideUserId = userId;
        sendChat(content, null);
    }

    @当("未认证消费者端用户发送消息 {string}")
    public void 未认证消费者端用户发送消息(String content) throws Exception {
        overrideClientType = "consumer";
        explicitlyNoUserId = true;
        sendChat(content, null);
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

    @那么("发送给 LLM 的 system prompt 应包含 {string}")
    public void 发送给LLM的systemPrompt应包含(String expected) throws Exception {
        String systemPrompt = extractSystemPromptFromMainChat();
        assertThat(systemPrompt).as("system prompt should contain: " + expected).contains(expected);
    }

    @并且("发送给 LLM 的 tools 应包含 {string}")
    public void 发送给LLM的tools应包含(String toolName) throws Exception {
        List<String> toolNames = extractToolNamesFromLlmRequests();
        assertThat(toolNames).as("tools sent to LLM should contain: " + toolName).contains(toolName);
    }

    @并且("发送给 LLM 的 tools 不应包含 {string}")
    public void 发送给LLM的tools不应包含(String toolName) throws Exception {
        List<String> toolNames = extractToolNamesFromLlmRequests();
        assertThat(toolNames).as("tools sent to LLM should NOT contain: " + toolName).doesNotContain(toolName);
    }

    @那么("SSE 流应包含 error 事件")
    public void sse流应包含error事件() {
        assertThat(receivedEvents).anyMatch(e -> "error".equals(e.event()));
    }

    @那么("SSE 流应包含 skill_matched 事件")
    public void sse流应包含skillMatched事件() {
        assertThat(receivedEvents).anyMatch(e -> "skill_matched".equals(e.event()));
    }

    @并且("SSE 流不应包含 skill_matched 事件")
    public void sse流不应包含skillMatched事件() {
        assertThat(receivedEvents).noneMatch(e -> "skill_matched".equals(e.event()));
    }

    @那么("LLM 路由的候选 Skill 应包含 {string}")
    public void llm路由候选Skill应包含(String skillName) throws Exception {
        String routingPrompt = extractSystemPromptFromLlmRequests();
        assertThat(routingPrompt).as("routing prompt should contain candidate: " + skillName).contains(skillName);
    }

    @并且("LLM 路由的候选 Skill 不应包含 {string}")
    public void llm路由候选Skill不应包含(String skillName) throws Exception {
        String routingPrompt = extractSystemPromptFromLlmRequests();
        assertThat(routingPrompt).as("routing prompt should NOT contain candidate: " + skillName).doesNotContain(skillName);
    }

    @并且("发送给 LLM 的 system prompt 不应包含 {string}")
    public void 发送给LLM的systemPrompt不应包含(String expected) throws Exception {
        String systemPrompt = extractSystemPromptFromMainChat();
        assertThat(systemPrompt).as("system prompt should NOT contain: " + expected).doesNotContain(expected);
    }

    @那么("MCP 收到的 {string} 调用参数应包含 userId {long}")
    public void mcp收到的调用参数应包含userId(String toolName, long userId) throws Exception {
        List<LoggedRequest> requests = mcpWireMock.findAll(
                WireMock.postRequestedFor(WireMock.urlEqualTo("/mcp"))
                        .withRequestBody(WireMock.containing("\"method\":\"tools/call\""))
                        .withRequestBody(WireMock.containing("\"name\":\"" + toolName + "\"")));
        assertThat(requests).as("Expected MCP tools/call for " + toolName).isNotEmpty();
        JsonNode body = objectMapper.readTree(requests.get(0).getBodyAsString());
        JsonNode args = body.path("params").path("arguments");
        assertThat(args.path("userId").asLong()).as("userId in MCP args").isEqualTo(userId);
    }

    @那么("MCP 收到的 {string} 调用参数不应包含 userId")
    public void mcp收到的调用参数不应包含userId(String toolName) throws Exception {
        List<LoggedRequest> requests = mcpWireMock.findAll(
                WireMock.postRequestedFor(WireMock.urlEqualTo("/mcp"))
                        .withRequestBody(WireMock.containing("\"method\":\"tools/call\""))
                        .withRequestBody(WireMock.containing("\"name\":\"" + toolName + "\"")));
        assertThat(requests).as("Expected MCP tools/call for " + toolName).isNotEmpty();
        JsonNode body = objectMapper.readTree(requests.get(0).getBodyAsString());
        JsonNode args = body.path("params").path("arguments");
        assertThat(args.has("userId")).as("MCP args should not contain userId").isFalse();
    }

    @那么("响应状态码应为 {int}")
    public void 响应状态码应为(int statusCode) {
        assertThat(lastResponse).as("lastResponse should not be null").isNotNull();
        assertThat(lastResponse.statusCode()).isEqualTo(statusCode);
    }

    private int mainChatRequestIndex() {
        return routingMatchedSkillNames != null ? 1 : 0;
    }

    private String extractSystemPromptFromLlmRequests() throws Exception {
        return extractSystemPromptAtIndex(0);
    }

    private String extractSystemPromptFromMainChat() throws Exception {
        return extractSystemPromptAtIndex(mainChatRequestIndex());
    }

    private String extractSystemPromptAtIndex(int index) throws Exception {
        List<LoggedRequest> requests = llmWireMock.findAll(
                WireMock.postRequestedFor(WireMock.urlPathEqualTo("/chat/completions")));
        assertThat(requests).hasSizeGreaterThan(index);
        JsonNode body = objectMapper.readTree(requests.get(index).getBodyAsString());
        for (JsonNode msg : body.path("messages")) {
            if ("system".equals(msg.path("role").asText())) {
                return msg.path("content").asText("");
            }
        }
        return "";
    }

    private List<String> extractToolNamesFromLlmRequests() throws Exception {
        int index = mainChatRequestIndex();
        List<LoggedRequest> requests = llmWireMock.findAll(
                WireMock.postRequestedFor(WireMock.urlPathEqualTo("/chat/completions")));
        assertThat(requests).hasSizeGreaterThan(index);
        JsonNode body = objectMapper.readTree(requests.get(index).getBodyAsString());
        List<String> names = new ArrayList<>();
        for (JsonNode tool : body.path("tools")) {
            names.add(tool.path("function").path("name").asText());
        }
        return names;
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
        if (llmAlwaysToolCall && !llmRounds.isEmpty()) {
            LlmRoundSetup round = llmRounds.get(0);
            String sseBody = buildToolCallSseResponse(round.toolName, round.toolArgs, "call_repeat");
            llmWireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/event-stream")
                    .withBody(sseBody)));
            return;
        }

        String scenarioName = "llm-conversation";
        String currentState = com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

        if (routingMatchedSkillNames != null) {
            try {
                String routingJson = objectMapper.writeValueAsString(routingMatchedSkillNames);
                String routingResponse = buildTextSseResponse(routingJson);
                String nextState = "routing-done";

                llmWireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                    .inScenario(scenarioName)
                    .whenScenarioStateIs(currentState)
                    .willSetStateTo(nextState)
                    .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(routingResponse)));
                currentState = nextState;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 0; i < llmRounds.size(); i++) {
            LlmRoundSetup round = llmRounds.get(i);
            String nextState = "round-" + (i + 1);
            String sseBody = round.isToolCall
                ? buildToolCallSseResponse(round.toolName, round.toolArgs, "call_" + i)
                : buildTextSseResponse(round.text);

            llmWireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(currentState)
                .willSetStateTo(nextState)
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/event-stream")
                    .withBody(sseBody)));
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
