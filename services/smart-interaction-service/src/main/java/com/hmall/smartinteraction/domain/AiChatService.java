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
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_TOOL_CALL_ROUNDS = 100;

    private final LlmClient llmClient;
    private final McpToolBridge mcpToolBridge;
    private final LlmProviderConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiChatService(LlmClient llmClient, McpToolBridge mcpToolBridge, LlmProviderConfig config) {
        this.llmClient = llmClient;
        this.mcpToolBridge = mcpToolBridge;
        this.config = config;
    }

    public void chat(ChatRequest request, SseEmitter emitter) {
        try {
            var provider = config.resolveProvider(request.provider());
            var messages = buildMessages(request);
            var tools = mcpToolBridge.getTools();
            streamWithToolCallLoop(provider, messages, tools, emitter);
        } catch (Exception e) {
            log.error("Chat error", e);
            sendEvent(emitter, "error", ChatEvent.error(e.getMessage()));
            completeEmitter(emitter);
        }
    }

    private void streamWithToolCallLoop(LlmProviderConfig.Provider provider,
                                        List<Map<String, Object>> messages,
                                        List<Map<String, Object>> tools,
                                        SseEmitter emitter) {
        for (int round = 0; round < MAX_TOOL_CALL_ROUNDS; round++) {
            var result = streamOnce(provider, messages, tools, emitter);
            if (result.toolCalls.isEmpty()) {
                sendEvent(emitter, "done", ChatEvent.done());
                completeEmitter(emitter);
                return;
            }
            appendAssistantToolCallMessage(messages, result);
            executeToolCalls(messages, result.toolCalls, emitter);
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
                                  SseEmitter emitter) {
        for (var tc : toolCalls) {
            Object args = parseArguments(tc.arguments());
            sendEvent(emitter, "tool_call", ChatEvent.toolCall(tc.id(), tc.name(), args));

            String toolResult = mcpToolBridge.executeTool(tc.name(), args);
            sendEvent(emitter, "tool_result", ChatEvent.toolResult(tc.id(), tc.name(), toolResult));

            messages.add(Map.of("role", "tool", "tool_call_id", tc.id(), "content", toolResult));
        }
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

    private Object parseArguments(String argsStr) {
        try {
            return objectMapper.readValue(argsStr, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> buildMessages(ChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();

        String currentPage = request.context() != null ? request.context().page() : "/";
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(currentPage)));

        for (ChatRequest.Message msg : request.messages()) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }
        return messages;
    }

    private String buildSystemPrompt(String currentPage) {
        return """
            你是 HMall 智能助手，帮助管理员通过对话管理电商系统。
            
            当前上下文：
            - 用户正在浏览：%s
            
            你的能力：
            - 通过工具操作商品目录（类目、商品、规格、SKU、图片）
            - 回答关于系统操作的问题
            - 引导用户完成复杂的多步操作
            
            数据模型（查询路径）：
            - 类目（Category）是树形结构：根类目 → 子类目 → 叶子类目
            - 商品（SPU）挂在叶子类目下：先 catalog_list_categories 获取类目，再 catalog_list_products(categoryId) 获取商品
            - SKU 挂在商品下：先获取商品 ID，再 catalog_list_skus(spuId) 获取 SKU
            - 要统计所有 SKU，需遍历：所有类目 → 所有子类目 → 所有商品 → 每个商品的 SKU
            - catalog_list_products 需要传 categoryId，必须传叶子类目的 ID（没有子类目的类目）
            - catalog_list_categories 不传 parentId 返回根类目；传 parentId 返回其子类目
            
            回复格式（严格遵守）：
            - 推理过程（分析思路、调用计划、中间推导）放在 <think>...</think> 标签内
            - 正式结论（查询结果、操作结果、数据汇总）直接写，不加标签
            - 后续建议（可选）用"---"分隔后写在末尾
            - 示例：
              <think>用户想查手机类目下的子类目，我需要先找到手机的类目 ID，再查子类目。</think>
              手机类目下有 3 个子系列：
              - Mate 系列
              - Pura 系列
              - 折叠屏系列
              ---
              需要我进一步查看某个系列下的商品吗？
            
            规则：
            - **必须通过工具获取真实数据，严禁编造数据或猜测数字**
            - 如果工具返回空列表，如实告知用户，不要虚构内容
            - 执行破坏性操作（删除）前，向用户确认
            - 操作成功后简要说明结果
            - 不确定时请求用户提供更多信息
            - 用中文回复
            """.formatted(currentPage);
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
