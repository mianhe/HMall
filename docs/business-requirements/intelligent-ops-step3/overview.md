# 智能运营 Step 3：订单事实分析（Order Fact Analytics）

## 一、需求概述与场景

### 业务背景与目标

智能运营 Step 1（多维事件基座）和 Step 2（Smart Interaction 接入 + 对话驱动页面）已完成，AI 运营助手可以做**事件级别的计数和查询**（L1 能力）：今天有多少订单、支付成功率多少、某订单经历了什么。

但当运营人员提出更深层的分析问题时——"含增值服务的订单占比多少？""哪个商品卖得最好？""镭雕订单的客单价比普通订单高多少？"——AI 无法回答。根因是 Activity BC 只有事件流（ODS 层），缺少以订单为中心和以商品为中心的分析视图（DWD 层）。

**本需求的目标**：在 Activity BC 内部构建 **OrderFact（订单事实）** 和 **OrderItemFact（订单商品明细事实）** 两个 CQRS 读模型，从已有事件链自动投影派生，为智能运营提供订单维度和商品维度的多维分析能力（L2）。

### 需求类型

**扩展已有能力**。在 Activity BC 现有的事件消费和统计基础上，新增分析读模型和查询 API。

| 维度 | 已有能力（Step 1+2） | 本需求新增（Step 3） |
|------|---------------------|---------------------|
| 数据层 | BusinessActivity（事件流，ODS） | OrderFact + OrderItemFact（分析视图，DWD） |
| 分析粒度 | 事件级计数（多少个 OrderCreated） | 订单级属性推导（VAS 渗透率、阶段分布、时效） |
| 商品分析 | 按 spuId/skuId 筛选事件 | 商品销量/销售额排名、商品-VAS 关联 |
| AI 能力 | 回答"发生了什么"（L1 事件计数） | 回答"整体状况如何"（L2 多维分析） |

### 影响面

| 影响范围 | 影响程度 | 说明 |
|---------|---------|------|
| **Activity BC** | 🔴 重大 | 新增 OrderFact/OrderItemFact 领域概念、投影逻辑、分析 API |
| **hmall-mcp** | 🟡 中等 | 新增 `order_fact_query` MCP 工具、更新 Ontology 文档 |
| **Smart Interaction** | 🟢 轻微 | 更新「智能运营助手」Skill 的 allowedTools 和 System Prompt |
| **其他 BC** | ⚪ 无变更 | 不修改任何上游 BC 的事件发布逻辑 |
| **前端** | ⚪ 无变更 | 运营画布已支持所有所需视图类型（stat_cards/pie_chart/bar_chart 等） |

### 用户分析场景全景

运营/管理人员可能提出的问题，按主题分为五类：

#### A. 订单维度分析

| # | 场景 | 示例问题 |
|---|------|---------|
| A1 | VAS 渗透率 | "含增值服务的订单占比多少？" |
| A2 | VAS 价值分析 | "镭雕订单的客单价比普通订单高多少？" |
| A3 | 订单状态分布 | "当前有多少在途订单？多少已完成？" |
| A4 | 支付时效 | "用户平均多久完成支付？" |
| A5 | 履约时效 | "从支付到签收平均多少天？" |
| A6 | 取消原因 | "取消订单中，超时取消和主动取消各占多少？" |
| A7 | 多件订单 | "多件商品订单占比多少？客单价差异？" |
| A8 | 漏斗分析 | "从下单到签收的转化漏斗是什么样的？" |
| A9 | VAS 组合 | "同时选镭雕+延保的订单多不多？" |

#### B. 商品维度分析

| # | 场景 | 示例问题 |
|---|------|---------|
| B1 | 销量排行 | "哪个商品卖得最好？" |
| B2 | 销售额排行 | "各商品销售额排名？" |
| B3 | 商品趋势 | "某商品最近 7 天卖了多少？" |
| B4 | 商品取消率 | "哪些商品的退单率高？" |
| B5 | VAS-商品关联 | "镭雕服务和哪些商品搭配最多？" |
| B6 | SKU 热度 | "同一商品下哪个规格最受欢迎？" |

#### C. 用户维度分析

| # | 场景 | 示例问题 |
|---|------|---------|
| C1 | 复购率 | "复购用户占比多少？" |
| C2 | 用户价值 | "高价值用户（累计消费 Top）有哪些？" |
| C3 | VAS 偏好 | "选镭雕的用户和不选的用户客单价差异？" |
| C4 | 新用户 | "最近 7 天有多少新用户下单？" |

