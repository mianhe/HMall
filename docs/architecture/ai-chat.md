# HMall 智能对话交互系统 — 技术设计

在 HMall 中引入对话式交互能力，用户可在后台管理页面通过自然语言操作系统。对话通过 MCP 协议调用已有的工具（当前 Catalog），实现「聊天即操作」。

---

## 一、定位

AI Chat **不是独立的限界上下文**，而是 **BFF 内部的功能模块**（`com.hmall.bff.ai.*`）。

| 判断维度 | 结论 |
|----------|------|
| 是否拥有领域模型（聚合根、实体、不变式） | 否 — 不管理业务实体 |
| 是否需要独立数据库 | 否 — V1 无持久化 |
| 是否产生领域事件 | 否 |
| 本质职责 | 编排层 — 将自然语言意图翻译为 MCP tool 调用 |

BFF 已有的职责是「为前端代理和编排后端服务调用」，AI Chat 是这一职责的自然延伸：从「路由 HTTP 请求到下游服务」变为「路由自然语言意图到 MCP tools」。因此不需要按 DDD 四层架构组织，也不需要创建独立微服务。

---

### 目标

1. **基础能力**：对话窗可在任意后台页面呼出，作为全局通用能力
2. **自然语言操作**：通过 LLM + MCP Tool Calling，用对话完成类目、商品、SKU 等管理操作
3. **可扩展**：当更多 BC 开放 MCP tools 时，对话能力自动扩展

### V1 边界（当前阶段）

| 做 | 不做 |
|----|------|
| Catalog MCP tools 全量对接 | 其他 BC 的 MCP tools（后续增量） |
| 流式对话（SSE） | WebSocket 双向通信 |
| 前端内存管理对话历史 | 对话持久化（数据库存储） |
| 多模型配置（千问、DeepSeek 等） | 用户级模型偏好设置 |
| 后台管理端（frontend-admin） | 用户端（frontend-web） |

---

## 二、架构总览

```
frontend-admin (Vue 3)
│
│  AiChatPanel (全局 Drawer)
│       │
│       │ POST /api/ai/chat (SSE streaming)
│       ▼
bff-web (Spring WebFlux)
│
│  AiChatController → AiChatService
│       │                    │
│       │                    ├─→ LlmClient (调 LLM API，streaming)
│       │                    │      → 千问 / DeepSeek / OpenAI ...
│       │                    │
│       │                    └─→ McpToolBridge (MCP Client)
│       │                           → hmall-mcp (HTTP, :3000/mcp)
│       │                                → catalog-service REST API
│       ▼
│  SSE 流式响应 → 前端逐 token 渲染
```

### 关键决策

| 决策 | 选择 | 理由 |
|------|------|------|
| LLM 调用位置 | BFF 后端 | API Key 不暴露；Tool Call 闭环执行，无需前端中转 |
| 流式协议 | SSE (Server-Sent Events) | 单向推送、浏览器原生支持、Spring WebFlux 天然适配 |
| MCP 通信方式 | HTTP (Streamable HTTP) | hmall-mcp 已有 HTTP 模式 (`/mcp`)，无需额外协议 |
| LLM API 格式 | OpenAI Chat Completions 兼容 | 千问、DeepSeek、OpenAI 均兼容此格式，一套代码多模型 |

---

## 三、前端设计

### 3.1 全局对话窗

对话以 Drawer 形式存在，从右侧滑出，覆盖在当前页面之上。

**触发方式**：
- 右下角浮动按钮点击
- 快捷键 `Ctrl+K` / `Cmd+K`

**在 App.vue 中全局挂载**，所有路由页面共享同一个对话实例。

### 3.2 组件结构

```
src/shared/ui/ai-chat/
├── AiChatPanel.vue          # Drawer 主面板
├── AiChatButton.vue          # 右下角浮动按钮
├── AiMessageList.vue         # 消息列表（滚动容器）
├── AiMessageBubble.vue       # 单条消息（区分 user/assistant/tool）
├── AiToolCallCard.vue        # Tool Call 可折叠展示卡片
└── AiChatInput.vue           # 输入框 + 发送按钮

src/shared/composables/
└── useAiChat.js              # 核心逻辑：SSE 连接、消息管理、状态

src/shared/api/
└── ai.js                     # AI API 封装
```

### 3.3 消息模型

```javascript
// 消息类型
{
  id: string,            // 唯一 ID
  role: 'user' | 'assistant',
  content: string,       // 文本内容（assistant 可能包含 markdown）
  toolCalls: [           // assistant 消息可能附带的 tool calls
    {
      name: string,      // 工具名，如 "catalog_list_categories"
      arguments: object,
      result: string,    // 执行结果
      status: 'calling' | 'success' | 'error'
    }
  ],
  loading: boolean       // 是否正在流式接收中
}
```

### 3.4 SSE 流式接收

前端通过 `fetch` + `ReadableStream` 解析 SSE 事件，逐 token 更新 assistant 消息：

