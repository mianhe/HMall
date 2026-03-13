# 智能运营 Step 1：多维事件基座

> 智能运营总体架构见 [business-process-architecture.md](../../business-process-architecture.md)。

## 一、需求概述与场景

### 需求概述

**业务目标**：让系统中的每个领域事件同时能被"哪笔订单"、"哪个用户"、"哪个商品（SPU/SKU）"三个角度查询和分析，为后续的用户发展、商品运营等智能化能力提供多维数据基座。

**需求类型**：扩展已有能力。在现有事件驱动架构上增强事件 payload 和事件消费侧的关联能力，不引入新的业务流程。

**与当前能力的对比**：

| 维度 | 当前 | Step 1 之后 |
|------|------|------------|
| 事件 payload | 以 orderId 为中心，部分事件缺少 userId/skuId/spuId/价格 | 所有关键事件自描述：携带 userId、items[{spuId, skuId, quantity, unitPriceCents}] |
| BusinessActivity 关联维度 | 仅 orderId | orderId + userId + correlationKeys（spuId、skuId） |
| Activity 查询 | 按 orderId 查询 | 按 orderId、userId、skuId、spuId 查询 |
| 事件元数据 | 按 BC 分类（boundedContext、label、category） | 增加流程归属（processRoles）和事件来源类型（origin） |

**后端影响面**：

| BC | 影响程度 | 变更内容 |
|----|---------|---------|
| Order | 🟡 中等 | 3 个出站事件 payload 增强（OrderCreated、OrderCompleted、OrderCancelled） |
| Activity | 🟡 中等 | BusinessActivity 模型扩展、消费侧多维提取、API 新增查询维度、EventMetadataRegistry 扩展 |
| Inventory | ⚪ 无变更 | StockReserved/Released 是 Saga 操作性事件，不承载业务分析价值，不增强 |
| Payment | ⚪ 无变更 | PaymentCompleted 已携带 amountCents，userId 可在消费侧通过 orderId 关联 |
| Fulfillment | ⚪ 无变更 | 事件 payload 已携带 orderId + fulfillmentOrderId，暂不增强 |
| Catalog | ⚪ 无变更 | — |

**前端影响面**：

| 前端 | 变更内容 |
|------|---------|
| frontend/admin | Activity 页面增加按 userId / skuId / spuId 查询的能力 |
| frontend/web | 无变更 |

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| S1 | 事件 payload 增强 | 主流程 | L2 中分析 | 增强 3 个 Order 事件的 Kafka 消息体，携带多流程所需字段 |
| S2 | Activity 多维关联 | 主流程 | L2 中分析 | BusinessActivity 新增 userId 列 + correlationKeys JSON 列，消费事件时提取 |
| S3 | Activity 多维查询 | 主流程 | L2 中分析 | Activity API 支持按 userId、skuId、spuId 查询事件序列 |
| S4 | EventMetadata 流程归属 | 支撑流程 | L1 轻分析 | EventMetadataRegistry 为每个事件标注所属一级业务流程和角色 |
| S5 | 前端多维查询 | 支撑流程 | L1 轻分析 | admin Activity 页面增加 userId/skuId/spuId 查询入口 |

---

## 二、场景分析（事件流）

### S1：事件 payload 增强（L2）

本场景无新的事件流，是在现有事件链上做 payload 扩展。以下列出 3 个事件的当前与目标 payload 对比。

#### 当前 vs 目标 payload

| 事件 | 当前 payload | 目标 payload（新增字段加粗） |
|------|-------------|---------------------------|
| OrderCreated | `{ eventType, orderId, items: [{ skuId, quantity }], occurredAt }` | `{ eventType, orderId,` **userId, totalAmountCents,** `items: [{skuId,` **spuId,** `quantity,` **unitPriceCents** `}], occurredAt }` |
| OrderCompleted | `{ eventType, orderId, occurredAt }` | `{ eventType, orderId,` **userId, totalAmountCents, items: [{ skuId, spuId, quantity, unitPriceCents }],** `occurredAt }` |
| OrderCancelled | `{ eventType, orderId, occurredAt }` | `{ eventType, orderId,` **userId, totalAmountCents, items: [{ skuId, spuId, quantity, unitPriceCents }],** `occurredAt }` |

