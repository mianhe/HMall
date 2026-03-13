# 智能运营 Step 2：Smart Interaction 接入 + 对话驱动页面 MVP

> 智能运营总体架构见 [business-process-architecture.md](../../business-process-architecture.md)。  
> 前置需求：[智能运营 Step 1（多维事件基座）](../intelligent-ops-step1/overview.md) ✅ 已完成。

---

## 一、需求概述与场景

### 需求概述

**业务目标**：在 Step 1 的多维事件查询能力基础上，接入 Smart Interaction，实现「对话驱动页面」——运营者通过自然语言与系统对话，AI 不只在聊天框里回复文字，同时在主页面动态渲染对应的数据视图。自然语言成为统一可见出口，让运营结果直观可感知。

**需求类型**：扩展已有能力。在现有 Smart Interaction 和 Activity BC 之上叠加新的交互模式，不引入新的领域概念。

**与当前能力的核心区别**：

| 维度 | 当前 | Step 2 之后 |
|------|------|------------|
| AI 入口 | 全局浮层 Drawer，所有页面通用 | 新增「智能运营」专属页面，常驻侧边栏 AI 面板 |
| AI 输出 | 仅在对话框输出文字 | 对话同时驱动主画布渲染数据组件（对话-页面联动） |
| Activity MCP 工具 | `activity_query`（仅支持按 orderId 查询） | 扩展支持 userId/skuId/spuId 多维查询；新增 stats 独立工具和 recent 独立工具 |
| 流程知识注入 | 无 Activity/智能运营专属 Skill | 新增「智能运营助手」Skill + `intelligent-ops-domain` MCP Resource，AI 回答带流程语义 |
| 运营页面 | 无智能运营专属页面 | 新增 `/ops` 页面：固定指标栏 + 动态画布 + 常驻 AI 侧边栏 |

**后端影响面**：

| BC/模块 | 影响程度 | 变更内容 |
|---------|---------|---------|
| MCP（hmall-mcp） | 🟡 中等 | 扩展 `activity_query` 支持多维参数；新增 `intelligent-ops-domain` Resource |
| Smart Interaction | 🟢 轻微 | `useAiChat.js` 补充反注册支持；新增「智能运营助手」Skill（运营配置） |
| Activity | ⚪ 无变更 | API 已在 Step 1 完成，本 Step 直接复用 |
| Order / 其他 BC | ⚪ 无变更 | — |

**前端影响面**：

| 前端 | 变更内容 |
|------|---------|
| frontend/admin | 新增 `OpsPage.vue`（`/ops` 路由）；新增 `OpsAiSidebar.vue`；新增 3 个画布组件；`useAiChat.js` 微调；导航入口新增「智能运营」链接 |
| frontend/web | 无变更 |

---

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| S1 | 对话驱动画布渲染 | 主流程 | L2 中分析 | 用户在对话框输入自然语言 → AI 调用 MCP Tool → 前端画布同步渲染对应数据组件 |
| S2 | 智能运营页面 | 主流程 | L2 中分析 | 运营者进入 `/ops` 页面，固定指标栏 + 动态画布 + 常驻 AI 侧边栏三区域协作 |
| S3 | Activity MCP 工具扩展 | 支撑流程 | L1 轻分析 | `activity_query` 增加 userId/skuId/spuId 参数；新增独立统计和近期事件工具 |
| S4 | 智能运营助手 Skill | 支撑流程 | L1 轻分析 | 新增 Skill + MCP Resource，AI 用流程语义解读事件 |
| S5 | 全局 AI 面板隔离 | 支撑流程 | L1 轻分析 | `/ops` 页面隐藏全局 AiChatButton，`onToolCallSuccess` 支持反注册避免跨页面副作用 |

---

## 二、场景分析

### S1：对话驱动画布渲染（L2）

#### 核心交互流程

```
运营者输入 "给我看用户 42 的事件序列"
    ↓
useAiChat.sendMessage() → POST /api/ai/chat（SSE）
    ↓
Smart Interaction 后端：LLM 决定调用 activity_query { action: "list", userId: 42 }
    ↓
SSE event: tool_call { name: "activity_query", arguments: {action: "list", userId: 42} }
    ↓
MCP Tool 执行，查 Activity API: GET /api/activities?userId=42
    ↓
SSE event: tool_result { name: "activity_query", result: [...] }
    ↓
前端 onToolCallSuccess("activity_query", result) 回调触发
    ↓
OpsPage 中注册的处理器：canvasStore.handleToolResult("activity_query", result)
    ↓
canvasStore.state = { type: "TIMELINE", data: result, params: {userId: 42} }
    ↓
动态画布渲染 OpsActivityTimeline 组件
    ↓
SSE event: delta "好的，以下是用户 42 的事件序列..." （文字解读同时流出）
    ↓
AI 侧边栏同步显示文字回复
```

