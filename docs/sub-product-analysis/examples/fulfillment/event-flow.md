# Fulfillment 限界上下文 - 事件流

集成方式、事件契约、API 契约。领域结构见 [domain-model.md](./domain-model.md)，需求列表见 [requirements.md](./requirements.md)。

---

## 一、参与方

| BC | 关系 | 集成方式 |
|----|------|----------|
| Order | 上游：调用创建/取消履约单 | **同步调用**（REST） |
| Fulfillment | 当前 BC | — |
| Order | 下游：订阅 Allocated / Shipped / Delivered / 🔲 ServiceActivated | **Kafka 事件** |
| Activity | 下游：订阅全部事件 | **Kafka 事件** |

---

## 二、流程

### 主流程

```
Order 同步调用 createFulfillment(orderId, items, shippingAddress)
  → Fulfillment 拆单、创建 FulfillmentOrder（MVP: 1:1）
  → 返回 fulfillmentOrderIds（Order 保持 PAID，不置 FULFILLING）
  → 发布 FulfillmentOrderCreated 事件（Activity 消费）

管理后台/内部操作：开始配货
  → 调用 POST /api/fulfillment/{fulfillmentOrderId}/allocate
  → FulfillmentOrder 状态 CREATED → ALLOCATING
  → 发布 FulfillmentOrderAllocated 事件（Order 消费后置 FULFILLING，Activity 消费）

管理后台/内部操作：发货
  → FulfillmentOrder 状态 ALLOCATING → SHIPPED
  → 发布 FulfillmentShipped 事件（Order + Activity 消费）

物流签收确认
  → FulfillmentOrder 状态 SHIPPED → DELIVERED
  → 发布 FulfillmentDelivered 事件（Order + Activity 消费）

🔲 虚拟服务履约单（来自业务需求 虚拟商品）：
  → 创建时按 itemType 拆单：SERVICE items（非镭雕）→ VIRTUAL 履约单
  → 虚拟履约单创建后自动激活（CREATED → ACTIVATED）
  → 发布 ServiceActivated 事件（Order + Activity 消费）

🔲 镭雕（来自业务需求 [镭雕服务](../../business-requirements/laser-engraving/overview.md)）：
  → 镭雕 SERVICE items 不拆成 VIRTUAL，engravingInfo 合并到关联的实体履约单
  → 管理后台调用 POST /api/fulfillment/{id}/complete-engraving 标记镭雕完成
  → 有 engravingInfo 时须 completeEngraving 后才能 ship
  → 可选发布 EngravingCompleted 事件（Activity 消费）
```

### 取消（补偿路径）

```
Order 同步调用 cancelFulfillment(orderId)
  → Fulfillment 取消该 orderId 下所有 CREATED、ALLOCATING 状态的履约单
  → 已 SHIPPED / DELIVERED 的履约单不受影响（不可取消）
```

### 流程图

```plantuml
@startuml fulfillment-flow
skinparam componentStyle rectangle
left to right direction
component "Order" as Order
component "Fulfillment" as Fulfillment
component "Activity" as Activity

Order --> Fulfillment : 1. createFulfillment（同步）
Order <-- Fulfillment : 返回 fulfillmentOrderIds
Fulfillment ..> Activity : 2. FulfillmentOrderCreated
Fulfillment ..> Order : 2a. FulfillmentOrderAllocated（开始配货后）
Fulfillment ..> Activity : 2a. FulfillmentOrderAllocated
Fulfillment ..> Order : 3. FulfillmentShipped
Fulfillment ..> Activity : 3. FulfillmentShipped
Fulfillment ..> Order : 4. FulfillmentDelivered
Fulfillment ..> Activity : 4. FulfillmentDelivered
Order --> Fulfillment : cancelFulfillment（同步，补偿）
@enduml
```

---

## 三、同步调用契约（REST API）

### 创建履约单

```
POST /api/fulfillment/create
```

请求体：

```json
{
  "orderId": 12345,
  "items": [
    { "skuId": 101, "quantity": 2, "itemType": "PHYSICAL" },
    { "skuId": 301, "quantity": 1, "itemType": "SERVICE", "relatedSkuId": 101, "serviceAttributes": { "engravingPatternId": 1, "engravingPatternName": "心形", "engravingText": "张三" } }
  ],
  "shippingAddress": {
    "recipientName": "何勉",
    "phone": "13641793760",
    "province": "上海",
    "city": "上海",
    "district": "浦东新区",
    "detail": "羽山路100弄9号2902"
  }
}
```

成功响应（200）：

```json
{
  "orderId": 12345,
  "fulfillmentOrderIds": [1001]
}
```

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 创建成功 | 200 | 返回 fulfillmentOrderIds |
| 幂等：orderId 已存在 | 200 | 返回已有 fulfillmentOrderIds |
| 缺失 orderId、items 为空或 shippingAddress 缺失 | 400 | 参数校验失败 |

### 取消履约单

```
POST /api/fulfillment/cancel
```

请求体：

```json
{
  "orderId": 12345
}
```

成功响应（200）：

```json
{
  "orderId": 12345,
  "cancelledCount": 1
}
```

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 取消成功 | 200 | 返回取消的履约单数量 |
| 无可取消的履约单 | 200 | cancelledCount = 0（幂等） |
| 缺失 orderId | 400 | 参数校验失败 |

### 开始配货

```
POST /api/fulfillment/{fulfillmentOrderId}/allocate
```

无请求体。

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 开始配货成功 | 200 | 状态 CREATED → ALLOCATING |
| 非 CREATED 状态 | 400 | 状态不允许开始配货 |
| 履约单不存在 | 404 | — |