> **决策 IO-S1-1**：OrderCompleted 和 OrderCancelled 携带完整 items 快照（含价格），而非仅 orderId。理由：这是"当时价格的快照"，消费方无需反查 Order 服务即可做商品维度的营收和退单分析。

#### 数据可达性

所有新增字段在事件发布点均可达，不需要新增模型字段：

| 新增字段 | 数据来源 | 发布点可达性 |
|---------|---------|------------|
| userId | `Order.userId` | ✅ placeOrder、cancelOrder、onFulfillmentDelivered 均加载了 Order 聚合 |
| totalAmountCents | `Order.totalAmountCents` | ✅ 同上 |
| spuId | `OrderLineItem.spuId` | ✅ 已实现，随 Order 聚合加载 |
| unitPriceCents | `OrderLineItem.unitPriceCents` | ✅ 已实现，随 Order 聚合加载 |

#### 兼容性

事件 payload 增加字段是**向后兼容**变更——现有消费方（Activity、其他未知消费者）会忽略新字段，不会破坏。无需版本号或多版本并行。

#### 变更层次

变更限于三层，域模型层不需要改：

| 层 | 文件 | 变更 |
|----|------|------|
| Application Event | `OrderCreatedEvent`、`OrderCompletedEvent`、`OrderCancelledEvent` | record 增加 userId、totalAmountCents、items 字段 |
| Kafka Message | `OrderCreatedMessage`、`OrderCompletedMessage`、`OrderCancelledMessage` | 增加 userId、totalAmountCents；items 使用 `ItemSnapshotPayload`（skuId, spuId, quantity, unitPriceCents）序列化 |
| Kafka Publisher | `KafkaOrderOutboundEventPublisher` | `from()` 方法传入新字段 |
| Application Service | `OrderApplicationService`、`OrderEventService` | 构造事件时传入 Order 聚合上的完整信息 |

---

### S2：Activity 多维关联（L2）

#### 当前状态

`BusinessActivity` 仅有 `orderId` 一个业务维度列。`RecordActivityCommand` 也只传 `orderId`。

#### 目标状态

| 新增列 | 类型 | 说明 |
|--------|------|------|
| userId | Long（可空） | 从事件 payload 的 `userId` 字段提取；无 userId 的事件（如 Inventory、Fulfillment）为 null |
| correlationKeys | String（JSON，可空） | 从事件 payload 的 `items` 字段提取 spuId/skuId 集合，结构：`{"spuIds":[1,2],"skuIds":[10,20]}` |

#### 提取逻辑

消费端（`ActivityKafkaEventConsumer`）在 `onEvent` 中从 message map 提取：

1. **userId**：`message.get("userId")` → Long，直传
2. **correlationKeys**：`message.get("items")` → 如果存在，提取所有 `spuId` 和 `skuId` 去重后序列化为 JSON

> **决策 IO-S2-1**：correlationKeys 使用 JSON 列而非多值关联表。理由：Step 1 阶段查询模式是"按某个 spuId/skuId 找关联事件"，用 JSON 列 + 数据库 JSON 函数（H2、MySQL 均支持）即可满足，避免多表 join 的复杂度。如果 Step 2+ 的查询模式演进到需要高性能聚合，可再提取为独立关联表。

#### 幂等键影响

当前 `deterministicEventId` 使用 `topic + eventType + orderId + occurredAt` 生成。增加 userId 后，orderId 仍是主要幂等因子，**不需要修改幂等策略**。

#### 变更层次

| 层 | 文件 | 变更 |
|----|------|------|
| Domain | `BusinessActivity` | 新增 `userId: Long`、`correlationKeys: String` 字段 |
| Application | `RecordActivityCommand` | 新增 `userId`、`correlationKeys` 参数 |
| Infrastructure (Consumer) | `ActivityKafkaEventConsumer` | `onEvent` 提取 userId 和 correlationKeys |
| Infrastructure (Persistence) | `BusinessActivityEntity` / DDL | 新增两列 + userId 索引 |