```javascript
const response = await fetch('/api/ai/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ messages, context: { page: route.path } })
})

const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
// 逐行解析 SSE：event: xxx / data: {...}
// 根据事件类型更新消息内容、tool calls 状态等
```

### 3.5 操作联动

当 tool call 成功修改数据后（如创建类目），前端通过事件通知当前页面刷新数据。`useAiChat` composable 暴露 `onToolCallSuccess` 回调，页面按需监听。

### 3.6 Markdown 渲染

LLM 回复可能包含表格、列表等 markdown 格式，需引入轻量渲染库（如 `marked` + `DOMPurify`）。

---

## 四、BFF 后端设计

### 4.1 模块结构

```
services/bff-web/src/main/java/com/hmall/bff/
└── ai/
    ├── AiChatController.java        # POST /api/ai/chat (SSE)
    │                                 # GET  /api/ai/models (可用模型列表)
    ├── AiChatService.java           # 核心编排：system prompt + LLM + tool call 循环
    ├── LlmClient.java               # 调 LLM API（WebClient streaming）
    ├── LlmProviderConfig.java       # 多模型配置绑定
    ├── McpToolBridge.java           # MCP ↔ OpenAI function calling 桥接
    └── dto/
        ├── ChatRequest.java          # 前端请求体
        └── ChatEvent.java           # SSE 事件体
```

### 4.2 核心流程

```
用户发送消息
    │
    ▼
AiChatController 接收 ChatRequest
    │
    ▼
AiChatService.chat()
    │
    ├─ 1. 获取可用 tools（McpToolBridge 缓存的 MCP tools → OpenAI function 格式）
    │
    ├─ 2. 组装 system prompt（注入页面上下文）
    │
    ├─ 3. 调 LLM (streaming)
    │     │
    │     ├─ 收到文本 token → 推 SSE "delta" 事件
    │     │
    │     └─ 收到 tool_calls → 推 SSE "tool_call" 事件
    │           │
    │           ├─ McpToolBridge.executeTool() → hmall-mcp → catalog-service
    │           │
    │           ├─ 推 SSE "tool_result" 事件
    │           │
    │           └─ 将 tool result 回填 messages，再次调 LLM（循环）
    │
    └─ 4. LLM 生成最终回复 → 推 SSE "done" 事件
```

### 4.3 MCP Tool Bridge

启动时（或首次请求时）连接 hmall-mcp，获取 tools 列表并缓存：

```
BFF 启动
    │
    ▼
POST http://127.0.0.1:3000/mcp
  { jsonrpc: "2.0", method: "initialize", params: {...} }
    │
    ▼
POST http://127.0.0.1:3000/mcp  (带 mcp-session-id)
  { jsonrpc: "2.0", method: "tools/list" }
    │
    ▼
返回 tools[]: { name, description, inputSchema }
    │
    ▼
转换为 OpenAI function calling 格式并缓存：
  { type: "function", function: { name, description, parameters: inputSchema } }
```

执行 tool call 时：

```
LLM 返回 tool_call: { name: "catalog_create_category", arguments: { name: "手机" } }
    │
    ▼
POST http://127.0.0.1:3000/mcp  (带 mcp-session-id)
  { jsonrpc: "2.0", method: "tools/call", params: { name: "...", arguments: {...} } }
    │
    ▼
hmall-mcp 调 catalog-service REST API → 返回结果
    │
    ▼
回填到 messages 作为 tool role 消息，继续 LLM 对话
```

### 4.4 LLM Client

使用 Spring WebClient 调用 OpenAI 兼容 API，以流式 (streaming) 方式接收响应：

```
POST {base-url}/chat/completions
Headers: Authorization: Bearer {api-key}
Body: {
  model: "qwen-plus",
  messages: [...],
  tools: [...],
  stream: true
}
```

响应为 SSE 流（`data: {...}` 逐行），WebClient 按行解析，逐 chunk 处理。

---

## 五、API 契约

### POST /api/ai/chat

**请求**：

```json
{
  "messages": [
    { "role": "user", "content": "帮我查看所有类目" }
  ],
  "context": {
    "page": "/catalog"
  },
  "provider": "qwen"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| messages | array | 对话历史（role: user/assistant） |
| context.page | string | 当前页面路由，用于 system prompt 上下文 |
| provider | string | 可选，模型提供商 ID，不传用默认 |

**响应**：`text/event-stream` (SSE)

```
event: delta
data: {"type":"delta","content":"正在查询"}

event: tool_call
data: {"type":"tool_call","id":"tc_1","name":"catalog_list_categories","arguments":{}}

event: tool_result
data: {"type":"tool_result","id":"tc_1","name":"catalog_list_categories","result":"| ID | 名称 |..."}

event: delta
data: {"type":"delta","content":"找到以下类目：..."}

event: done
data: {"type":"done"}