#### 画布状态机

| 状态 | 触发条件 | 渲染组件 |
|------|---------|---------|
| `EMPTY` | 初始 / 清空对话 | 欢迎引导，提示可问的问题类型 |
| `LOADING` | tool_call 事件收到、tool_result 未收到 | 骨架屏（对应上一次画布状态的占位） |
| `STATS` | tool_result of `activity_stats` | `OpsStatsPanel`（多指标卡片） |
| `TIMELINE` | tool_result of `activity_query`（含 userId/skuId/spuId/orderId） | `OpsActivityTimeline`（事件时间线） |
| `RECENT` | tool_result of `activity_recent` | `OpsRecentList`（近期事件列表） |

> **决策 IO2-S1-1**：工具结果到画布状态的映射在前端维护（`useOpsCanvas.js` 的 `TOOL_TO_CANVAS_MAP`），而非由后端或 AI 控制。理由：前端完全掌握渲染能力，映射规则是视图逻辑而非业务逻辑，且这样 AI 端不需要知道前端组件存在，解耦更彻底。

> **决策 IO2-S1-2**：`LOADING` 状态保留上一次画布内容加遮罩，而非清空画布。理由：避免内容闪烁，给用户连续感。

#### tool_result 数据格式约定

MCP Tool 目前返回格式化文字字符串（`{ content: [{ type: "text", text: "..." }] }`），前端 `onToolCallSuccess` 收到的是原始 result 字符串，**无法直接用于图表渲染**。

> **决策 IO2-S1-3**：扩展 Activity MCP Tool，让特定 action 在返回给 LLM 的文字结果之外，**同时在 result 的 JSON 结构中附带原始数据**（`_raw` 字段）。前端 `onToolCallSuccess` 从 `result._raw` 取结构化数据渲染组件；若无 `_raw` 则不触发画布更新。

这样对 LLM 的文字格式无影响，只是额外携带了结构化数据。

具体结构：
```json
{
  "content": [{ "type": "text", "text": "最近活动（5 条）：..." }],
  "_raw": {
    "type": "activity_list",
    "items": [ { "id": 1, "eventType": "OrderCreated", "orderId": 123, "userId": 42, "occurredAt": "..." }, ... ]
  }
}
```

对于 stats 类型：
```json
{
  "content": [{ "type": "text", "text": "统计区间：..." }],
  "_raw": {
    "type": "activity_stats",
    "data": { "ordersCreated": 142, "paymentSuccess": 138, ... }
  }
}
```

#### `onToolCallSuccess` 反注册问题

`useAiChat.js` 目前只有注册没有反注册，导致 OpsPage 离开后回调仍然存在。需微调：

```javascript
// 修改前：注册无返回值
function onToolCallSuccess(callback) {
  toolCallCallbacks.push(callback)
}

// 修改后：返回反注册函数
function onToolCallSuccess(callback) {
  toolCallCallbacks.push(callback)
  return () => {
    const idx = toolCallCallbacks.indexOf(callback)
    if (idx > -1) toolCallCallbacks.splice(idx, 1)
  }
}
```

OpsPage 中：
```javascript
const unregister = chat.onToolCallSuccess(handler)
onUnmounted(() => unregister())
```

---

### S2：智能运营页面（L2）

#### 页面布局（`/ops`）

```
┌───────────────────────────────────────────────────┬──────────────────────┐
│  固定指标栏（始终显示）                              │                      │
│  今日订单 N  |  今日支付 ¥X  |  最新事件 M 条       │  AI 侧边栏（常驻）    │
├───────────────────────────────────────────────────┤                      │
│                                                   │  [Skill 选择器]       │
│  动态画布区                                        │                      │
│  ┌─────────────────────────────────────────────┐  │  消息列表             │
│  │ 初始状态：欢迎引导                            │  │  （共享全局 chat）    │
│  │                                             │  │                      │
│  │ 或：AI 渲染的数据组件                        │  │                      │
│  │  - OpsStatsPanel                            │  │                      │
│  │  - OpsActivityTimeline                      │  │  ──────────────────  │
│  │  - OpsRecentList                            │  │  [输入框]            │
│  └─────────────────────────────────────────────┘  │                      │
└───────────────────────────────────────────────────┴──────────────────────┘
```