---

### S3：Activity 多维查询（L2）

#### 当前 API

- `GET /api/activities?orderId=...&limit=20`
- `GET /api/activities/recent?limit=20`

#### 新增查询参数

在现有 `GET /api/activities` 上新增可选查询参数：

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 按用户查询：查看某用户的所有关联事件（时间正序） |
| skuId | Long | 按 SKU 查询：查看某 SKU 关联的所有事件 |
| spuId | Long | 按 SPU 查询：查看某商品关联的所有事件 |

多参数同时传入时按 **orderId → userId → skuId → spuId** 优先级取第一个非空维度查询（单次请求只按一个维度）。userId 走数据库索引直查；skuId、spuId 走 correlationKeys JSON 列查询。

> **决策 IO-S3-1**：不新增独立的 `/api/activities/by-user`、`/api/activities/by-sku` 端点，而是在现有 `/api/activities` 上扩展查询参数。理由：Activity 的查询本质都是"按条件过滤事件列表"，统一入口更符合 RESTful 风格，也减少前端对接成本。

#### 统计 API 扩展

`GET /api/activities/stats` 同样可选传入 userId、spuId 做维度筛选（如"某用户的支付成功总额"、"某商品的订单完成数"），为 Step 2 的 Level 1 分析做好准备。此处仅预留 API 参数，实际统计逻辑可在 Step 2 实现。

---

### S4：EventMetadata 流程归属（L1）

在 `EventMetadata` record 上新增两个属性：

| 属性 | 类型 | 说明 |
|------|------|------|
| origin | `EventOrigin` 枚举（`DOMAIN` / `BEHAVIORAL` / `DERIVED`） | 事件来源类型。Step 1 所有事件均为 `DOMAIN`，`BEHAVIORAL` 和 `DERIVED` 为 Step 2-3 预留 |
| processRoles | `Map<String, String>` | 事件在各一级业务流程中的角色。key = 流程标识（如 `trading`、`user_development`、`product_ops`），value = 角色（`MILESTONE` / `PROGRESSION`） |

示例：

```
OrderCompleted:
  origin: DOMAIN
  processRoles: { "trading": "MILESTONE", "user_development": "PROGRESSION", "product_ops": "PROGRESSION" }

PaymentCompleted:
  origin: DOMAIN
  processRoles: { "trading": "PROGRESSION" }
```

> **决策 IO-S4-1**：origin 和 processRoles 先在 `EventMetadataRegistry` 硬编码，不做运行时动态注册。理由：一级流程和事件归属变化频率极低，硬编码足够且便于代码审查。

EventMetadata API（`GET /api/activities/event-metadata`）的 DTO 同步扩展，返回 origin 和 processRoles。

---

### S5：前端多维查询（L1）

admin Activity 页面（ActivityPage）在现有 orderId 查询输入框旁边增加 userId、skuId、spuId 三个可选查询输入框。查询时将非空参数拼入 `GET /api/activities` 的 query string。

事件列表展示不变（EventMetadata 的 origin / processRoles 在前端暂不展示，为 Step 2 的流程视图预留）。

---

### 查询影响

| 现有查询 | 影响 |
|---------|------|
| `GET /api/activities?orderId=...` | 无变更，向后兼容 |
| `GET /api/activities/recent` | 无变更 |
| `GET /api/activities/event-metadata` | DTO 新增 origin、processRoles 字段（向后兼容） |
| `GET /api/activities/stats` | 新增可选 userId、spuId 参数（向后兼容，不传则全量统计） |
| MCP `activity_query` 工具 | 考虑新增 userId 参数支持（可选，Step 1 不强制） |

### 流程间耦合

本需求无新增流程间耦合。变更方向是单向的：Order 事件 payload 增强（发布方）→ Activity 消费侧提取新字段（消费方）。不影响 Order 与 Inventory/Payment/Fulfillment 的编排逻辑。