#### D. 时间维度分析

| # | 场景 | 示例问题 |
|---|------|---------|
| D1 | 时段分布 | "一天中哪个时段下单最多？" |
| D2 | 周末效应 | "周末和工作日销售差异？" |
| D3 | VAS 趋势 | "增值服务渗透率在逐步提升吗？" |

#### E. 已有能力（Step 1+2 已覆盖，不在本需求范围内）

| # | 场景 | 示例问题 |
|---|------|---------|
| E1 | 经营大盘 | "最近 7 天经营情况怎么样？" |
| E2 | 支付健康 | "支付成功率正常吗？" |
| E3 | 订单追踪 | "订单 42 经历了什么？" |

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | 订单事实投影 | 支撑流程 | L2 中分析 | 事件写入时投影为 OrderFact + OrderItemFact |
| F2 | 订单维度运营分析 | 主流程 | L2 中分析 | AI 通过 order_fact_query 回答 VAS/时效/漏斗/用户问题（场景 A+C+D） |
| F3 | 商品维度销售分析 | 主流程 | L2 中分析 | AI 通过 order_fact_query 回答商品排名/关联问题（场景 B） |
| F4 | MCP 工具与 AI Skill 更新 | 支撑流程 | L1 轻分析 | 新增 order_fact_query 工具、更新 Ontology 与 Skill |

### Payload 完整性验证

> **决策 IO3-1**：现有事件 Payload 完全满足读模型所需，不需要扩展任何上游 BC 的事件发布逻辑。

关键信息来源：

| 读模型字段 | 信息来源 | Payload 中是否存在 |
|-----------|---------|-------------------|
| orderId, userId, totalAmountCents | OrderCreated | ✅ 已有 |
| items[]{skuId, spuId, quantity, unitPriceCents} | OrderCreated.items[] | ✅ 已有 |
| hasEngraving | EngravingCompleted 事件存在性 | ✅ 事件存在性推导 |
| hasWarranty | ServiceActivated 事件存在性 | ✅ 事件存在性推导 |
| currentStage | 从事件集合推导 | ✅ 事件集合推导 |
| 各环节时间戳 | 各事件的 occurredAt | ✅ 已有 |
| 产品名称 | AI 查询时通过 catalog_query 获取 | ✅ 查询时关联 |

## 二、场景分析

### F1：订单事实投影（L2）

OrderFact 和 OrderItemFact 的构建机制。这是所有分析场景的数据基础。

#### 投影触发时机与流程

每当一条新事件写入 BusinessActivity 时，检查是否有 orderId。若有，触发该 orderId 的投影更新：

1. 查出该 orderId 的**所有已有事件**（从 BusinessActivity 表）
2. 按规则从事件集合中**推导** OrderFact 的各字段
3. **UPSERT** 到 order_fact 表（orderId 已存在则更新，否则插入）
4. 若事件类型为 OrderCreated，**解析 payload 中的 items[]**，为每个 item 生成 OrderItemFact 行（仅首次，后续不重复）
5. 若 OrderFact 的 hasEngraving / hasWarranty / currentStage 发生变化，**级联更新** OrderItemFact 的冗余字段

> **决策 IO3-2**：投影在事件写入时同步执行（事务内），不引入异步机制。Activity BC 的事件量级适合同步投影，无需额外的消息队列或定时任务。

#### 投影触发的三种场景

| 场景 | 触发方式 | 说明 |
|------|---------|------|
| **实时 Kafka 事件** | `record()` 内自动触发 | 每个新事件写入后，同步投影该 orderId |
| **种子数据生成** | 生成完成后调用批量重建 API | SeedDataGenerator 直接调 `repository.save()` 绕过 `record()`，需在生成后显式触发重建 |
| **历史数据回填** | 一次性调用批量重建 API | 部署 OrderFact 功能后，对已有 BusinessActivity 执行全量重建 |

批量重建 API：`POST /api/order-facts/rebuild`（可选参数 `seedBatch` 限定范围）。从 BusinessActivity 表取所有 distinct orderId，逐个执行投影。

#### OrderFact 推导规则

从事件集合推导各字段：