**三个区域的分工**：

| 区域 | 控制方 | 说明 |
|------|--------|------|
| 固定指标栏 | 前端自动加载 | 进入页面时调用 `activity_stats?period=today`，AI 不控制 |
| 动态画布区 | AI 对话驱动 | 收到 tool_result 时更新，由 `useOpsCanvas` 管理状态 |
| AI 侧边栏 | 共享全局 `useAiChat` | 与其他页面共享同一对话实例，仅布局形态不同 |

> **决策 IO2-S2-1**：OpsPage 使用全局 `inject('aiChat')` 而非独立实例。理由：技术最简单，无需改 App.vue；进入 OpsPage 只需附加 `onToolCallSuccess` 监听，离开时反注册。切换到其他页面时对话历史保留，体验连贯。

> **决策 IO2-S2-2**：AI 侧边栏宽度固定 360px，不可调整（MVP）。常驻展示，不需要呼出操作。OpsPage 进入时通过 `meta.hideGlobalChatButton` 隐藏全局 `AiChatButton`，避免重复。

#### 全局 AiChatButton 隐藏机制

在路由 meta 中标记，`App.vue` 根据当前路由决定是否显示：

```javascript
// router/index.js
{ path: '/ops', name: 'Ops', component: OpsPage, meta: { hideGlobalChatButton: true } }
```

```vue
<!-- App.vue -->
<AiChatButton v-if="!route.meta.hideGlobalChatButton" @toggle="chatOpen = !chatOpen" />
<AiChatPanel v-if="!route.meta.hideGlobalChatButton" :open="chatOpen" @close="chatOpen = false" />
```

---

### S3：Activity MCP 工具扩展（L1）

**当前状态**：`activity_query` 支持 `action=list|recent|stats`，list 只支持 `orderId` 筛选，没有 userId/skuId/spuId，也没有 `_raw` 结构化数据。

**扩展内容**：

1. `activity_query`（list action）新增可选参数：
   - `userId: number?` — 按用户 ID 查事件序列
   - `skuId: number?` — 按 SKU ID 查关联事件
   - `spuId: number?` — 按 SPU ID 查关联事件

2. 所有 action 的返回结果新增 `_raw` 字段，附带结构化数据供前端渲染（见 S1 决策 IO2-S1-3）。

3. Tool 描述（description）更新，明确告知 LLM 可用的查询维度，引导正确调用。

---

### S4：智能运营助手 Skill（L1）

新增一个 Skill，专门用于智能运营场景：

| 字段 | 值 |
|------|-----|
| **名称** | 智能运营助手 |
| **描述** | 智能运营数据查询与分析：查询事件序列（按订单/用户/商品维度）、查询统计指标、查看最近事件。 |
| **allowedTools** | `activity_query` |
| **audience** | `admin` |

**System Prompt 要点**：
- 角色：HMall 智能运营助手，帮助运营者通过数据了解系统运行状况
- 主要用 `activity_query` 工具查询事件数据
- 事件类型语义说明（中文翻译）：OrderCreated=「订单已创建」、PaymentCompleted=「支付完成」、OrderCompleted=「订单已完成」等
- 查询用户维度时传 userId；查询商品维度时传 skuId 或 spuId；查询订单时传 orderId
- 统计数据展示时换算价格单位（分 → 元）
- 不执行任何写操作

新增 **`intelligent-ops-domain` MCP Resource**：
- URI：`hmall://intelligent-ops/domain-knowledge`
- 内容：一级流程定义（交易/用户发展/商品运营）、事件类型与流程的映射（EventMetadata processRoles）、BusinessActivity 多维关联说明
- 作用：注入 LLM 上下文，使 AI 回答带流程语义（如「这是交易流程中的里程碑事件」）

> **决策 IO2-S4-1**：Skill 由运营配置（非代码）创建，通过 Admin 的 Skill 管理界面录入。MCP Resource 是代码实现（在 hmall-mcp 中注册），两者配合使用。

---

### S5：全局 AI 面板隔离（L1）

当用户在 `/ops` 页面，全局 `AiChatButton`（右下角浮动按钮）和 `AiChatPanel`（Drawer）隐藏。原因：
1. 功能重叠——侧边栏已提供完整对话能力
2. 视觉冲突——右下角按钮在有右侧边栏时位置冲突