---

## 三、变更分析

### Order（🟡 中等，🔄 需调整：3 个出站事件 payload）

#### 领域模型变更

无。`Order.userId`、`Order.totalAmountCents`、`OrderLineItem.spuId`、`OrderLineItem.unitPriceCents` 均已存在。

#### 事件流变更

**修改 3 个 Application Event record**：

- `OrderCreatedEvent`：当前 `(Long orderId, List<ItemQuantity> items)` 其中 `ItemQuantity(long skuId, int quantity)`
  - 🔄 → `(Long orderId, Long userId, Long totalAmountCents, List<ItemSnapshot> items)` 其中 `ItemSnapshot(long skuId, Long spuId, int quantity, long unitPriceCents)`

- `OrderCompletedEvent`：当前 `(Long orderId)`
  - 🔄 → `(Long orderId, Long userId, Long totalAmountCents, List<ItemSnapshot> items)`，复用 `OrderCreatedEvent.ItemSnapshot`（或提取为共享内部 record）

- `OrderCancelledEvent`：当前 `(Long orderId)`
  - 🔄 → `(Long orderId, Long userId, Long totalAmountCents, List<ItemSnapshot> items)`，同上

**修改 3 个 Kafka Message record**：

- `OrderCreatedMessage`：
  - 🔄 新增 `long userId, long totalAmountCents`
  - 🔄 `ItemQuantity` → `ItemSnapshot(long skuId, Long spuId, int quantity, long unitPriceCents)`
  - 🔄 `from()` 工厂方法接受新的 Event 参数

- `OrderCompletedMessage`：
  - 🔄 新增 `long userId, long totalAmountCents, List<ItemSnapshot> items`
  - 🔄 `from()` 工厂方法接受 Event 参数

- `OrderCancelledMessage`：同 OrderCompletedMessage

**修改 Kafka Publisher**：

- `KafkaOrderOutboundEventPublisher`：3 个 `publish()` 方法传入新 Event 字段至 `Message.from()`，无逻辑变更

**修改 Application Service**（事件构造点）：

- `OrderApplicationService.placeOrder()`：构造 `OrderCreatedEvent` 时传入 `saved.getUserId()`、`saved.getTotalAmountCents()`，items 映射增加 `spuId`、`unitPriceCents`
- `OrderApplicationService.cancelOrder()`：构造 `OrderCancelledEvent` 时传入 `order.getUserId()`、`order.getTotalAmountCents()`、order items 映射
- `OrderEventService.onFulfillmentDelivered()`：构造 `OrderCompletedEvent` 时传入 `order.getUserId()`、`order.getTotalAmountCents()`、order items 映射
- `OrderEventService.onServiceActivated()`：同上

**Kafka Topic 契约变更**（更新 `event-flow.md`）：

| Topic | 当前消息体 | 新增字段 |
|-------|----------|---------|
| `order.created` | eventType, orderId, items[{skuId, quantity}], occurredAt | **userId, totalAmountCents**, items 扩展为 [{skuId, **spuId**, quantity, **unitPriceCents**}] |
| `order.completed` | eventType, orderId, occurredAt | **userId, totalAmountCents, items[{skuId, spuId, quantity, unitPriceCents}]** |
| `order.cancelled` | eventType, orderId, occurredAt | **userId, totalAmountCents, items[{skuId, spuId, quantity, unitPriceCents}]** |

#### 需求场景变更

- 🔄 修改已有事件消费测试：验证 Kafka 消息体包含新字段
- 🔲 新增场景：OrderCompleted 消息包含 userId、totalAmountCents、items 快照
- 🔲 新增场景：OrderCancelled 消息包含 userId、totalAmountCents、items 快照

---

### Activity（🟡 中等，🔄 需调整：模型扩展 + 消费提取 + API 扩展 + 元数据扩展）

#### 领域模型变更

**BusinessActivity（聚合根）扩展**：