| 字段 | 推导规则 |
|------|---------|
| orderId, userId, totalAmountCents | 取自 OrderCreated 事件的 payload |
| itemCount | OrderCreated.items[].length |
| totalQuantity | sum(OrderCreated.items[].quantity) |
| hasEngraving | 事件集合中存在 EngravingCompleted → true |
| hasWarranty | 事件集合中存在 ServiceActivated → true |
| currentStage | 按优先级：CANCELLED > COMPLETED > DELIVERED > SHIPPED > FULFILLING > PAID > CREATED（取事件集合中最高阶段） |
| cancelReason | 若 CANCELLED：事件集合中有 PaymentExpired → TIMEOUT，否则 → MANUAL |
| isAbnormal | 事件集合中存在 PaymentFailed 或 PaymentExpired → true |
| createdAt | OrderCreated.occurredAt |
| paidAt | PaymentCompleted.occurredAt（可空） |
| shippedAt | FulfillmentShipped.occurredAt（可空） |
| deliveredAt | FulfillmentDelivered.occurredAt（可空） |
| completedAt | OrderCompleted.occurredAt（可空） |
| cancelledAt | OrderCancelled.occurredAt（可空） |
| paymentDurationSec | paidAt - createdAt（秒，可空） |
| fulfillmentDurationSec | deliveredAt - paidAt（秒，可空） |
| createdDate | createdAt 的日期部分 |
| createdHour | createdAt 的小时部分（0-23） |

currentStage 推导逻辑——事件类型到阶段的映射：

| 事件类型 | 对应阶段 | 优先级（高→低） |
|---------|---------|---------------|
| OrderCancelled | CANCELLED | 最高（终态） |
| OrderCompleted | COMPLETED | 次高（终态） |
| FulfillmentDelivered | DELIVERED | 3 |
| FulfillmentShipped | SHIPPED | 4 |
| FulfillmentOrderCreated / Allocated | FULFILLING | 5 |
| PaymentCompleted | PAID | 6 |
| OrderCreated | CREATED | 最低（初始态） |

#### OrderItemFact 生成规则

仅在处理 OrderCreated 事件时生成，每个 item 一行：

| 字段 | 来源 |
|------|------|
| orderId, userId | OrderCreated |
| skuId, spuId, quantity, unitPriceCents | items[] 中对应元素 |
| lineTotalCents | quantity × unitPriceCents |
| orderTotalAmountCents | OrderCreated.totalAmountCents |
| orderCurrentStage | 从 OrderFact 冗余 |
| orderHasEngraving, orderHasWarranty | 从 OrderFact 冗余 |
| createdDate | OrderCreated.occurredAt 的日期部分 |

### F2：订单维度运营分析（L2）

覆盖场景 A1–A9、C1–C4、D1–D3。AI 通过 `order_fact_query` 工具查询 OrderFact 的聚合统计。

#### 数据流向

```
运营人员提问 → AI（智能运营助手）
  → order_fact_query { action: "stats", period: "last7" }
  → MCP Server → Activity API: GET /api/order-facts/stats?period=last7
  → Activity BC 从 order_fact 表聚合查询
  → 返回结构化统计 → AI 调用 ops_canvas 渲染 → 画布展示 + 文字分析
```

#### stats API 返回结构

```json
{
  "totalOrders": 100,
  "completedOrders": 72,
  "cancelledOrders": 12,
  "inProgressOrders": 16,
  "cancelByTimeout": 8,
  "cancelByManual": 4,
  "ordersWithEngraving": 11,
  "ordersWithWarranty": 9,
  "ordersWithAnyVas": 17,
  "multiItemOrders": 25,
  "totalRevenueCents": 25880000,
  "avgOrderAmountCents": 258800,
  "avgVasOrderAmountCents": 328500,
  "avgNonVasOrderAmountCents": 243200,
  "avgPaymentDurationSec": 720,
  "avgFulfillmentDurationSec": 259200,
  "distinctBuyerCount": 42,
  "repeatBuyerCount": 18,
  "from": "2026-03-08",
  "to": "2026-03-14"
}
```

#### stats_daily API 返回结构

```json
[
  {
    "date": "2026-03-10",
    "totalOrders": 15,
    "completedOrders": 12,
    "cancelledOrders": 1,
    "vasOrders": 3,
    "engravingOrders": 2,
    "warrantyOrders": 1,
    "totalRevenueCents": 3882000,
    "avgAmountCents": 258800
  }
]
```

#### 场景 → API 映射