离开 `/ops` 页面后全局面板恢复正常。具体实现见 S2 中「全局 AiChatButton 隐藏机制」。

---

## 三、变更分析

### MCP（hmall-mcp）（🟡 中等）

#### 工具变更

**`activity_query` 扩展**（`hmall-mcp/tools/activity.js`）：

- `list` action 新增可选参数：
  - `userId: z.number().optional()` — 传入时调用 `GET /api/activities?userId={n}`
  - `skuId: z.number().optional()` — 传入时调用 `GET /api/activities?skuId={n}`
  - `spuId: z.number().optional()` — 传入时调用 `GET /api/activities?spuId={n}`
  - 多参数传入时按 orderId → userId → skuId → spuId 优先级取第一个非空
- 所有 action 返回结构变更：在原有 `{ content: [{ type: "text", text: "..." }] }` 外，追加 `_raw` 字段：
  - list/recent：`_raw: { type: "activity_list", items: ActivityItem[] }`
  - stats：`_raw: { type: "activity_stats", data: StatsObject }`
- Tool description 更新：补充 userId/skuId/spuId 参数说明，引导 LLM 正确使用多维查询

#### Resource 新增

新增 `hmall-mcp/resources/intelligent-ops-domain.js`：
- URI：`hmall://intelligent-ops/domain-knowledge`
- Name：`HMall 智能运营领域知识`
- 内容：
  - 一级流程定义（交易/用户发展/商品运营，含关联键说明）
  - 事件类型列表及其流程归属（processRoles）
  - BusinessActivity 多维关联说明（userId、correlationKeys 含义）
  - 查询建议（查用户用 userId，查商品用 skuId/spuId，查订单用 orderId）
  - 常见场景示例（「用户 X 发生了什么」→ `activity_query list userId=X`）

在 `hmall-mcp/index-http.js` 中注册新 Resource。

#### 需求场景变更

- 🔄 修改 5.4：Activity 模块工具支持按 userId/skuId/spuId 多维查询（原仅支持 orderId）

---

### Smart Interaction（🟢 轻微）

#### 前端代码变更（`frontend/admin/src/shared/composables/useAiChat.js`）

- `onToolCallSuccess(callback)` 返回反注册函数：
  ```javascript
  function onToolCallSuccess(callback) {
    toolCallCallbacks.push(callback)
    return () => {
      const idx = toolCallCallbacks.indexOf(callback)
      if (idx > -1) toolCallCallbacks.splice(idx, 1)
    }
  }
  ```

#### 路由变更（`frontend/admin/src/router/index.js`）

- 新增路由：`{ path: '/ops', name: 'Ops', component: () => import('../pages/OpsPage.vue'), meta: { title: '智能运营', hideGlobalChatButton: true } }`

#### App.vue 变更

- `AiChatButton` 和 `AiChatPanel` 加 `v-if="!route.meta.hideGlobalChatButton"` 控制
- 引入 `useRoute`

#### 运营配置（非代码）

- 新增「智能运营助手」Skill（管理员在 Admin Skill 管理界面手动创建，见 S4 规格）

---

### 新增前端模块（🔲 全新）

以下文件全部新增于 `frontend/admin/src/`：

#### `pages/OpsPage.vue`

智能运营主页面，三区域布局：

- **固定指标栏**：进入页面自动加载 `activity_query { action: "stats", period: "today" }`，展示今日订单数、今日支付总额、今日事件数。
- **动态画布区**：受 `useOpsCanvas` 驱动，根据当前状态渲染对应组件（OpsStatsPanel / OpsActivityTimeline / OpsRecentList / 欢迎引导）。
- **AI 侧边栏**：内嵌 `OpsAiSidebar`，共享全局 `inject('aiChat')`；注册 `onToolCallSuccess` 并在 `onUnmounted` 反注册。

#### `shared/composables/useOpsCanvas.js`

画布状态管理：