- 🔲 新增 `userId: Long`（可空）——从事件 payload 提取的用户 ID
- 🔲 新增 `correlationKeys: String`（可空）——JSON 格式的 spuId/skuId 集合，结构 `{"spuIds":[1,2],"skuIds":[10,20]}`

不变式更新：
- `userId` 可空（Inventory/Fulfillment 等事件无 userId）
- `correlationKeys` 可空（仅含 items 的事件才有值）

**EventMetadata（值对象）扩展**：

- 🔲 新增 `origin: EventOrigin`——枚举 `DOMAIN` / `BEHAVIORAL` / `DERIVED`
- 🔲 新增 `processRoles: Map<String, String>`——事件在各一级流程中的角色

**新增枚举 EventOrigin**：`DOMAIN`、`BEHAVIORAL`、`DERIVED`

**EventMetadataRegistry 更新**——所有现有事件标注 origin=DOMAIN，增加 processRoles：

| 事件 | processRoles |
|------|-------------|
| OrderCreated | `trading: MILESTONE` |
| OrderCancelled | `trading: MILESTONE` |
| OrderCompleted | `trading: MILESTONE, user_development: PROGRESSION, product_ops: PROGRESSION` |
| StockReserved | `trading: PROGRESSION` |
| StockReleased | `trading: PROGRESSION` |
| PaymentCompleted | `trading: MILESTONE` |
| PaymentFailed | `trading: PROGRESSION` |
| PaymentExpired | `trading: PROGRESSION` |
| FulfillmentOrderCreated | `trading: PROGRESSION` |
| FulfillmentOrderAllocated | `trading: PROGRESSION` |
| FulfillmentShipped | `trading: PROGRESSION` |
| FulfillmentDelivered | `trading: MILESTONE` |
| ServiceActivated | `trading: PROGRESSION` |
| EngravingCompleted | `trading: PROGRESSION` |

#### 事件流变更

**RecordActivityCommand 扩展**：
- 🔄 新增 `userId: Long`、`correlationKeys: String` 参数

**ActivityKafkaEventConsumer 消费逻辑扩展**：
- 🔄 `onEvent()` 中从 `message` 提取 `userId`（`toLong(message.get("userId"))`）
- 🔄 `onEvent()` 中从 `message.get("items")` 提取 spuId/skuId 集合，序列化为 correlationKeys JSON
- 🔄 传入 `RecordActivityCommand` 新增参数

**API 扩展**（更新 `api.yaml`）：

- `GET /api/activities`：
  - 🔄 新增可选参数 `userId: Long`
  - 🔲 新增可选参数 `skuId: Long`、`spuId: Long`
  - 现有 `orderId` 参数不变

- `GET /api/activities/stats`：
  - 🔲 新增可选参数 `userId: Long`、`spuId: Long`（Step 1 仅预留参数定义，实际筛选逻辑可延至 Step 2）

- `GET /api/activities/event-metadata`：
  - 🔄 `EventMetadataDto` 新增 `origin: String`、`processRoles: Map<String, String>` 字段

- `ActivityDto`：
  - 🔄 新增 `userId: Long`（nullable）字段

**ActivityRepository 扩展**：
- 🔲 新增 `findByUserId(Long userId, int limit): List<BusinessActivity>`
- 🔲 新增 `findByCorrelationKey(String keyName, Long keyValue, int limit): List<BusinessActivity>`（通过 JSON 函数查询 correlationKeys 中的 spuId/skuId）

**持久化层**：
- `BusinessActivityEntity`：新增 `userId`（Long，nullable）列、`correlationKeys`（TEXT，nullable）列
- DDL/schema：`userId` 列增加索引 `idx_business_activity_user_id`

#### 需求场景变更

- 🔲 新增：收到含 userId 的事件后，BusinessActivity 的 userId 字段正确填充
- 🔲 新增：收到含 items 的事件后，correlationKeys 正确提取 spuIds 和 skuIds
- 🔲 新增：按 userId 查询返回该用户关联的所有事件（时间正序）
- 🔲 新增：按 spuId 查询返回含该 SPU 的所有事件
- 🔲 新增：按 skuId 查询返回含该 SKU 的所有事件
- 🔄 修改 2.4：ActivityDto 新增 userId 字段
- 🔄 修改 2.5：EventMetadataDto 新增 origin、processRoles 字段

