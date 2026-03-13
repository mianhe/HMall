# 智能运营画布交互重新设计

> 本文档是对 [Step 2 overview](./overview.md) 中「对话-画布联动」能力的重新设计。  
> Step 2 MVP 已交付，本设计针对 MVP 暴露的交互问题进行架构级改进。

---

## 一、问题与根因

### 1.1 已观察到的问题

Step 2 MVP 上线后，在实际使用中暴露三个核心问题：

1. **内容显示混乱**：用户无法预判画布何时变化、变成什么，不同表述下画布反应不一致
2. **可视化能力缺失**：无法绘制趋势图等图表，用户说"过去 7 天的趋势"得不到趋势图
3. **逻辑不一致**：类似的查询有时能取到数据、有时不能；数据取到了也画不出图

### 1.2 根因

**一个根因**：AI 与画布之间没有通信通道——画布由工具结果的数据类型被动驱动，而非由 AI 的展示意图主动驱动。

当前链路：

```
AI 调用 activity_query → 工具返回 _raw → 前端按 _raw.type 硬编码映射到组件
```

画布更新是数据查询的**附带副作用**，不是 AI 的**主动决策**。AI 在此过程中无法表达三件事：
- 要不要更新画布
- 用什么可视化形式
- 展示哪些维度的数据

---

## 二、设计目标

| # | 目标 | 侧重 |
|---|------|------|
| G1 | **对话即查询**：用户用自然语言表达意图，系统自动完成数据获取与呈现，用户无需了解底层数据模型 | 智能 |
| G2 | **左右屏各司其职**：右屏承载文字对话与分析解读，左屏承载数据可视化；两者互补不重复，如同"演讲者 + 幻灯片" | 友好 |
| G3 | **画布行为可预期**：AI 主动控制画布，AI 的文字中自然引出画布内容；用户能从对话预判画布变化 | 友好 |
| G4 | **可视化智能选择**：AI 根据数据特征和用户意图自动选择最合适的图表类型（折线图 / 柱状图 / 卡片 / 时间线等） | 智能+友好 |
| G5 | **上下文连续性**：支持在前一轮基础上追问或调整，无需重复查询条件 | 智能 |

---

## 三、核心设计：Canvas Command 协议

### 3.1 设计思路

引入 MCP 工具 `ops_canvas`，作为 AI 到画布的**专用通信通道**。

**核心原则**：画布只响应 `ops_canvas` 工具调用，不再响应其他工具的副作用。

AI 的工作流变为**显式两步**：

```
第一步（数据获取）：调用 activity_query，拿到原始数据
第二步（画布渲染）：调用 ops_canvas，指定可视化方式、标题、数据
```

这两步对应 LLM 的两轮 tool call。现有的 `streamWithToolCallLoop` 已支持多轮 tool call，无需改动。

### 3.2 ops_canvas 工具定义

```
ops_canvas({
  view: "line_chart" | "bar_chart" | "pie_chart" | "stat_cards" | "timeline" | "table" | "event_list",
  title: string,       // 画布标题，如 "过去 7 天成交金额趋势"
  data: object          // 视图数据，结构随 view 类型不同
})
```

**工具实现**：MCP 工具，handler 仅做校验并将数据原样返回。返回给 LLM 的文字为简短确认（如"已渲染：过去 7 天成交金额趋势"），返回给前端的 `_raw` 携带完整的画布指令。

### 3.3 data 结构约定

| view | data 结构 | 适用场景 |
|------|-----------|---------|
| `line_chart` | `{ labels: string[], series: [{ name: string, values: number[] }] }` | 时间趋势、变化走势 |
| `bar_chart` | `{ labels: string[], series: [{ name: string, values: number[] }] }` | 多指标对比、分类比较 |
| `pie_chart` | `{ items: [{ label: string, value: number }] }` | 占比分析 |
| `stat_cards` | `{ cards: [{ label: string, value: string(已格式化), status?: "success"\|"warning"\|"critical", description?: string }] }` | 洞察概览（AI 从原始数据中提炼派生指标） |
| `timeline` | `{ items: [{ time: string, type: string, title: string, detail?: string }] }` | 事件序列、订单旅程 |
| `table` | `{ columns: [{ key: string, label: string }], rows: object[] }` | 详细数据明细 |
| `event_list` | `{ items: [{ time: string, type: string, orderId?: number }] }` | 近期事件快览 |