```javascript
const TOOL_TO_CANVAS_MAP = {
  activity_query: (result) => {
    if (!result._raw) return null
    if (result._raw.type === 'activity_list') return { type: 'TIMELINE', data: result._raw.items }
    return null
  },
  activity_recent: (result) => {
    if (!result._raw) return null
    return { type: 'RECENT', data: result._raw.items }
  },
  activity_stats: (result) => {
    if (!result._raw) return null
    return { type: 'STATS', data: result._raw.data }
  },
}

export function useOpsCanvas() {
  const state = ref({ type: 'EMPTY' })  // EMPTY | LOADING | STATS | TIMELINE | RECENT
  const loadingFor = ref(null)  // 当前 loading 的 tool name

  function onToolCall(toolName) {
    if (TOOL_TO_CANVAS_MAP[toolName]) {
      loadingFor.value = toolName
      state.value = { ...state.value, loading: true }
    }
  }

  function onToolResult(toolName, result) {
    loadingFor.value = null
    const mapped = TOOL_TO_CANVAS_MAP[toolName]?.(result)
    if (mapped) state.value = mapped
    else state.value = { ...state.value, loading: false }
  }

  function reset() { state.value = { type: 'EMPTY' } }

  return { state, onToolCall, onToolResult, reset }
}
```

> 注：`onToolCall` 需要 `useAiChat` 暴露 tool_call 事件回调，目前 `useAiChat` 只有 `onToolCallSuccess`（结果回调），需同时暴露 `onToolCallStart(callback)`（工具开始执行时回调）以实现 LOADING 状态。这是对 `useAiChat.js` 的第二处微调。

#### `shared/ui/ops/OpsAiSidebar.vue`

常驻侧边栏 AI 面板，复用现有原子组件：
- 顶部：Skill 选择器（复用 `SkillSelector.vue`）
- 中部：消息列表（复用 `AiMessageList.vue`）
- 底部：输入框（复用 `AiChatInput.vue`）
- 不包含 Skill 管理、系统设置等 Drawer 特有功能（保持轻量）

#### `shared/ui/ops/OpsStatsPanel.vue`

展示统计数据，数据来源：`activity_stats` 的 `_raw.data`。

布局：多指标卡片网格，展示字段：
- 今日/区间：订单数、支付成功数、支付总额、履约发货数、库存占用次数

技术：使用现有 `StatCard.vue` 组件（已有）。

#### `shared/ui/ops/OpsActivityTimeline.vue`

展示事件时间线，数据来源：`activity_query list` 的 `_raw.items`。

布局：时间线列表，每条事件显示：时间、事件类型（中文）、关联维度（orderId/userId/spuId）、BC 来源。

查询维度标识：根据 items 中哪个维度有值，显示「用户 #42 的事件」「订单 #123 的旅程」等标题。

技术：不依赖图表库，纯 Tailwind CSS 时间线样式。

#### `shared/ui/ops/OpsRecentList.vue`

展示最近事件列表，数据来源：`activity_recent` 的 `_raw.items`。

布局：简单列表，按时间倒序，显示：时间、事件类型、订单 ID。

---

### BC 间数据流

```
用户 → OpsPage → useAiChat.sendMessage()
    → POST /api/ai/chat（Smart Interaction）
    → LLM 决策调用 activity_query
    → MCP activity_query → GET /api/activities?userId=X（Activity API）
    → tool_result 含 _raw 结构化数据
    → onToolCallSuccess 回调 → useOpsCanvas.onToolResult()
    → canvasState 更新 → OpsActivityTimeline 渲染
```

---

## 四、迭代计划

### 迭代 0：基础设施准备 ✅ 已完成

**涉及模块**：MCP（hmall-mcp），`useAiChat.js`
**前置依赖**：无

**后端（MCP）**：
- `activity_query` 扩展：list action 增加 userId/skuId/spuId 参数；所有 action 返回 `_raw` 结构化数据
- 新增 `intelligent-ops-domain` Resource（hmall-mcp/resources/）
- 在 index-http.js 注册新 Resource

**前端**：
- `useAiChat.js` 微调：`onToolCallSuccess` 返回反注册函数；新增 `onToolCallStart` 回调支持

**验收标准**：
- 调用 `activity_query { action: "list", userId: 1 }` 返回该用户的事件列表，result 包含 `_raw.items`
- 调用 `activity_query { action: "stats" }` 返回统计数据，result 包含 `_raw.data`
- MCP Resource `hmall://intelligent-ops/domain-knowledge` 可发现并读取
- `useAiChat.onToolCallSuccess(cb)` 返回反注册函数，反注册后 cb 不再被调用

---

### 迭代 1：智能运营页面（OpsPage MVP） ✅ 已完成

**涉及模块**：frontend/admin
**前置依赖**：迭代 0

