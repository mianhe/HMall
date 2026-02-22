# Smart Interaction — 技术架构

Smart Interaction 是 HMall 的智能交互限界上下文（BC），提供对话式操作能力。用户通过自然语言与系统交互，系统借助 LLM + MCP Tool Calling 执行业务操作。

---

## 一、定位

Smart Interaction 是**独立的限界上下文**，拥有自己的领域模型、数据库和服务进程。

| 判断维度 | 结论 |
|----------|------|
| 是否拥有领域模型 | 是 — Skill 聚合根、Conversation 聚合根 |
| 是否需要独立数据库 | 是 — 持久化 Skill、对话历史、系统设置 |
| 本质职责 | 智能编排层 — 将自然语言意图通过 Skill 上下文翻译为 MCP tool 调用 |

---

## 二、架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│  frontend-admin (Vue 3)                                         │
│                                                                 │
│  ┌───────────────────────────────────────────┐                  │
│  │  AI Chat (全局 Drawer)                     │                  │
│  │  ├── Skill 选择器                          │                  │
│  │  ├── 对话面板 (SSE 流式)                    │                  │
│  │  ├── Skill 管理 (CRUD)                     │                  │
│  │  └── 系统设置                               │                  │
│  └──────────────┬────────────────────────────┘                  │
└─────────────────┼───────────────────────────────────────────────┘
                  │ REST + SSE
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│  Smart Interaction Service (端口 8089)                           │
│                                                                 │
│  ┌─── API 层 ─────────────────────────────────────────────┐    │
│  │  对话 API (SSE)    Skill API (CRUD)    Settings API     │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                 │
│  ┌─── 领域层 ─────────────────────────────────────────────┐    │
│  │  Skill (聚合根)         ChatOrchestrator (编排)         │    │
│  │  Conversation (聚合根)  ToolFilter (工具过滤)           │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                 │
│  ┌─── 基础设施层 ─────────────────────────────────────────┐    │
│  │  LlmClient        McpToolBridge       JPA Repository    │    │
│  │  (→ LLM API)      (→ MCP Server)      (→ PostgreSQL)    │    │
│  └───────┬──────────────────┬─────────────────┬───────────┘    │
└──────────┼──────────────────┼─────────────────┼────────────────┘
           │                  │                 │
           ▼                  ▼                 ▼
      LLM API            hmall-mcp          PostgreSQL
    (千问/DeepSeek)       (:3000)
                      ┌────────────┐
                      │ tools/     │
                      │  catalog   │
                      │  inventory │  ← 按需扩展
                      │  order     │
                      │  ...       │
                      └─────┬──────┘
                            │ REST
                            ▼
                       各 BC 服务 API
```

### 关键决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 独立 BC | 是 | 拥有 Skill、Conversation 等领域模型，需要独立持久化 |
| LLM 调用位置 | 后端 | API Key 不暴露；Tool Call 闭环执行，无需前端中转 |
| 流式协议 | SSE | 单向推送、浏览器原生支持、Spring WebFlux 天然适配 |
| MCP 通信 | HTTP (Streamable HTTP) | hmall-mcp 已有 HTTP 端点 `/mcp` |
| LLM API 格式 | OpenAI Chat Completions 兼容 | 千问、DeepSeek 等均兼容，一套代码多模型 |
| MCP Server | 单一进程，按模块组织 tools | 物理简单；通过 Skill 的 allowedTools 在 Client 端做逻辑过滤 |
| Skill 存储位置 | Smart Interaction 数据库 | Client 端管理，灵活可运营，不依赖 MCP Server |

---

## 三、核心概念

### 3.1 Skill

Skill 是 Smart Interaction 的核心领域概念，定义了 AI 助手在特定场景下的行为：

- **systemPrompt**：注入给 LLM 的系统提示词，包含角色定义、领域知识、行为规则
- **allowedTools**：限定 LLM 可调用的工具范围（支持通配符，如 `inventory_*`）
- **用户可运营**：管理员通过前端 UI 随时创建、编辑、删除 Skill，无需改代码或重启

Skill 的设计理念：**MCP Server 提供能力（Tools），Skill 编排能力的使用方式**。

### 3.2 Tool 过滤机制

MCP Server 暴露全量 tools，Smart Interaction 根据当前 Skill 的 `allowedTools` 过滤后再传给 LLM：

```
MCP Server (tools/list)
  → 全量 tools [catalog_*, inventory_*, order_*, ...]
      │
      │  ToolFilter（按 Skill.allowedTools 过滤）
      ▼
  → 过滤后 tools [inventory_*, catalog_list_skus]
      │
      │  传给 LLM
      ▼
  LLM 只看到与当前 Skill 相关的 tools
```

好处：减少 token 消耗、提高工具选择准确率、避免越权操作。

### 3.3 对话编排流程

```
用户选择 Skill → 发送消息
    │
    ▼
ChatOrchestrator
  1. 加载 Skill（无则使用默认 Skill）
  2. 构建 system message = 基础规则 + Skill.systemPrompt
  3. 从 MCP 获取全量 tools → 按 Skill.allowedTools 过滤
  4. 发送给 LLM（streaming）
  5. LLM 返回文本 → 推 SSE delta
  6. LLM 返回 tool_calls → 执行 → 结果回填 → 再次调 LLM（循环）
  7. 最终回复 → 推 SSE done
  8. 持久化对话记录
