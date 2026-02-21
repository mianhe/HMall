# Smart Interaction 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。Feature 目录：`services/smart-interaction-service/src/test/resources/features/smart-interaction/`。技术设计见 [architecture/ai-chat.md](../../architecture/ai-chat.md)。

### 状态图例

- ✅ 已实现（后端 + 测试均已完成）
- 🔲 待实现

---

## 一、背景与定位

Smart Interaction 提供智能交互能力，当前以 LLM + MCP Tool Calling 实现对话式系统操作。未来将扩展为更丰富的交互形式（内容卡片、操作面板等）。

| 维度 | 说明 |
|------|------|
| 核心职责 | 将自然语言意图翻译为工具调用，编排 LLM 与 MCP 的多轮交互 |
| 当前交互形式 | 对话（Chat） |
| 未来交互形式 | 内容卡片、操作面板、智能推荐等 |
| 领域模型 | 当前无聚合根，以领域服务为核心 |
| 数据库 | 当前无持久化，V2 计划引入对话历史存储 |

---

## 二、AI 流式对话
`ai-chat.feature`

- ✅ 1.1 纯文本对话应通过 SSE 流式返回（delta 事件逐 token 推送，done 事件结束）
- ✅ 1.2 单轮 Tool Call 应调用 MCP 工具并返回结果（LLM 返回 tool_calls → 执行 MCP → 回填结果 → LLM 生成最终回复）
- ✅ 1.3 多轮 Tool Call 应逐轮执行并最终返回结果（LLM 多次返回 tool_calls 时逐轮执行，直到产出纯文本）
- ✅ 1.4 查询可用模型列表时应返回已配置的模型（GET /api/ai/models）
- 🔲 1.5 LLM 返回空 id/name 的无效 tool call 时应过滤并正常完成对话，不死循环
- 🔲 1.6 MCP Server 不可用时对话应返回错误提示而非挂起
- 🔲 1.7 LLM API Key 缺失或无效时应返回明确错误提示

---

## 三、前端交互（frontend-admin，手工验收）

以下为前端 UI 需求，当前通过手工验收：

- ✅ 2.1 全局对话窗（Drawer）可通过右下角浮动按钮或 Ctrl+K / Cmd+K 快捷键呼出
- ✅ 2.2 SSE 流式接收，逐 token 更新 assistant 消息内容
- ✅ 2.3 Tool Call 可视化：展示工具名、参数、执行状态和结果（可折叠卡片）
- ✅ 2.4 Markdown 渲染：支持表格、列表等格式（marked + DOMPurify）
- ✅ 2.5 回复结构化展示：区分思考过程（可折叠）、正式结论、后续建议
- ✅ 2.6 流式中断：清空对话时中断正在进行的 SSE 流（AbortController）
- ✅ 2.7 模型切换：可选择不同 LLM 提供商

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 场景数 | 状态 |
|------|----------------|--------|------|
| 1. AI 流式对话 | ai-chat.feature | 1.1～1.4 | ✅ 已实现；1.5～1.7 🔲 待实现 |
| 2. 前端交互 | （手工验收） | 2.1～2.7 | ✅ 已实现 |

---

## 四、后续演进

- 对话历史持久化（数据库存储）
- MCP Server 动态注册/管理
- 用户级模型偏好设置
- 内容卡片式交互（可点击操作）
- 操作面板（批量操作向导）