---

### Inventory / Payment / Fulfillment / Catalog（⚪ 无变更）

本需求不涉及这些 BC 的任何改动。

---

### BC 间数据流

```
Order [OrderCreated/Completed/Cancelled]
  → Kafka（userId, totalAmountCents, items[{skuId, spuId, quantity, unitPriceCents}]）
  → Activity [消费提取 userId + correlationKeys, 落库 BusinessActivity]
  → Activity API [多维查询：orderId / userId / skuId / spuId]
  → frontend/admin [Activity 页面多维查询]
```

数据流方向单一：Order（发布方）→ Activity（消费方）→ 前端（展示方）。不存在回调或反向依赖。

---

## 四、迭代计划

本需求拆为 3 个迭代，按依赖顺序执行。

### 迭代 0：Order 事件 payload 增强 ✅ 已完成

**涉及 BC**：Order
**前置依赖**：无

**后端**：
- `OrderCreatedEvent`、`OrderCompletedEvent`、`OrderCancelledEvent` 增加 userId、totalAmountCents、items（含 spuId、unitPriceCents）
- `OrderCreatedMessage`、`OrderCompletedMessage`、`OrderCancelledMessage` 同步扩展
- `KafkaOrderOutboundEventPublisher` 传参调整
- `OrderApplicationService`、`OrderEventService` 构造事件时传入完整数据

**前端**：无

**验收**：
- Order 现有测试全绿（payload 增强不破坏已有行为）
- OrderCreated Kafka 消息包含 userId、totalAmountCents、items[{skuId, spuId, quantity, unitPriceCents}]
- OrderCompleted Kafka 消息包含 userId、totalAmountCents、items 快照
- OrderCancelled Kafka 消息包含 userId、totalAmountCents、items 快照

### 迭代 1：Activity 多维关联与查询 ✅ 已完成

**涉及 BC**：Activity
**前置依赖**：迭代 0（事件 payload 含 userId 和 items 后，消费侧才有数据可提取）

**后端**：
- `BusinessActivity` 新增 userId、correlationKeys 字段
- `RecordActivityCommand` 新增参数
- `ActivityKafkaEventConsumer` 提取 userId、correlationKeys
- `BusinessActivityEntity` 新增两列 + userId 索引
- `ActivityRepository` 新增 findByUserId、findByCorrelationKey
- `ActivityController` `GET /api/activities` 新增 userId / skuId / spuId 可选参数
- `ActivityDto` 新增 userId 字段
- `EventMetadata` 新增 origin、processRoles；`EventMetadataRegistry` 补全所有事件的流程归属
- `EventMetadataDto` 新增 origin、processRoles 字段
- `api.yaml` 更新

**前端**：无

**验收**：
- Activity 现有测试全绿
- 收到 OrderCreated（含 userId）后 BusinessActivity.userId 正确落库
- 收到 OrderCompleted（含 items）后 correlationKeys 正确提取 spuIds/skuIds
- `GET /api/activities?userId=1` 返回该用户关联的事件列表
- `GET /api/activities?spuId=100` 返回含该 SPU 的事件列表
- `GET /api/activities/event-metadata` 返回 origin 和 processRoles

**E2E 验收**：`BIZ-IO1-001` 下单 → 支付 → 签收全流程后，Activity 按 orderId、userId、spuId 三种维度均可查到完整事件链

### 迭代 2：前端多维查询 ✅ 已完成

**涉及 BC**：frontend/admin
**前置依赖**：迭代 1（API 就绪后前端才能对接）

**后端**：无

**前端**：
- ActivityPage 新增 userId / skuId / spuId 查询输入框
- 查询参数拼入 `GET /api/activities` 请求