```

### 3.4 持久化数据

| 数据 | 说明 |
|------|------|
| **Skill** | 可运营的 AI 助手配置 |
| **Conversation** | 对话历史，含多轮消息和 tool call 记录 |
| **Settings** | 系统级配置（如默认模型、tool call 轮次限制等） |

---

## 四、MCP Server 架构

### 4.1 单一 MCP Server，按模块组织

```
hmall-mcp/
├── index.js              # stdio 入口（供 Cursor 等 Client）
├── index-http.js         # HTTP 入口（供 Smart Interaction）
├── tools/
│   ├── catalog.js        # 类目、商品、SKU、图片 CRUD
│   ├── inventory.js      # 库存查询与管理（待扩展）
│   ├── order.js          # 订单查询（待扩展）
│   ├── fulfillment.js    # 履约管理（待扩展）
│   └── activity.js       # 监控与统计（待扩展）
└── package.json
```

### 4.2 MCP Server 的职责边界

MCP Server **只做**：
- 暴露 Tools（包含 name、description、inputSchema）
- 接收 tool call 请求，转发到对应 BC 的 REST API
- 返回执行结果

MCP Server **不做**：
- 不知道 Skill 的存在
- 不做工具过滤（这是 Client 的事）
- 不做权限控制（将来如有需要，在 Smart Interaction 层做）

### 4.3 Tool 命名约定

```
{module}_{action}_{target}
```

示例：`catalog_list_products`、`inventory_query_stock`、`order_list_orders`

模块前缀使 Skill 的通配符过滤（`inventory_*`）自然可行。

---

## 五、前端设计

### 5.1 AI Chat Drawer 结构

```
AI Chat Drawer (全局，右侧滑出)
├── 顶部
│   ├── Skill 选择器（下拉 / 标签切换）
│   ├── 新建对话
│   └── 设置入口
├── 中部 — 对话面板
│   ├── 消息列表（流式渲染）
│   ├── Tool Call 可视化卡片
│   └── Markdown 渲染
├── 底部 — 输入区
│   └── 输入框 + 发送按钮
└── 子面板（覆盖式切换）
    ├── Skill 管理（列表 / 创建 / 编辑）
    ├── 对话历史列表
    └── 系统设置
```

### 5.2 组件结构

```
src/shared/ui/ai-chat/
├── AiChatPanel.vue           # Drawer 主面板
├── AiChatButton.vue          # 浮动触发按钮
├── AiMessageList.vue         # 消息列表
├── AiMessageBubble.vue       # 消息气泡
├── AiToolCallCard.vue        # Tool Call 展示卡片
├── AiChatInput.vue           # 输入框
├── AiSkillSelector.vue       # Skill 选择器（新增）
├── AiSkillManager.vue        # Skill CRUD 面板（新增）
├── AiConversationList.vue    # 对话历史列表（新增）
└── AiSettingsPanel.vue       # 系统设置面板（新增）

src/shared/composables/
├── useAiChat.js              # 对话逻辑
└── useAiSkills.js            # Skill 管理逻辑（新增）

src/shared/api/
└── ai.js                     # API 封装
```

---

## 六、API 契约概览

### 对话

```
POST /api/ai/chat              → SSE 流式对话（请求含 skillId）
```

### Skill 管理

```
GET    /api/ai/skills          → Skill 列表
POST   /api/ai/skills          → 创建 Skill
GET    /api/ai/skills/{id}     → Skill 详情
PUT    /api/ai/skills/{id}     → 更新 Skill
DELETE /api/ai/skills/{id}     → 删除 Skill
```

### 对话历史

```
GET    /api/ai/conversations              → 对话列表
GET    /api/ai/conversations/{id}         → 对话详情（含消息）
DELETE /api/ai/conversations/{id}         → 删除对话
```

### 系统设置

```
GET    /api/ai/settings        → 获取设置
PUT    /api/ai/settings        → 更新设置
```

### 模型

```
GET    /api/ai/models          → 可用模型列表
```

---

## 七、技术选型

| 层面 | 选型 | 说明 |
|------|------|------|
| 服务框架 | Spring Boot + WebFlux | 流式 SSE 响应 |
| LLM 调用 | Spring WebClient (reactive streaming) | OpenAI 兼容格式 |
| 流式推送 | SSE (Server-Sent Events) | 浏览器原生支持 |
| MCP 通信 | HTTP JSON-RPC (Streamable HTTP) | 端口 3000 |
| 持久化 | JPA + PostgreSQL | Skill、Conversation、Settings |
| 前端 | Vue 3 + Tailwind CSS | 组件化，与 frontend-admin 一致 |
| Markdown | marked + DOMPurify | 轻量渲染 |
| MCP Server | Node.js + @modelcontextprotocol/sdk | 多模块 tools |

---

## 八、扩展性

### 新增 MCP 工具模块

在 `hmall-mcp/tools/` 下新增文件并注册。Smart Interaction 通过 `tools/list` 自动发现，无需改动 Smart Interaction 代码。新模块的 tools 可立即被已有 Skill 或新建 Skill 引用。

### Skill 的灵活性

- 管理员随时创建新 Skill，组合不同模块的 tools
- Skill 的 systemPrompt 可包含任意领域知识（数据模型说明、业务规则、操作指引）
- 通配符机制使 Skill 配置简洁且自动适应新增的 tools

### 前端接入扩展

当前仅 frontend-admin 接入。将来 frontend-web（消费者端）可接入同一服务，使用面向消费者的 Skill（如"购物助手"）。