### 3.4 画布行为规则

| 条件 | 画布行为 | 用户感知 |
|------|---------|---------|
| AI 调用 `ops_canvas` | 渲染指定视图 | AI 文字中会自然引出（如"以下是趋势图"） |
| AI 调用 `activity_query`（不调用 `ops_canvas`） | 画布不变 | AI 仅在文字中回答 |
| AI 纯文字回复 | 画布不变 | 正常对话 |
| 清空对话 | 画布回到 EMPTY（欢迎引导） | 重新开始 |
| 进入页面 | 固定指标栏自动加载；画布为 EMPTY | 首次进入 |

**关键**：画布变化 100% 由 `ops_canvas` 触发，不再有隐式副作用。

### 3.5 对 5 个设计目标的达成

| 目标 | 如何达成 |
|------|---------|
| G1 对话即查询 | AI 理解意图 → 调用 activity_query 获取数据 → 调用 ops_canvas 展示。用户只需说自然语言 |
| G2 左右屏各司其职 | AI 文字走右屏（分析解读），ops_canvas 走左屏（图表）。两条路径独立，互补不重复 |
| G3 画布行为可预期 | 画布只响应 ops_canvas；AI 文字中会自然引出画布内容；不调用 ops_canvas 时画布不变 |
| G4 可视化智能选择 | AI 在第二步中根据数据特征选择 view 类型。Skill prompt 提供选择指引 |
| G5 上下文连续性 | LLM 上下文保持对话历史，追问时无需重复条件。AI 可直接在已有数据基础上调整画布 |

---

## 四、数据层补齐

### 4.1 新增时序统计接口

当前 `activity_stats` 只返回时段聚合总量，无法支撑趋势图所需的逐日数据。

**新增接口**：

```
GET /api/activities/stats/daily?from=YYYY-MM-DD&to=YYYY-MM-DD
```

**返回**：

```json
[
  {
    "date": "2026-03-06",
    "ordersCreated": 8,
    "ordersCancelled": 1,
    "ordersCompleted": 5,
    "paymentSuccess": 7,
    "paymentTotalCents": 1558900,
    "fulfillmentShipped": 3,
    "stockReserved": 8
  },
  ...
]
```

与现有 `stats` 接口结构一致，但按日拆分。

### 4.2 MCP 工具扩展

`activity_query` 新增 `action: "stats_daily"`：

| 参数 | 说明 |
|------|------|
| `action: "stats_daily"` | 按日统计 |
| `from: string` | 起始日期 YYYY-MM-DD（必填） |
| `to: string` | 结束日期 YYYY-MM-DD（必填） |

返回 `_raw: { type: "activity_stats_daily", items: DailyStats[] }`。

---

## 五、场景验证

### 场景 A："过去 7 天的销售趋势"

```
Round 1  AI → activity_query { action: "stats_daily", from: "2026-03-06", to: "2026-03-12" }
         ← 7 天逐日数据

Round 2  AI → ops_canvas {
           view: "line_chart",
           title: "过去 7 天成交金额趋势",
           data: {
             labels: ["03-06", "03-07", "03-08", "03-09", "03-10", "03-11", "03-12"],
             series: [{ name: "成交金额(元)", values: [15589, 11561, 10093, 12791, 23638, ...] }]
           }
         }

Round 3  AI 输出文字：
         "过去 7 天成交金额整体呈波动走势，其中 03-12 达到峰值 ¥23,638，
          03-10 为低谷 ¥10,093。近 2 天有强劲反弹。"
```

- 左屏：折线图
- 右屏：文字分析与洞察
- 互补不重复 ✅

### 场景 B："今天的整体情况"

```
Round 1  AI → activity_query { action: "stats", period: "today" }
         ← 今日聚合统计

Round 2  AI 从原始数据计算派生指标，构建洞察卡片：
         AI → ops_canvas {
           view: "stat_cards",
           title: "今日经营洞察",
           data: {
             cards: [
               { label: "成交总额", value: "¥23,638", description: "6 笔支付" },
               { label: "支付成功率", value: "85.7%", status: "success", description: "7 次尝试中 6 次成功" },
               { label: "订单转化", value: "6/8", description: "2 笔未支付" }
             ]
           }
         }

Round 3  AI 输出文字分析：整体健康，关注 2 笔未支付订单等。
```