| 场景 | API 调用 | 从返回数据中提取 |
|------|---------|----------------|
| A1 VAS 渗透率 | stats | ordersWithAnyVas / totalOrders |
| A2 VAS 客单价 | stats | avgVasOrderAmountCents vs avgNonVasOrderAmountCents |
| A3 订单状态分布 | stats | completedOrders / cancelledOrders / inProgressOrders |
| A4 支付时效 | stats | avgPaymentDurationSec |
| A5 履约时效 | stats | avgFulfillmentDurationSec |
| A6 取消原因 | stats | cancelByTimeout / cancelByManual |
| A7 多件订单 | stats | multiItemOrders / totalOrders |
| A8 漏斗分析 | stats | totalOrders → completedOrders（各阶段转化） |
| A9 VAS 组合 | stats | ordersWithEngraving + ordersWithWarranty - ordersWithAnyVas（交集） |
| C1 复购率 | stats | repeatBuyerCount / distinctBuyerCount |
| C4 新用户 | stats_daily | 按日统计的新用户（首次下单） |
| D3 VAS 趋势 | stats_daily | vasOrders / totalOrders 按日 |

### F3：商品维度销售分析（L2）

覆盖场景 B1–B6。AI 通过 `order_fact_query` 工具的 `product_ranking` action 查询 OrderItemFact 的聚合统计。

#### 数据流向

```
运营人员提问 → AI（智能运营助手）
  → order_fact_query { action: "product_ranking", rankBy: "revenue", period: "last7" }
  → MCP Server → Activity API: GET /api/order-facts/product-ranking?rankBy=revenue&period=last7
  → Activity BC 从 order_item_fact 表 GROUP BY spuId 聚合
  → 返回排名 → AI 可选调 catalog_query 获取产品名称 → ops_canvas 渲染
```

#### product_ranking API 返回结构

```json
{
  "rankBy": "revenue",
  "items": [
    { "spuId": 3, "totalQuantity": 45, "totalRevenueCents": 4491000, "orderCount": 32, "cancelledOrderCount": 3 },
    { "spuId": 1, "totalQuantity": 38, "totalRevenueCents": 3762200, "orderCount": 28, "cancelledOrderCount": 1 }
  ],
  "from": "2026-03-08",
  "to": "2026-03-14"
}
```

#### 产品名称解决策略

> **决策 IO3-3**：产品名称通过 AI 的两步调用解决——先查排名得到 spuId，再调 `catalog_query` 获取名称。不在 OrderItemFact 中快照产品名称，避免跨 BC 耦合和数据过时。

#### 场景 → API 映射

| 场景 | API 调用 | 说明 |
|------|---------|------|
| B1 销量排行 | product_ranking, rankBy=quantity | 按总件数排名 |
| B2 销售额排行 | product_ranking, rankBy=revenue | 按总金额排名 |
| B3 商品趋势 | stats_daily + spuId 筛选 | 按日统计某商品 |
| B4 商品取消率 | product_ranking | cancelledOrderCount / orderCount |
| B5 VAS-商品关联 | product_ranking, hasEngraving=true | 含镭雕订单中的商品分布 |
| B6 SKU 热度 | product_ranking, groupBy=sku | 按 skuId 排名 |

### F4：MCP 工具与 AI Skill 更新（L1）

新增 `order_fact_query` MCP 工具，更新 Ontology 文档，更新「智能运营助手」Skill。

- **MCP 工具**：在 `hmall-mcp/tools/` 新增 `order-fact.js`，注册 `order_fact_query` 工具
- **allowedTools**：智能运营助手 Skill 添加 `order_fact_query`
- **MCP Resource**：`hmall://intelligent-ops/domain-knowledge` 自动加载更新后的 Ontology
- **System Prompt**：增加 order_fact_query 的使用指引和示例（VAS 分析、商品排名等）

### 流程间耦合

- **F1（投影）→ F2/F3（查询）**：F2/F3 完全依赖 F1 产出的 OrderFact/OrderItemFact。投影必须先于查询实现。
- **F4（MCP）→ F2/F3（AI 可用）**：MCP 工具注册后，AI 才能调用分析 API。
- **与 Step 1/2 的关系**：本需求不修改 `activity_query`（现有事件级查询工具），`order_fact_query` 与之并行存在。`activity_query` 回答"发生了什么"，`order_fact_query` 回答"整体状况如何"。