**验收**：
- 在 Activity 页面输入 userId 可查到该用户的事件
- 在 Activity 页面输入 spuId 可查到该商品关联的事件
- 现有 orderId 查询不受影响

---

## 一致性检查

| 维度 | 检查项 | 结果 |
|------|--------|------|
| 场景完整 | S1-S5 覆盖全部变更面？ | ✅ S1=事件 payload，S2=Activity 模型，S3=API 查询，S4=元数据，S5=前端 |
| 事件完整 | 3 个 Order 事件 payload 变更已明确？ | ✅ OrderCreated/Completed/Cancelled 的当前与目标 payload 对比已列出 |
| 数据可达 | 所有新增字段在发布点可达？ | ✅ 零缺口，4 个字段均来自已有 Order 聚合 |
| 场景↔变更 | 5 个场景在变更分析中均有对应 BC 规格？ | ✅ S1→Order 变更，S2/S3/S4→Activity 变更，S5→前端迭代 2 |
| 变更内部 | payload 与模型一致？ | ✅ 事件新增字段均可从现有 Order 域模型字段直接映射 |
| 前端 | 迭代计划标注了受影响页面？ | ✅ 迭代 2 标注 ActivityPage |
| 跨 BC 一致 | Order 发布的 payload 与 Activity 消费侧提取逻辑一致？ | ✅ userId 直提取，items→correlationKeys JSON |
| 兼容性 | 变更是否向后兼容？ | ✅ 事件增加字段、API 增加可选参数，均向后兼容 |

---

## 交付跟踪

由 `deliver-requirement` 在执行交付时维护。

### Step 1 交付工作项

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Order 事件 payload 增强 | evolve-feature | — | ✅ 完成 |
| 2 | Activity 多维关联与查询 | evolve-feature | #1 | ✅ 完成 |
| 3 | 前端多维查询 | frontend-development | #2 | ✅ 完成 |
| 4 | E2E 交付门禁 | deliver-requirement | #1–#3 | ✅ 完成 |

**交付日期**：2026-03-11  
**下一迭代**：本需求（Step 1）已全部交付。按当前智能运营路线，下一步为 **Step 2（Smart Interaction 接入）**：为 Activity 提供 MCP Tools + 流程知识 Resource，实现「用自然语言查事件」，使每步都有直观可见的结果；再接 Step 3（生命周期 + Level 1 分析）。见 [business-process-architecture.md](../../business-process-architecture.md) 与 [project-status.md](../../project-status.md)。

功能验证步骤见 [VERIFY.md](./VERIFY.md)。

---

## 六、修改总结与设计理由

### 6.1 修改清单（按 BC / 层）