event: error
data: {"type":"error","message":"模型调用失败"}
```

### GET /api/ai/models

**响应**：

```json
{
  "models": [
    { "id": "qwen", "name": "通义千问", "default": true },
    { "id": "deepseek", "name": "DeepSeek", "default": false }
  ]
}
```

---

## 六、配置

```yaml
# bff-web application.yml
hmall:
  ai:
    default-provider: qwen
    providers:
      qwen:
        api-key: ${QWEN_API_KEY}
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
        model: qwen-plus
      deepseek:
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com/v1
        model: deepseek-chat
    mcp:
      url: http://127.0.0.1:3000/mcp
```

通过环境变量注入 API Key，不在配置文件中硬编码。

---

## 七、配置管理与 MCP 注册的归属

### 问题

两个能力需要明确归属：

1. **模型配置管理** — 管理 LLM 提供商的 API Key、base-url、默认模型的切换
2. **MCP Server 注册/解绑** — 管理连接哪些 MCP Server、查看可用 tools、启用/禁用

它们都涉及**运行时可变的配置状态**，但本质上都是为 AI Chat 编排层服务的基础设施。

### V1 方案：BFF 内部 + YAML 配置

| 能力 | V1 做法 | 管理方式 |
|------|---------|----------|
| 模型配置 | `application.yml` + 环境变量 | 修改配置 + 重启 |
| MCP Server 地址 | `application.yml` 中 `hmall.ai.mcp.url` | 修改配置 + 重启 |
| MCP Tools 发现 | BFF 启动时自动通过 `tools/list` 获取并缓存 | 自动 |

V1 不引入数据库管理这些配置。理由：当前只有一个 MCP Server、少数几个模型提供商，运行时动态增删需求不强烈。

### V2 演进：Settings 页面 + 持久化

当以下情况出现时，升级为数据库持久化 + 管理界面：

- 需要在 Settings 页面动态添加/切换模型提供商
- 需要注册多个 MCP Server 并按模块启用/禁用 tools
- 需要按用户/角色配置不同的模型偏好

届时可在 BFF 中新增：
- `AiSettingsController` — Settings 页面的 CRUD API
- 持久化层（BFF 自有的轻量数据库，或复用已有 PostgreSQL）
- 前端 Settings 页面新增"AI 设置"板块

这些仍然是 BFF 内部模块，不需要升级为独立 BC。因为它们服务的对象始终是 AI Chat 编排能力本身，不存在独立的业务领域。

---

## 八、System Prompt

```
你是 HMall 智能助手，帮助管理员通过对话管理电商系统。

当前上下文：
- 用户正在浏览：{currentPage}

你的能力：
- 通过工具操作商品目录（类目、商品、规格、SKU、图片）
- 回答关于系统操作的问题
- 引导用户完成复杂的多步操作

规则：
- 执行破坏性操作（删除）前，向用户确认
- 操作成功后简要说明结果
- 不确定时请求用户提供更多信息
- 用中文回复
```

---

## 九、扩展性

### 更多 MCP 模块

当 Inventory、Order 等 BC 开放 MCP tools 时，在 hmall-mcp 中新增 `tools/inventory.js`、`tools/order.js` 等模块并注册即可。BFF 通过 `tools/list` 自动发现所有 tools，无需改动 BFF 代码。

### 页面上下文感知

不同页面传递不同 `context.page`，system prompt 可据此调整引导方向：
- `/catalog` → 侧重商品管理
- `/inventory` → 侧重库存查询
- `/` → 通用助手

### 操作联动

Tool call 成功后，前端可根据操作类型触发对应页面数据刷新（如创建类目后刷新 CatalogTree）。

---

## 十、技术选型汇总

| 层面 | 选型 | 说明 |
|------|------|------|
| LLM 模型 | 通义千问 qwen-plus (V1 默认) | 支持 function calling，OpenAI 兼容 API |
| LLM 调用 | Spring WebClient (reactive streaming) | BFF 已用 WebFlux，天然支持 |
| 流式推送 | SSE (Server-Sent Events) | 浏览器原生支持，实现简单 |
| MCP 通信 | HTTP JSON-RPC (Streamable HTTP) | hmall-mcp 已有 HTTP 端点 |
| 前端渲染 | marked + DOMPurify | 轻量 markdown 渲染 |
| 对话状态 | Vue composable 内存管理 | V1 不持久化，刷新清空 |

---

## 十一、实施阶段

| 阶段 | 内容 | 依赖 |
|------|------|------|
| Phase 1 | BFF: LlmClient + AiChatController（streaming SSE，不含 tool calling） | 千问 API Key |
| Phase 2 | BFF: McpToolBridge（连接 hmall-mcp，tool call 执行与回填循环） | hmall-mcp 运行中 |
| Phase 3 | 前端: AiChatPanel 全局组件 + SSE 接收 + 消息渲染 | Phase 1 |
| Phase 4 | 联调 + Tool Call 可视化 + markdown 渲染 + 操作联动 | Phase 1-3 |