## 三、变更分析

### Activity BC（🔴 重大，🔲 全新：OrderFact/OrderItemFact 读模型 + 投影 + 分析 API）

#### 领域模型变更

新增两个读模型（非聚合根，是 CQRS 投影产物）：

**OrderFact**（订单事实，每个订单一行）：

| 属性 | 类型 | 约束 | 说明 |
|------|------|------|------|
| orderId | Long | PK | 订单 ID |
| userId | Long | 可空 | 用户 ID |
| totalAmountCents | long | | 订单总金额（分） |
| itemCount | int | | 商品种类数 |
| totalQuantity | int | | 商品总件数 |
| hasEngraving | boolean | 默认 false | 是否含镭雕（EngravingCompleted 事件存在） |
| hasWarranty | boolean | 默认 false | 是否含延保（ServiceActivated 事件存在） |
| currentStage | String | 非空 | CREATED / PAID / FULFILLING / SHIPPED / DELIVERED / COMPLETED / CANCELLED |
| cancelReason | String | 可空 | TIMEOUT / MANUAL（仅 CANCELLED 时有值） |
| isAbnormal | boolean | 默认 false | 流程中是否出现异常（PaymentFailed/Expired） |
| createdAt | Instant | 非空 | 下单时间 |
| paidAt | Instant | 可空 | 支付时间 |
| shippedAt | Instant | 可空 | 发货时间 |
| deliveredAt | Instant | 可空 | 签收时间 |
| completedAt | Instant | 可空 | 完成时间 |
| cancelledAt | Instant | 可空 | 取消时间 |
| paymentDurationSec | Long | 可空 | 下单到支付耗时（秒） |
| fulfillmentDurationSec | Long | 可空 | 支付到签收耗时（秒） |
| createdDate | LocalDate | 非空 | 用于按日聚合 |
| createdHour | int | 0-23 | 用于时段分布 |
| seedBatch | String | 可空 | 种子数据批次标记 |

**OrderItemFact**（订单商品明细事实，每个订单行项目一行）：

| 属性 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, 自增 | 主键 |
| orderId | Long | 非空 | 关联订单 |
| userId | Long | 可空 | 用户（冗余） |
| skuId | Long | 非空 | SKU ID |
| spuId | Long | 可空 | SPU ID |
| quantity | int | 非空 | 购买数量 |
| unitPriceCents | long | 非空 | 单价（分） |
| lineTotalCents | long | 非空 | 行金额（分） |
| orderTotalAmountCents | long | | 订单总额（冗余） |
| orderCurrentStage | String | | 订单当前阶段（冗余） |
| orderHasEngraving | boolean | | 订单是否含镭雕（冗余） |
| orderHasWarranty | boolean | | 订单是否含延保（冗余） |
| createdDate | LocalDate | 非空 | 用于按日聚合 |
| seedBatch | String | 可空 | 种子数据批次标记 |

新增领域服务：

**OrderFactProjection**（投影服务）：
- `projectOrder(orderId)` → 查出该 orderId 的所有 BusinessActivity，推导 OrderFact 并 UPSERT；仅 OrderCreated 时生成 OrderItemFact
- 在 `ActivityApplicationService.record()` 成功后调用

#### 事件流变更

不新增或修改任何领域事件。

