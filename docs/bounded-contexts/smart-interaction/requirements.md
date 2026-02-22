# Smart Interaction 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。Feature 目录：`services/smart-interaction-service/src/test/resources/features/smart-interaction/`。技术架构见 [architecture.md](./architecture.md)，领域模型见 [domain-model.md](./domain-model.md)。

### 状态图例

- ✅ 已实现
- 🔲 待实现

---

## 一、职责说明

Smart Interaction 提供智能交互能力：管理员通过自然语言与系统对话，借助 Skill 定义的上下文和工具范围，由 LLM + MCP Tool Calling 编排执行业务操作。

**核心设计决策**：

| # | 决策 | 说明 |
|---|------|------|
| S1 | Smart Interaction 是独立 BC | 拥有 Skill、Conversation、Settings 领域模型，独立持久化 |
| S2 | Skill 存储在 Client 端（Smart Interaction），不在 MCP Server | 灵活可运营，管理员可随时 CRUD，无需改代码或重启 MCP Server |
| S3 | Tool 过滤在 Client 端执行 | MCP Server 暴露全量 tools，Smart Interaction 按 Skill.allowedTools 过滤后传给 LLM |
| S4 | 单一 MCP Server，按模块组织 tools | 物理简单，逻辑隔离由 Skill 的 allowedTools 完成 |
| S5 | 通配符匹配 allowedTools | `inventory_*` 匹配所有 `inventory_` 前缀的工具；`*` 或空列表表示不过滤 |
| S6 | Settings 单例 | 系统级配置全局一条记录，暂不按用户区分 |
| S7 | 对话历史持久化 | Conversation + Message 存数据库，支持历史回顾 |

---

## 1. Skill 管理

`skill-management.feature`

- ✅ 1.1 创建 Skill 时传入名称、描述、systemPrompt、allowedTools，应创建成功并返回 Skill 详情
- ✅ 1.2 创建 Skill 时名称为空应返回 400 错误
- ✅ 1.3 查询 Skill 列表应返回所有 Skill（按 createdAt 降序）
- ✅ 1.4 按 ID 查询 Skill 详情应返回完整信息（含 allowedTools）
- ✅ 1.5 更新 Skill 时应修改对应字段并更新 updatedAt
- ✅ 1.6 删除 Skill 应成功；删除不存在的 Skill 应返回 404
- ✅ 1.7 将某 Skill 设为默认时，应自动取消原默认 Skill 的 isDefault 标记
- ✅ 1.8 删除默认 Skill 后，系统应无默认 Skill（不自动指定替代）

---

## 2. AI 流式对话

`ai-chat.feature`

- ✅ 2.1 纯文本对话应通过 SSE 流式返回（delta 事件逐 token 推送，done 事件结束）
- ✅ 2.2 单轮 Tool Call 应调用 MCP 工具并返回结果（LLM 返回 tool_calls → 执行 MCP → 回填结果 → LLM 生成最终回复）
- ✅ 2.3 多轮 Tool Call 应逐轮执行并最终返回结果（LLM 多次返回 tool_calls 时逐轮执行，直到产出纯文本）
- ✅ 2.4 查询可用模型列表时应返回已配置的模型（GET /api/ai/models）
- ✅ 2.5 指定 skillId 时，对话应加载该 Skill 的 systemPrompt 并按 allowedTools 过滤工具
- ✅ 2.6 未指定 skillId 时，应使用默认 Skill；无默认 Skill 时使用基础 system prompt + 全量工具
- ✅ 2.7 Tool Call 轮次超过限制时应终止循环并返回错误提示
- 🔲 2.8 LLM 返回空 id/name 的无效 tool call 时应过滤并正常完成对话，不死循环
- 🔲 2.9 MCP Server 不可用时对话应返回错误提示而非挂起
- 🔲 2.10 LLM API Key 缺失或无效时应返回明确错误提示

---

## 3. 对话历史

`conversation-history.feature`

- 🔲 3.1 对话完成后应自动持久化 Conversation 及其 Messages
- 🔲 3.2 查询对话列表应返回所有 Conversation（按 updatedAt 降序），包含 title、skillId、provider、时间
- 🔲 3.3 按 ID 查询对话详情应返回 Conversation 及其所有 Messages（含 toolCalls）
- 🔲 3.4 删除对话应级联删除其所有 Messages
- 🔲 3.5 删除不存在的对话应返回 404

---

## 4. 系统设置

`settings.feature`

- 🔲 4.1 获取系统设置应返回当前配置（defaultProvider、maxToolCallRounds）
- 🔲 4.2 首次获取时若无记录应返回默认值（使用 application.yml 中的配置）
- 🔲 4.3 更新系统设置应修改对应字段并更新 updatedAt
- 🔲 4.4 更新时 maxToolCallRounds <= 0 应返回 400 错误

---

## 5. MCP 工具扩展

`mcp-tools.feature`

- 🔲 5.1 Inventory 模块工具应可通过 MCP 调用（查询库存、库存列表）
- 🔲 5.2 Order 模块工具应可通过 MCP 调用（查询订单、订单列表）
- 🔲 5.3 Fulfillment 模块工具应可通过 MCP 调用（查询履约单、触发配货/发货/签收）
- 🔲 5.4 Activity 模块工具应可通过 MCP 调用（查询统计、查询事件列表）

---

## 6. 前端交互（frontend-admin，手工验收）

以下为前端 UI 需求，通过手工验收：

### 已实现

- ✅ 6.1 全局对话窗（Drawer）可通过右下角浮动按钮或 Ctrl+K / Cmd+K 快捷键呼出
- ✅ 6.2 SSE 流式接收，逐 token 更新 assistant 消息内容
- ✅ 6.3 Tool Call 可视化：展示工具名、参数、执行状态和结果（可折叠卡片）
- ✅ 6.4 Markdown 渲染：支持表格、列表等格式
- ✅ 6.5 回复结构化展示：区分思考过程（可折叠）、正式结论、后续建议
- ✅ 6.6 流式中断：清空对话时中断正在进行的 SSE 流
- ✅ 6.7 模型切换：可选择不同 LLM 提供商

### 待实现

- ✅ 6.8 Skill 选择器：对话顶部可选择/切换 Skill，下拉显示所有 Skill 及默认标记
- ✅ 6.9 Skill 管理面板：在 Drawer 内进行 Skill 的创建、编辑、删除、设为默认
- 🔲 6.10 对话历史列表：在 Drawer 内查看历史对话，点击可恢复
- 🔲 6.11 系统设置面板：在 Drawer 内配置默认模型、tool call 轮次等

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 场景数 | 状态 |
|------|----------------|--------|------|
| 1. Skill 管理 | skill-management.feature | 1.1～1.8 | ✅ 已实现 |
| 2. AI 流式对话 | ai-chat.feature | 2.1～2.7 ✅；2.8～2.10 🔲 | 部分完成 |
| 3. 对话历史 | conversation-history.feature | 3.1～3.5 | 🔲 待实现 |
| 4. 系统设置 | settings.feature | 4.1～4.4 | 🔲 待实现 |
| 5. MCP 工具扩展 | mcp-tools.feature | 5.1～5.4 | 🔲 待实现 |
| 6. 前端交互 | （手工验收） | 6.1～6.9 ✅；6.10～6.11 🔲 | 部分完成 |
