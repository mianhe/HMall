package com.hmall.smartinteraction.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.smartinteraction.api.dto.ChatEvent;
import com.hmall.smartinteraction.api.dto.ChatRequest;
import com.hmall.smartinteraction.infrastructure.LlmClient;
import com.hmall.smartinteraction.infrastructure.LlmProviderConfig;
import com.hmall.smartinteraction.infrastructure.McpToolBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int DEFAULT_MAX_TOOL_CALL_ROUNDS = 100;

    static final String NO_TOOLS_DIRECTIVE = """
            ## 重要：当前没有可用工具

            你当前没有被授予任何数据查询工具。这意味着你无法获取任何真实的商品、价格、库存、订单等数据。

            你必须严格遵守以下规则：
            - 绝对不要编造、猜测或从记忆中回忆任何具体的商品名称、价格、型号、库存数量、订单信息
            - 绝对不要列举任何具体的产品列表，即使用户直接要求
            - 对于任何需要查询数据才能回答的问题，请回复："抱歉，我目前无法查询相关信息。请通过页面直接浏览，或稍后再试。"
            - 你可以回答不需要数据查询的一般性问题（如网站使用指引、购物流程说明等）
            """;

    private final LlmClient llmClient;
    private final McpToolBridge mcpToolBridge;
    private final LlmProviderConfig config;
    private final SkillRepository skillRepository;
    private final SettingsRepository settingsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String DEFAULT_ADMIN_BASE_PROMPT = """
            你是 HMall 智能助手，帮助管理员通过自然语言管理电商系统。

            规则：
            - 必须通过工具获取数据，严禁编造
            - 工具返回的价格已是人民币元，直接展示即可（如 ¥5199.00），无需再做转换
            - 删除操作前向用户确认
            - 用中文回复
            """;

    public static final String DEFAULT_CONSUMER_BASE_PROMPT = """
            你是 HMall 购物助手，帮用户找商品、管购物车、下单。语气友好自然。

            规则：
            - 必须通过工具获取数据，严禁编造商品名、价格、skuId、orderId
            - 没有工具时回复："抱歉，我目前无法查询。请通过页面浏览或稍后再试。"
            - 工具返回的价格已是人民币元，直接展示即可（如 ¥5199.00），无需再做转换
            - 加购和下单前确认用户选择的 SKU 和数量
            - 不执行管理操作（不创建/修改/删除商品和类目）
            - 用中文回复
            """;

    public AiChatService(LlmClient llmClient, McpToolBridge mcpToolBridge,
                         LlmProviderConfig config, SkillRepository skillRepository,
                         SettingsRepository settingsRepository) {
        this.llmClient = llmClient;
        this.mcpToolBridge = mcpToolBridge;
        this.config = config;
        this.skillRepository = skillRepository;
        this.settingsRepository = settingsRepository;
    }

    public void chat(ChatRequest request, Long userId, SseEmitter emitter) {
        try {
            var provider = config.resolveProvider(request.provider());
            List<Skill> matchedSkills;
            boolean manualSelection;

            if (request.skillId() != null) {
                Skill skill = skillRepository.findById(request.skillId()).orElse(null);
                matchedSkills = skill != null ? List.of(skill) : List.of();
                manualSelection = true;
            } else if ("none".equals(request.skillMode())) {
                matchedSkills = List.of();
                manualSelection = false;
            } else {
                Skill defaultSkill = skillRepository.findDefault().orElse(null);
                if (defaultSkill != null) {
                    matchedSkills = List.of(defaultSkill);
                    manualSelection = true;
                } else {
                    matchedSkills = resolveAutoMatchedSkills(provider, request);
                    manualSelection = false;
                    if (!matchedSkills.isEmpty()) {
                        String skillsJson = serializeMatchedSkills(matchedSkills);
                        sendEvent(emitter, "skill_matched", ChatEvent.skillMatched(skillsJson));
                    }
                }
            }

            List<Map<String, Object>> tools;
            if (manualSelection) {
                tools = resolveFilteredTools(matchedSkills);
            } else if (request.clientType() != null && !request.clientType().isBlank()) {
                tools = matchedSkills.isEmpty() ? List.of() : resolveUnionFilteredTools(matchedSkills);
            } else {
                tools = mcpToolBridge.getTools();
            }
            String providerId = request.provider() != null && !request.provider().isBlank()
                ? request.provider() : config.defaultProvider();
            var messages = buildMessages(request, matchedSkills, !tools.isEmpty(), providerId, provider.model());
            int maxRounds = resolveMaxRounds(request.maxToolCallRounds());
            streamWithToolCallLoop(provider, messages, tools, maxRounds, userId, emitter);
        } catch (Exception e) {
            log.error("Chat error", e);
            sendEvent(emitter, "error", ChatEvent.error(e.getMessage()));
            completeEmitter(emitter);
        }
    }

    @Value("${smart-interaction.routing-tool-threshold:20}")
    private int routingToolThreshold;

    private List<Skill> resolveAutoMatchedSkills(LlmProviderConfig.Provider provider, ChatRequest request) {
        String clientType = request.clientType();
        List<Skill> candidateSkills = skillRepository.findAllOrderByCreatedAtDesc().stream()
                .filter(s -> s.matchesAudience(clientType))
                .toList();

        int toolCount = mcpToolBridge.getTools().size();
        if (toolCount <= routingToolThreshold) {
            log.info("Tool count ({}) <= threshold ({}), skipping LLM routing, using all {} audience-matching Skills",
                    toolCount, routingToolThreshold, candidateSkills.size());
            return candidateSkills;
        }

        return autoMatchSkills(provider, request, clientType);
    }

    private List<Skill> autoMatchSkills(LlmProviderConfig.Provider provider, ChatRequest request, String clientType) {
        List<Skill> allSkills = skillRepository.findAllOrderByCreatedAtDesc();
        List<Skill> candidateSkills = allSkills.stream()
                .filter(s -> s.matchesAudience(clientType))
                .toList();
        if (candidateSkills.isEmpty()) {
            return List.of();
        }

        String routingPrompt = buildRoutingPrompt(candidateSkills);
        String userMessage = request.messages().get(request.messages().size() - 1).content();

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", routingPrompt),
                Map.of("role", "user", "content", userMessage));

        var responseBuffer = new StringBuilder();
        llmClient.streamChat(provider, messages, null)
                .doOnNext(chunk -> {
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String content = choices.get(0).path("delta").path("content").asText(null);
                        if (content != null) responseBuffer.append(content);
                    }
                })
                .blockLast();

        List<String> matchedNames = parseMatchedSkillNames(responseBuffer.toString().trim());
        return candidateSkills.stream()
                .filter(s -> matchedNames.contains(s.getName()))
                .toList();
    }

    private String buildRoutingPrompt(List<Skill> skills) {
        var sb = new StringBuilder("你是 Skill 路由器。根据用户消息判断需要哪些领域知识。\n\n可选 Skill：\n");
        for (Skill s : skills) {
            sb.append("- ").append(s.getName());
            if (s.getDescription() != null && !s.getDescription().isBlank()) {
                sb.append("：").append(s.getDescription());
            }
            sb.append("\n");
        }
        sb.append("\n仅返回匹配的 Skill 名称，JSON 数组格式。无匹配返回 []。不要解释。");
        return sb.toString();
    }

    private List<String> parseMatchedSkillNames(String response) {
        try {
            JsonNode arr = objectMapper.readTree(response);
            if (arr.isArray()) {
                List<String> names = new ArrayList<>();
                for (JsonNode n : arr) {
                    names.add(n.asText());
                }
                return names;
            }
        } catch (Exception e) {
            log.warn("Failed to parse routing response: {}", response);
        }
        return List.of();
    }

    private String serializeMatchedSkills(List<Skill> skills) {
        try {
            List<Map<String, Object>> list = skills.stream()
                    .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName()))
                    .toList();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Map<String, Object>> resolveFilteredTools(List<Skill> skills) {
        var allTools = mcpToolBridge.getTools();
        if (skills.isEmpty()) return allTools;

        Skill skill = skills.get(0);
        List<String> allToolNames = mcpToolBridge.getToolNames();
        List<String> allowedNames = skill.matchTools(allToolNames);

        return allTools.stream()
                .filter(tool -> {
                    @SuppressWarnings("unchecked")
                    var fn = (Map<String, Object>) tool.get("function");
                    return fn != null && allowedNames.contains(fn.get("name"));
                })
                .toList();
    }

    private List<Map<String, Object>> resolveUnionFilteredTools(List<Skill> skills) {
        if (skills.isEmpty()) return List.of();
        var allTools = mcpToolBridge.getTools();
        List<String> allToolNames = mcpToolBridge.getToolNames();

        var unionAllowed = new java.util.HashSet<String>();
        for (Skill skill : skills) {
            unionAllowed.addAll(skill.matchTools(allToolNames));
        }

        return allTools.stream()
                .filter(tool -> {
                    @SuppressWarnings("unchecked")
                    var fn = (Map<String, Object>) tool.get("function");
                    return fn != null && unionAllowed.contains(fn.get("name"));
                })
                .toList();
    }

    private int resolveMaxRounds(Integer requestMaxRounds) {
        if (requestMaxRounds != null && requestMaxRounds > 0) {
            return requestMaxRounds;
        }
        return DEFAULT_MAX_TOOL_CALL_ROUNDS;
    }

    private void streamWithToolCallLoop(LlmProviderConfig.Provider provider,
                                        List<Map<String, Object>> messages,
                                        List<Map<String, Object>> tools,
                                        int maxRounds,
                                        Long userId,
                                        SseEmitter emitter) {
        for (int round = 0; round < maxRounds; round++) {
            log.info("LLM call round {} starting (provider={}, model={})", round, provider.baseUrl(), provider.model());
            if (round == 0) {
                logMessages(messages, tools);
            }
            var result = streamOnce(provider, messages, tools, emitter);
            log.info("LLM call round {} finished, toolCalls={}", round, result.toolCalls.size());
            if (result.toolCalls.isEmpty()) {
                sendEvent(emitter, "done", ChatEvent.done());
                completeEmitter(emitter);
                return;
            }
            appendAssistantToolCallMessage(messages, result);
            executeToolCalls(messages, result.toolCalls, userId, emitter);
        }
        sendEvent(emitter, "error", ChatEvent.error("Too many tool call rounds"));
        sendEvent(emitter, "done", ChatEvent.done());
        completeEmitter(emitter);
    }

    private void appendAssistantToolCallMessage(List<Map<String, Object>> messages, StreamResult result) {
        var rawCalls = result.toolCalls.stream()
            .map(tc -> Map.<String, Object>of(
                "id", tc.id(),
                "type", "function",
                "function", Map.of("name", tc.name(), "arguments", tc.arguments())
            ))
            .toList();

        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        msg.put("content", result.content != null ? result.content : "");
        msg.put("tool_calls", rawCalls);
        messages.add(msg);
    }

    private void executeToolCalls(List<Map<String, Object>> messages,
                                  List<ToolCall> toolCalls,
                                  Long userId,
                                  SseEmitter emitter) {
        for (var tc : toolCalls) {
            Object args = parseArguments(tc.arguments());
            log.info("Tool call: {} | args: {}", tc.name(), tc.arguments());
            sendEvent(emitter, "tool_call", ChatEvent.toolCall(tc.id(), tc.name(), args));

            Object argsForMcp = injectUserId(args, userId);
            var execResult = mcpToolBridge.executeTool(tc.name(), argsForMcp);
            log.info("Tool result: {} | {}", tc.name(), execResult.textForLlm());
            sendEvent(emitter, "tool_result", ChatEvent.toolResult(tc.id(), tc.name(), execResult.resultForClient()));

            messages.add(Map.of("role", "tool", "tool_call_id", tc.id(), "content", execResult.textForLlm()));
        }
    }

    @SuppressWarnings("unchecked")
    private Object injectUserId(Object args, Long userId) {
        if (userId == null) return args;
        if (args instanceof Map) {
            var map = new HashMap<String, Object>((Map<String, Object>) args);
            map.put("userId", userId);
            return map;
        }
        return args;
    }

    private StreamResult streamOnce(LlmProviderConfig.Provider provider,
                                    List<Map<String, Object>> messages,
                                    List<Map<String, Object>> tools,
                                    SseEmitter emitter) {
        var contentBuffer = new StringBuilder();
        var accumulators = new TreeMap<Integer, ToolCallAccumulator>();

        llmClient.streamChat(provider, messages, tools.isEmpty() ? null : tools)
            .doOnNext(chunk -> processChunk(chunk, contentBuffer, accumulators, emitter))
            .doOnError(e -> log.error("LLM stream error", e))
            .blockLast();

        var validToolCalls = accumulators.values().stream()
            .filter(ToolCallAccumulator::isValid)
            .map(ToolCallAccumulator::toToolCall)
            .toList();

        return new StreamResult(contentBuffer.toString(), validToolCalls);
    }

    private void processChunk(JsonNode chunk,
                              StringBuilder contentBuffer,
                              Map<Integer, ToolCallAccumulator> accumulators,
                              SseEmitter emitter) {
        JsonNode choices = chunk.path("choices");
        if (!choices.isArray() || choices.isEmpty()) return;

        JsonNode delta = choices.get(0).path("delta");

        String content = delta.path("content").asText(null);
        if (content != null && !content.isEmpty()) {
            contentBuffer.append(content);
            sendEvent(emitter, "delta", ChatEvent.delta(content));
        }

        JsonNode toolCallsNode = delta.path("tool_calls");
        if (toolCallsNode.isArray()) {
            for (JsonNode tc : toolCallsNode) {
                int index = tc.path("index").asInt(0);
                var acc = accumulators.computeIfAbsent(index, k -> new ToolCallAccumulator());
                acc.accumulate(tc);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void logMessages(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        var sb = new StringBuilder("\n========== MESSAGES SENT TO LLM ==========\n");
        for (int i = 0; i < messages.size(); i++) {
            var msg = messages.get(i);
            String role = String.valueOf(msg.get("role"));
            String content = String.valueOf(msg.get("content"));
            sb.append(String.format("[%d] role=%s\n%s\n---\n", i, role, content));
        }
        sb.append(String.format("tools count: %d", tools.size()));
        if (!tools.isEmpty()) {
            sb.append(" → ");
            for (var tool : tools) {
                var fn = (Map<String, Object>) tool.get("function");
                if (fn != null) sb.append(fn.get("name")).append(", ");
            }
        }
        sb.append("\n==========================================");
        log.info(sb.toString());
    }

    private Object parseArguments(String argsStr) {
        try {
            return objectMapper.readValue(argsStr, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> buildMessages(ChatRequest request,
                                                     List<Skill> skills,
                                                     boolean hasTools,
                                                     String providerId,
                                                     String modelName) {
        List<Map<String, Object>> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt(request, skills, hasTools, providerId, modelName);
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (ChatRequest.Message msg : request.messages()) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }
        return messages;
    }

    private String buildSystemPrompt(ChatRequest request, List<Skill> skills,
                                     boolean hasTools,
                                     String providerId,
                                     String modelName) {
        String basePrompt = buildDefaultSystemPrompt(request);
        var sb = new StringBuilder(basePrompt);
        sb.append("\n\n---\n当前实际调用的模型由系统配置决定，本次为：").append(providerId).append("（").append(modelName).append("）。若用户询问你使用的模型，请如实回答此信息，不要编造其他型号（如不要自称 Claude）。");

        if (!hasTools) {
            sb.append("\n\n").append(NO_TOOLS_DIRECTIVE);
        }

        String resourceKnowledge = resolveResourceKnowledge(skills);
        if (!resourceKnowledge.isEmpty()) {
            sb.append("\n\n---\n以下是当前对话匹配到的领域知识（来自 MCP Resources）：\n\n");
            sb.append(resourceKnowledge);
        }

        List<String> supplements = skills.stream()
                .map(Skill::getSystemPrompt)
                .filter(p -> p != null && !p.isBlank())
                .toList();

        if (!supplements.isEmpty()) {
            sb.append("\n\n---\n以下是当前对话匹配到的操作指引与示例：\n");
            for (String supplement : supplements) {
                sb.append("\n").append(supplement).append("\n");
            }
        }

        return sb.toString();
    }

    private String resolveResourceKnowledge(List<Skill> skills) {
        var allResources = mcpToolBridge.getResources();
        if (allResources.isEmpty()) return "";

        var toolPrefixes = new java.util.HashSet<String>();
        for (Skill skill : skills) {
            for (String pattern : skill.getAllowedTools()) {
                if (pattern.endsWith("*")) {
                    toolPrefixes.add(pattern.substring(0, pattern.length() - 1));
                } else if (pattern.contains("_")) {
                    toolPrefixes.add(pattern.substring(0, pattern.indexOf('_') + 1));
                }
            }
        }

        var matchedUris = new java.util.LinkedHashSet<String>();
        for (var resource : allResources) {
            String uri = resource.uri();
            if (uri.contains("/ontology/")) {
                matchedUris.add(uri);
                continue;
            }
            for (String prefix : toolPrefixes) {
                String domain = prefix.replace("_", "");
                if (uri.contains(domain) || uri.contains(prefix.replace("_", "-"))) {
                    matchedUris.add(uri);
                    break;
                }
            }
        }

        if (skills.isEmpty() || toolPrefixes.isEmpty()) {
            allResources.forEach(r -> matchedUris.add(r.uri()));
        }

        var sb = new StringBuilder();
        for (String uri : matchedUris) {
            String content = mcpToolBridge.readResource(uri);
            if (!content.isBlank()) {
                sb.append(content).append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    private String buildDefaultSystemPrompt(ChatRequest request) {
        String currentPage = request.context() != null ? request.context().page() : "/";
        String clientType = request.clientType();
        boolean isConsumer = "consumer".equals(clientType);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE);

        String template = resolveBasePromptTemplate(isConsumer);
        var sb = new StringBuilder(template);
        sb.append("\n当前页面：").append(currentPage);
        sb.append("\n当前日期：").append(dateStr).append("（").append(dayOfWeek).append("）");

        var ctx = request.context();
        if (ctx != null && ctx.canvasPanels() != null && !ctx.canvasPanels().isEmpty()) {
            sb.append("\n当前画布展示：");
            var descriptions = ctx.canvasPanels().stream()
                    .map(p -> p.type().toLowerCase() + " \"" + p.title() + "\"")
                    .toList();
            sb.append(String.join("、", descriptions));
        }

        return sb.toString();
    }

    private String resolveBasePromptTemplate(boolean isConsumer) {
        Settings settings = getOrCreateSettings();
        String prompt = isConsumer ? settings.getConsumerBasePrompt() : settings.getAdminBasePrompt();
        return (prompt != null && !prompt.isBlank()) ? prompt
                : (isConsumer ? DEFAULT_CONSUMER_BASE_PROMPT : DEFAULT_ADMIN_BASE_PROMPT);
    }

    private Settings getOrCreateSettings() {
        return settingsRepository.find().orElseGet(() ->
                settingsRepository.save(Settings.createDefault(
                        DEFAULT_ADMIN_BASE_PROMPT, DEFAULT_CONSUMER_BASE_PROMPT)));
    }

    private void sendEvent(SseEmitter emitter, String eventName, ChatEvent event) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(event));
        } catch (Exception e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }

    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.debug("Emitter already completed");
        }
    }

    private record ToolCall(String id, String name, String arguments) {}

    private record StreamResult(String content, List<ToolCall> toolCalls) {}

    private static class ToolCallAccumulator {
        String id;
        String name;
        final StringBuilder arguments = new StringBuilder();

        void accumulate(JsonNode tc) {
            String tcId = tc.path("id").asText(null);
            if (tcId != null && !tcId.isBlank()) this.id = tcId;

            JsonNode fn = tc.path("function");
            String fnName = fn.path("name").asText(null);
            if (fnName != null && !fnName.isBlank()) this.name = fnName;

            String fnArgs = fn.path("arguments").asText(null);
            if (fnArgs != null) this.arguments.append(fnArgs);
        }

        boolean isValid() {
            return id != null && !id.isBlank() && name != null && !name.isBlank();
        }

        ToolCall toToolCall() {
            return new ToolCall(id, name, arguments.toString());
        }
    }
}