新增 API 端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/order-facts/stats` | 订单事实聚合统计。参数：period / from+to / hasEngraving / hasWarranty |
| GET | `/api/order-facts/stats/daily` | 按日订单事实统计。参数同上 |
| GET | `/api/order-facts/product-ranking` | 商品销售排名。参数：rankBy(quantity/revenue/orderCount) / period / from+to / hasEngraving / hasWarranty / limit |
| GET | `/api/order-facts` | 订单事实列表。参数：hasEngraving / hasWarranty / currentStage / userId / spuId / period / from+to / limit |
| POST | `/api/order-facts/rebuild` | 批量重建投影。可选参数：seedBatch（限定范围）。用于历史数据回填和种子数据生成后触发 |

#### 需求场景变更

新增 feature：`order-fact.feature`

- 🔲 新增 5.1：事件写入后 OrderFact 自动投影（OrderCreated → 初始 OrderFact）
- 🔲 新增 5.2：后续事件更新 OrderFact（PaymentCompleted → currentStage 变为 PAID，paidAt 填充）
- 🔲 新增 5.3：VAS 事件推导（EngravingCompleted → hasEngraving = true）
- 🔲 新增 5.4：取消订单推导（OrderCancelled → CANCELLED + cancelReason）
- 🔲 新增 5.5：OrderItemFact 生成（OrderCreated 的 items[] 展开为多行）
- 🔲 新增 5.6：stats 查询返回正确的聚合指标（VAS 渗透率、客单价等）
- 🔲 新增 5.7：stats_daily 查询返回按日统计
- 🔲 新增 5.8：product_ranking 查询返回商品排名
- 🔲 新增 5.9：列表查询支持多维筛选（hasEngraving / currentStage 等）
- 🔲 新增 5.10：种子数据生成后 OrderFact/OrderItemFact 自动投影

### hmall-mcp（🟡 中等，🔲 全新：order_fact_query 工具 + Ontology 更新）

#### MCP 工具变更

新增 `hmall-mcp/tools/order-fact.js`：

```javascript
server.tool('order_fact_query', '订单事实分析：以订单为中心的多维分析视图', {
  action: z.enum(['stats', 'stats_daily', 'product_ranking', 'list']),
  hasEngraving: z.boolean().optional(),
  hasWarranty: z.boolean().optional(),
  currentStage: z.string().optional(),
  spuId: z.number().optional(),
  userId: z.number().optional(),
  rankBy: z.enum(['quantity', 'revenue', 'orderCount']).optional(),
  period: z.string().optional(),
  from: z.string().optional(),
  to: z.string().optional(),
  limit: z.number().optional(),
})
```

#### Ontology 更新

在 `docs/intelligent-ops-ontology.md` 新增第七章「订单事实分析（Order Fact）」：

- OrderFact 概念说明
- 可分析维度表（VAS / 阶段 / 时效 / 用户 / 商品）
- 健康指标（VAS 渗透率阈值、VAS 客单价提升比等）
- order_fact_query 工具使用指南

### Smart Interaction（🟢 轻微，🔄 需调整：Skill 配置更新）

#### Skill 变更

更新「智能运营助手」Skill（通过 API 更新）：

- **allowedTools**：添加 `order_fact_query`（变为 `activity_query, ops_canvas, order_query, order_fact_query`）
- **System Prompt**：新增 `order_fact_query` 使用指引：
  - 何时用 `activity_query` vs `order_fact_query`（事件级 vs 订单级）
  - VAS 分析示例
  - 商品排名 + 名称查询示例（两步调用）
  - 新增健康指标（VAS 渗透率、VAS 客单价提升）

### 其他 BC（⚪ 无变更）

Order、Catalog、Payment、Inventory、Fulfillment、User、Cart、BFF 均无变更。

### BC 间数据流

```
Activity BC BusinessActivity（已有）
  ↓ 投影（同步，事务内）
Activity BC OrderFact + OrderItemFact（新增）
  ↓ API
hmall-mcp order_fact_query（新增）
  ↓ MCP Tool Calling
Smart Interaction → AI 智能运营助手
  ↓ （AI 可选：产品名称补充）