**前端**：
- 新增 `pages/OpsPage.vue`（三区域布局）
- 新增 `shared/composables/useOpsCanvas.js`
- 新增 `shared/ui/ops/OpsAiSidebar.vue`
- 新增 `shared/ui/ops/OpsStatsPanel.vue`
- 新增 `shared/ui/ops/OpsActivityTimeline.vue`
- 新增 `shared/ui/ops/OpsRecentList.vue`
- `router/index.js` 新增 `/ops` 路由（含 `hideGlobalChatButton: true`）
- `App.vue` 支持 `hideGlobalChatButton` meta 控制
- `AppHeader.vue` 导航新增「智能运营」链接

**验收标准**：
- 进入 `/ops` 页面，固定指标栏展示今日统计数据
- 全局 AiChatButton 在 `/ops` 页面隐藏，离开后恢复
- 常驻侧边栏显示 AI 对话面板，可正常收发消息
- AI 侧边栏输入框发送消息后，对话正常流式输出

---

### 迭代 2：对话-画布联动 ✅ 已完成

**涉及模块**：frontend/admin
**前置依赖**：迭代 0、迭代 1

**前端**：
- OpsPage 注册 `onToolCallSuccess` 和 `onToolCallStart` 监听
- `useOpsCanvas` 接入：收到 tool_result 后更新画布状态
- 三个画布组件实现完整渲染逻辑

**验收标准**：
- 对话框输入「最近发生了什么」→ AI 调用 activity_query，画布渲染 OpsRecentList
- 对话框输入「今天的整体情况如何」→ AI 调用 activity_query stats，画布渲染 OpsStatsPanel
- 对话框输入「用户 1 的事件序列」→ AI 调用 activity_query list userId=1，画布渲染 OpsActivityTimeline，标题显示「用户 #1 的事件」
- AI 工具调用期间画布显示 loading 状态（骨架屏）

**E2E 验收**：`BIZ-IO2-001`
- 场景：进入智能运营页面，通过对话触发画布渲染，验证对话-画布联动全链路
- 步骤：进入 `/ops` → 输入「最近发生了什么」→ 等待 AI 回复 → 验证画布出现事件列表 → 输入「用户 1 的事件」→ 验证画布切换为时间线视图

---

### 迭代 3：智能运营助手 Skill（运营配置） ✅ 已完成

**涉及模块**：Smart Interaction（运营配置，非代码）
**前置依赖**：迭代 0（MCP Resource 就绪）

**配置操作**：
- 管理员进入 Admin → AI Chat 面板 → Skill 管理 → 创建新 Skill
- 录入「智能运营助手」Skill 规格（见 S4 场景分析）

**验收标准**：
- 进入 `/ops` 页面，对话时自动匹配「智能运营助手」Skill
- AI 回复中事件类型使用中文语义（如「订单已创建」而非「OrderCreated」）
- AI 能根据查询维度提示用法（如「可以告诉我订单 ID 或用户 ID，我来帮你查」）

---

## 交付跟踪

### 迭代 0：基础设施准备 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | MCP: activity_query 扩展多维参数 + `_raw` 结构化返回 | mcp-development | — | ✅ 完成 |
| 2 | MCP: 新增 intelligent-ops-domain Resource | mcp-development | — | ✅ 完成 |
| 3 | useAiChat.js: onToolCallSuccess 反注册 + onToolCallStart | — | — | ✅ 完成 |
| 3b | McpToolBridge: `_raw` 透传至前端（ToolExecutionResult） | — | #1 | ✅ 完成 |

**交付日期**：2026-03-12

### 迭代 1：智能运营页面（OpsPage MVP） ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 4 | frontend/admin: OpsPage + 布局 + 常驻侧边栏 + 路由 | frontend-development | 迭代 0 | ✅ 完成 |

**交付日期**：2026-03-12

### 迭代 2：对话-画布联动 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 5 | frontend/admin: useOpsCanvas + 三画布组件 + 联动接入 | frontend-development | 迭代 0, 1 | ✅ 完成 |
| 6 | E2E 交付门禁 BIZ-IO2-001 | deliver-requirement | 迭代 0, 1, 2 | ✅ 完成 |

**交付日期**：2026-03-12

### 迭代 3：智能运营助手 Skill（运营配置） ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 7 | 创建「智能运营助手」Skill（Admin 界面配置） | — | 迭代 0 | ✅ 完成 |

**交付日期**：2026-03-12
**本需求已全部交付。**