### 完成镭雕

```
POST /api/fulfillment/{fulfillmentOrderId}/complete-engraving
```

> 以下变更来自业务需求 [镭雕服务](../../business-requirements/laser-engraving/overview.md)

无请求体。仅当 engravingInfo 非空且 engravingCompletedAt 未设时可用。

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 完成成功 | 200 | 设 engravingCompletedAt，可选发布 EngravingCompleted |
| engravingInfo 为空 | 400 | 该履约单无镭雕内容 |
| engravingCompletedAt 已设 | 400 | 镭雕已完成，重复操作 |
| 履约单不存在 | 404 | — |

### 发货

```
POST /api/fulfillment/{fulfillmentOrderId}/ship
```

请求体：

```json
{
  "carrier": "顺丰",
  "trackingNumber": "SF1234567890"
}
```

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 发货成功 | 200 | 状态 ALLOCATING → SHIPPED |
| 非 ALLOCATING 状态（含 CREATED） | 400 | 状态不允许发货 |
| 有 engravingInfo 且 engravingCompletedAt 未设 | 400 | 须先完成镭雕 |
| 履约单不存在 | 404 | — |

### 签收确认

```
POST /api/fulfillment/{fulfillmentOrderId}/deliver
```

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 签收成功 | 200 | 状态 → DELIVERED |
| 非 SHIPPED 状态 | 400 | 状态不允许签收 |
| 履约单不存在 | 404 | — |

### 查询

```
GET /api/fulfillment/{fulfillmentOrderId}
GET /api/fulfillment?orderId={orderId}&status={status}
```

| 参数 | 说明 |
|------|------|
| orderId | 可选。不传则返回全部履约单（管理端列表）；传则仅返回该订单的履约单。 |
| status | 可选。CREATED / ALLOCATING / SHIPPED / DELIVERED / CANCELLED，过滤状态。 |

---

## 四、事件契约

### Fulfillment 发布

| 事件 | 时机 | Topic | 订阅方 | 关键 Payload |
|------|------|-------|--------|-------------|
| FulfillmentOrderCreated | 创建履约单成功 | `fulfillment.order.created` | Activity | orderId, fulfillmentOrderIds, occurredAt |
| FulfillmentOrderAllocated | 开始配货成功 | `fulfillment.order.allocated` | Order, Activity | orderId, fulfillmentOrderId, occurredAt |
| FulfillmentShipped | 发货成功 | `fulfillment.shipped` | Order, Activity | orderId, fulfillmentOrderId, occurredAt |
| FulfillmentDelivered | 签收确认 | `fulfillment.delivered` | Order, Activity | orderId, fulfillmentOrderId, occurredAt |
| 🔲 ServiceActivated | 虚拟服务激活 | `fulfillment.service.activated` | Order, Activity | orderId, fulfillmentOrderId, serviceSkuId, activatedAt, expiresAt, occurredAt |
| 🔲 EngravingCompleted | 镭雕已完成 | `fulfillment.engraving.completed` | Activity | orderId, fulfillmentOrderId, completedAt, occurredAt |

**注意**：FulfillmentOrderCreated 事件仅 Activity 消费。Order 在同步创建履约单后保持 PAID；收到 FulfillmentOrderAllocated 后置 FULFILLING。🔲 ServiceActivated 对 Order 等效 FulfillmentDelivered。🔲 EngravingCompleted 供 Activity 订单旅程展示，Order 不消费。

### 事件消息体

**FulfillmentOrderCreated**：

```json
{
  "eventType": "FulfillmentOrderCreated",
  "eventId": "uuid",
  "orderId": 12345,
  "fulfillmentOrderIds": [1001],
  "occurredAt": "2026-02-20T10:00:00Z"
}
```

**FulfillmentOrderAllocated**：

```json
{
  "eventType": "FulfillmentOrderAllocated",
  "eventId": "uuid",
  "orderId": 12345,
  "fulfillmentOrderId": 1001,
  "occurredAt": "2026-02-20T10:05:00Z"
}
```

**FulfillmentShipped**：

```json
{
  "eventType": "FulfillmentShipped",
  "eventId": "uuid",
  "orderId": 12345,
  "fulfillmentOrderId": 1001,
  "occurredAt": "2026-02-20T12:00:00Z"
}
```

**FulfillmentDelivered**：

```json
{
  "eventType": "FulfillmentDelivered",
  "eventId": "uuid",
  "orderId": 12345,
  "fulfillmentOrderId": 1001,
  "occurredAt": "2026-02-20T14:00:00Z"
}
```

**🔲 ServiceActivated**：

```json
{
  "eventType": "ServiceActivated",
  "eventId": "uuid",
  "orderId": 12345,
  "fulfillmentOrderId": 1002,
  "serviceSkuId": 301,
  "activatedAt": "2026-02-20T10:00:05Z",
  "expiresAt": "2027-02-20T10:00:05Z",
  "occurredAt": "2026-02-20T10:00:05Z"
}
```

**🔲 EngravingCompleted**：

```json
{
  "eventType": "EngravingCompleted",
  "eventId": "uuid",
  "orderId": 12345,
  "fulfillmentOrderId": 1001,
  "completedAt": "2026-02-20T11:00:00Z",
  "occurredAt": "2026-02-20T11:00:00Z"
}
```

### 事件通用约定

| 约定 | 说明 |
|------|------|
| 关联键 | orderId |
| 幂等键 | eventId（UUID），Fulfillment 生成 |
| 消息格式 | JSON |
| 传输 | Kafka；Topic 命名 `fulfillment.<event-type>` |
| 消费语义 | at-least-once，消费方保证幂等 |