- 左屏：洞察卡片（派生指标 + 状态标记，而非原始数字搬运）
- 右屏：详细分析 ✅

### 场景 C："帮我解释一下什么是 OrderCreated 事件"

```
Round 1  AI 不调用任何工具
         AI 输出："OrderCreated 是订单创建事件，当用户提交订单时由 Order BC 发出…"
```

- 左屏：保持不变（不会莫名跳转）
- 用户预期清晰：概念问答不影响画布 ✅

### 场景 D：追问 "按天看看具体数据"（接场景 A）

```
Round 1  AI 理解上下文——"具体数据"指同一个 7 天区间
         AI → ops_canvas {
           view: "table",
           title: "过去 7 天每日运营数据",
           data: {
             columns: [
               { key: "date", label: "日期" },
               { key: "orders", label: "订单数" },
               { key: "payment", label: "成交金额" },
               { key: "shipped", label: "已发货" }
             ],
             rows: [
               { date: "03-06", orders: 8, payment: "¥15,589", shipped: 3 },
               ...
             ]
           }
         }

Round 2  AI 输出："以下是每日数据明细。03-12 订单量最高。"
```

- AI 无需重新查询（上一轮数据仍在 LLM 上下文中）
- 左屏从折线图切换为表格
- 上下文连续 ✅

### 场景 E："用户 1 最近的订单旅程"

```
Round 1  AI → activity_query { action: "list", userId: 1 }
         ← 用户 1 的事件序列

Round 2  AI → ops_canvas {
           view: "timeline",
           title: "用户 #1 的事件序列",
           data: {
             items: [
               { time: "2026-03-12 10:30", type: "OrderCreated", title: "订单已创建", detail: "订单 #42" },
               { time: "2026-03-12 10:31", type: "PaymentSuccess", title: "支付完成", detail: "¥299.00" },
               ...
             ]
           }
         }

Round 3  AI 输出："用户 #1 今日创建了 2 笔订单，均已支付成功并进入履约流程。"
```

- 左屏：时间线
- 右屏：旅程总结 ✅

### 场景 F："对比一下这 7 天的订单量和支付量"（接场景 A）

```
Round 1  AI → ops_canvas {
           view: "bar_chart",
           title: "过去 7 天订单量 vs 支付成功量",
           data: {
             labels: ["03-06", "03-07", "03-08", "03-09", "03-10", "03-11", "03-12"],
             series: [
               { name: "订单创建", values: [8, 5, 6, 4, 7, 6, 8] },
               { name: "支付成功", values: [6, 4, 6, 3, 5, 5, 6] }
             ]
           }
         }

Round 2  AI 输出："整体来看支付成功率约 80%，03-09 支付成功率最低（75%）。"
```

- AI 复用上下文中已有的逐日数据，无需重新查询
- 左屏从折线图变为双系列柱状图
- 同一份数据换了可视化形式 ✅

---

## 六、Skill System Prompt 设计要点

「智能运营助手」Skill 的 system prompt 需要教会 AI 以下行为：

### 两步工作流

```
当用户的问题适合可视化呈现时：
  第一步：调用 activity_query 获取数据
  第二步：调用 ops_canvas 选择合适的图表类型，将数据渲染到画布

当用户只是在问概念或做简单问答时：
  直接用文字回答，不调用 ops_canvas
```

### 可视化类型选择指引

```
根据数据特征选择 view 类型：
- 时间维度的变化趋势 → line_chart
- 多指标并列比较 → bar_chart
- 组成占比分析 → pie_chart
- 运营洞察概览（派生指标 + 健康状态） → stat_cards
- 单个实体的事件流/旅程 → timeline
- 详细数据查看 → table
- 近期事件快览 → event_list
```

### 左右屏协作

```
画布是你的"幻灯片"，文字是你的"解读"。两者配合：
- 画布展示数据的直观可视化
- 文字提供分析洞察、趋势判断、关键发现
- 不要在文字中重复画布上已经展示的原始数据
- 更新画布时在文字中自然引出，如"以下是过去 7 天的趋势图"
```

---

## 七、架构变更总览

### 新增

| 模块 | 变更 |
|------|------|
| Activity Service | `GET /api/activities/stats/daily` 时序统计接口 |
| MCP (hmall-mcp) | `ops_canvas` 工具（画布渲染命令） |
| MCP (hmall-mcp) | `activity_query` 新增 `stats_daily` action |
| 前端 | `OpsChart.vue`（基于 Chart.js，支持 line / bar / pie） |
| 前端 | `OpsTable.vue`（数据表格组件） |
| 前端依赖 | Chart.js |