hmall-mcp catalog_query（已有）→ Catalog API
```

## 四、迭代计划

### 迭代 0：OrderFact 领域模型与投影（Activity BC） ✅ 已完成

**涉及 BC**：Activity
**前置依赖**：无

**后端**：
- OrderFact / OrderItemFact 领域模型（record）
- OrderFactRepository 接口与 JPA 实现（Entity + JpaRepository）
- OrderFactProjection 领域服务（事件集合 → OrderFact 推导 + UPSERT）
- ActivityApplicationService.record() 增加投影触发
- 验收场景 5.1–5.5（投影正确性）

**前端**：无

**验收**：
- 写入 OrderCreated 事件后，order_fact 表生成对应记录，currentStage=CREATED
- 写入 PaymentCompleted 后，同一 orderId 的 OrderFact 更新为 PAID，paidAt 有值
- 写入 EngravingCompleted 后，hasEngraving=true
- 写入 OrderCancelled 后，currentStage=CANCELLED，cancelReason 正确
- OrderCreated 的 items[] 展开为 OrderItemFact 多行，lineTotalCents 正确
- `mvn test -q` 全绿

### 迭代 1：分析 API + MCP 工具（Activity BC + hmall-mcp） ✅ 已完成

**涉及 BC**：Activity, hmall-mcp
**前置依赖**：迭代 0

**后端**：
- OrderFactController：stats / stats_daily / product-ranking / list 四个端点
- OrderFactStatsDto / DailyOrderFactStatsDto / ProductRankingDto
- Repository 聚合查询方法
- 验收场景 5.6–5.9

**MCP**：
- `hmall-mcp/tools/order-fact.js`：`order_fact_query` 工具注册
- `hmall-mcp/server.js`：引入并注册

**验收**：
- `GET /api/order-facts/stats?period=last7` 返回正确的 VAS 渗透率、客单价等指标
- `GET /api/order-facts/product-ranking?rankBy=revenue` 返回按销售额排名的商品列表
- MCP 工具 `order_fact_query` 可被发现和调用
- `mvn test -q` 全绿；`node scripts/verify-mcp-local.mjs` 通过

### 迭代 2：Ontology + AI Skill + 种子数据适配 ✅ 已完成

**涉及 BC**：hmall-mcp（Ontology）, Smart Interaction（Skill）, Activity（种子数据）
**前置依赖**：迭代 1

**后端**：
- SeedDataGenerator 生成数据后触发投影（验收场景 5.10）
- 或：提供 API 触发全量重建投影

**文档/配置**：
- `docs/intelligent-ops-ontology.md` 新增第七章
- 更新「智能运营助手」Skill（allowedTools + System Prompt）
- 通过 `/api/ai/skills` API 更新线上 Skill

**验收**：
- 生成种子数据后，OrderFact/OrderItemFact 正确投影
- AI 智能运营助手可以回答 VAS 相关问题（调用 order_fact_query）
- AI 可以回答商品排名问题（order_fact_query + catalog_query 两步调用）

**E2E 验收**：`BIZ-IO3-001` — 生成测试数据 → 在智能运营页面提问"含增值服务的订单占比"→ AI 调用 order_fact_query → 画布展示 VAS 渗透率卡片

---

### 一致性检查

| 维度 | 检查项 | 结果 |
|------|--------|------|
| 场景完整 | A1–A9, B1–B6, C1–C4, D1–D3 共 22 个用户场景，全部有 F1–F3 覆盖 | ✅ |
| 场景↔变更 | F1 投影 → Activity 领域模型变更；F2/F3 查询 → Activity API 变更 + MCP 变更；F4 Skill → Smart Interaction 变更 | ✅ |
| 数据可达 | OrderFact 全部字段可从 BusinessActivity 事件集合推导，无 ❌ 缺口 | ✅ |
| Payload 一致 | 不修改任何事件 Payload，仅消费已有字段 | ✅ |
| 变更内部 | 验收场景 5.1–5.10 覆盖所有模型字段推导和 API 返回 | ✅ |
| 前端 | 无前端变更（画布已支持所有视图类型） | ✅ 不适用 |
| 运营配置 | 迭代 2 包含 Skill 更新工作项 | ✅ |

---

## 交付跟踪

### 迭代 0：OrderFact 领域模型与投影 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Activity: OrderFact/OrderItemFact 域对象 + 持久化 + 投影逻辑 + 验收场景 5.1–5.5 | evolve-feature | — | ✅ 完成 |
| 2 | Activity: rebuild API + 种子数据投影 + 验收场景 5.10 | evolve-feature | #1 | ✅ 完成 |

**交付日期**：2026-03-14

### 迭代 1：分析 API + MCP 工具 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 3 | Activity: stats/stats_daily/product-ranking/list API + 验收场景 5.6–5.9 | evolve-feature | 迭代 0 | ✅ 完成 |
| 4 | hmall-mcp: order_fact_query 工具注册 | evolve-feature | #3 | ✅ 完成 |

**交付日期**：2026-03-14

### 迭代 2：Ontology + AI Skill + E2E ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 5 | Ontology: intelligent-ops-ontology.md 第七章 | — | 迭代 1 | ✅ 完成 |
| 6 | Smart Interaction: 智能运营助手 Skill 更新 | — | #5 | ✅ 完成 |
| 7 | E2E 交付门禁 (BIZ-IO3-001) | deliver-requirement | #1–#6 | ✅ 完成 |

**交付日期**：2026-03-14
**整理内容**：清理未使用的 `updateItemsCascade`/`existsItemsByOrderId` 方法、移除 Step Definitions 中未使用的 `ObjectMapper` 依赖。

本需求已全部交付。