| 层级 / BC | 文件或模块 | 变更要点 |
|-----------|------------|----------|
| **Order** | `application/event/ItemSnapshot.java` | 新增：行快照 record（skuId, spuId, quantity, unitPriceCents） |
| Order | `application/event/OrderCreatedEvent.java` | 增加 userId、totalAmountCents；items 改为 `List<ItemSnapshot>` |
| Order | `application/event/OrderCompletedEvent.java` | 增加 userId、totalAmountCents、items |
| Order | `application/event/OrderCancelledEvent.java` | 同上 |
| Order | `infrastructure/kafka/OrderCreatedMessage.java` | 增加 userId、totalAmountCents；items 为 `ItemSnapshotPayload` |
| Order | `infrastructure/kafka/OrderCompletedMessage.java` | 同上 |
| Order | `infrastructure/kafka/OrderCancelledMessage.java` | 同上 |
| Order | `infrastructure/kafka/KafkaOrderOutboundEventPublisher.java` | 三个 publish 调用 `Message.from(event)` 传完整事件 |
| Order | `application/OrderApplicationService.java` | placeOrder/cancelOrder 构造事件时传入 userId、totalAmountCents、ItemSnapshot 列表 |
| Order | `application/OrderEventService.java` | onFulfillmentDelivered/onServiceActivated 构造 OrderCompletedEvent 时传入同上 |
| **Activity** | `domain/BusinessActivity.java` | 增加 userId、correlationKeys 字段 |
| Activity | `domain/EventOrigin.java` | 新增枚举 DOMAIN / BEHAVIORAL / DERIVED |
| Activity | `domain/EventMetadata.java` | 增加 origin、processRoles |
| Activity | `domain/EventMetadataRegistry.java` | 所有事件注册 origin=DOMAIN 及 processRoles |
| Activity | `domain/ActivityRepository.java` | 新增 findByUserId、findByCorrelationKey |
| Activity | `application/RecordActivityCommand.java` | 增加 userId、correlationKeys |
| Activity | `application/ActivityApplicationService.java` | record 使用新参数；新增 listByUserId、listByCorrelationKey |
| Activity | `infrastructure/kafka/ActivityKafkaEventConsumer.java` | 从 message 提取 userId、extractCorrelationKeys(items) |
| Activity | `infrastructure/persistence/BusinessActivityEntity.java` | 增加 userId、correlationKeys 列及 userId 索引 |
| Activity | `infrastructure/persistence/BusinessActivityJpaRepository.java` | 新增 findByUserIdOrderByOccurredAtAsc、findByCorrelationKeyPatterns |
| Activity | `infrastructure/ActivityRepositoryImpl.java` | 实现 findByUserId、findByCorrelationKey（LIKE 模式查 JSON） |
| Activity | `api/ActivityController.java` | GET /api/activities 增加 userId/skuId/spuId 参数；优先级 orderId → userId → skuId → spuId |
| Activity | `api/dto/ActivityDto.java` | 增加 userId |
| Activity | `api/dto/EventMetadataDto.java` | 增加 origin、processRoles |
| **frontend/admin** | `shared/api/activity.js` | 新增 getActivities(params) 支持 orderId/userId/skuId/spuId |
| frontend/admin | `pages/ActivityPage.vue` | 多维查询区：4 个输入框 + 查询按钮 + 结果表格 |
| frontend/admin | `tests/business-e2e/specs/intelligent-ops-step1/activity-multi-query.spec.cjs` | BIZ-IO1-001：orderId 查询 → 结果或空态 |
| **文档** | Order/Activity BC 的 event-flow、domain-model、requirements、api.yaml | Phase B 落地；事件 payload、模型、API、需求场景同步 |
| 文档 | business-flows.md、project-status.md | 事件表、Step 1 状态、变更日志 |
| 文档 | VERIFY.md | 验证入口与步骤 |

### 6.2 设计理由（为什么这样改）

1. **事件 payload 一次带齐 userId、总金额、行快照**  
   消费方（Activity、未来分析）无需反查 Order 即可做用户维、商品维、金额维分析，满足多一级流程（交易、用户发展、商品运营）共用同一批事件。

2. **Order 域模型零改动**  
   userId、totalAmountCents、spuId、unitPriceCents 已在聚合内存在；只扩展“出站”的 Event/Message 与发布调用，避免侵入下单/取消/完成的核心逻辑。

3. **Kafka 消息用 ItemSnapshotPayload 而非直接暴露 ItemSnapshot**  
   基础设施层用独立 DTO 序列化，与领域 Event 的 ItemSnapshot 解耦，便于 JSON 字段命名与向后兼容。

4. **Activity 用 JSON 列存 correlationKeys**  
   Step 1 查询模式为“按某一 spuId/skuId 查事件”，LIKE 查 JSON 即可；若后续有高性能聚合需求，再拆为关联表。

5. **GET /api/activities 多参数优先级**  
   单次请求只按一个维度查，避免多维度 AND 语义与实现复杂度；优先级固定后行为可预期，前端可据此做简单联动。

6. **EventMetadata 增加 origin、processRoles**  
   为 Step 2/3（派生事件、流程视图）预留；Step 1 仅标注 DOMAIN 与一级流程角色，便于后续 MCP/报表按流程过滤。

7. **Inventory 事件不增强**  
   StockReserved/StockReleased 属于 Saga 操作性事件，不承载业务分析价值，保持原样以减少改动面。