### 修改

| 模块 | 变更 |
|------|------|
| 前端 `useOpsCanvas.js` | 移除 `TOOL_TO_CANVAS_MAP` 硬编码映射，改为只响应 `ops_canvas` |
| 前端 `OpsPage.vue` | 画布区支持新视图类型（LINE_CHART / BAR_CHART / PIE_CHART / TABLE） |
| Skill | 更新「智能运营助手」system prompt，教会 AI 两步工作流与可视化选择 |

### 不变

| 模块 | 说明 |
|------|------|
| `useAiChat.js` | 现有 `onToolCallSuccess` / `onToolCallStart` 机制完全适用 |
| `AiChatService.java` | 多轮 tool call loop 已支持两步工作流 |
| `McpToolBridge.java` | `_raw` 透传机制已适用 |
| 固定指标栏 | 保持独立加载，不受 AI 控制 |
| Activity Service 原有 API | `stats` / `list` / `recent` 保持不变 |

---

## 八、实施计划

### 迭代 A：数据层 + Canvas Command 基础

**后端**：
- Activity Service：新增 `GET /api/activities/stats/daily` 接口
- MCP：`activity_query` 新增 `stats_daily` action
- MCP：新增 `ops_canvas` 工具

**前端**：
- 引入 Chart.js
- 新增 `OpsChart.vue`（line / bar / pie）
- 新增 `OpsTable.vue`
- 重写 `useOpsCanvas.js`：只响应 `ops_canvas`
- `OpsPage.vue` 画布区适配新视图类型

**验收**：AI 调用 ops_canvas 后画布正确渲染对应图表

### 迭代 B：Skill 调优 + 端到端联动

- 更新「智能运营助手」Skill system prompt
- 端到端验证所有场景（A~F）
- 确认两步工作流稳定运行

**验收**：用户说"过去 7 天的趋势"→ 左屏出现折线图 + 右屏出现文字分析

---

## 九、Prompt 体系与多面板增强（Step 3 补充）

> 本节记录在 Step 2 基础上对 Prompt 体系和画布机制的增强改进。

### 9.1 改进背景

Step 2 MVP 暴露两个系统性问题：
1. **Prompt 机制缺失**：智能运营没有完整的四层 Prompt 体系（Tool Schema → MCP Resource → Base Prompt → Skill Prompt），导致 AI 不知道日期、不稳定地执行两步工作流、图表选择随机。
2. **单幅图限制**：画布一次只能显示一个面板，无法展示综合经营概览等多维度信息。

### 9.2 Prompt 体系改进

| 改动 | 说明 |
|------|------|
| **Base Prompt 注入日期** | `AiChatService.buildDefaultSystemPrompt` 增加 `当前日期：YYYY-MM-DD（星期X）`，系统级改动，所有场景受益 |
| **事件本体文档** | 新增 `docs/intelligent-ops-ontology.md`，定义事件类型全表、Payload 结构、状态机、因果链、健康指标推导 |
| **MCP Resource 重构** | `intelligent-ops-domain.js` 改为从 Markdown 文件读取（与 `ontology.js` 模式一致），领域知识可读可维护 |
| **ops_canvas 精简** | Tool description 从 ~300 字精简为 ~80 字，操作策略移至 Skill Prompt |
| **智能运营助手 Skill** | 新增 Skill：两步工作流 + 多面板策略 + 图表选择指南 + data 结构约定 + few-shot 示例 |

### 9.3 多面板画布改进

| 改动 | 说明 |
|------|------|
| **useOpsCanvas 状态模型** | 从单一 `state` 对象改为 `panels` 数组 + `phase`（EMPTY/LOADING/READY），同一轮多次 `ops_canvas` 追加面板 |
| **OpsPage 面板渲染** | 多面板自适应网格布局（1 个面板全宽，≥2 个面板按类型智能分列） |
| **对话轮次管理** | `onTurnEnd` 回调重置 turn 标记，下一轮对话清空旧面板 |
| **画布上下文传递** | 前端发送 `context.canvasPanels` 摘要，后端注入 System Prompt，支持追问时引用当前画布 |
| **useAiChat 增强** | 新增 `onDone` 回调 + `contextExtras` 扩展机制 |
