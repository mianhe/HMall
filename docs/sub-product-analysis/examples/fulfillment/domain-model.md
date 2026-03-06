# Fulfillment 限界上下文 - 领域模型

聚合、实体、值对象、领域事件。事件流转与集成方式见 [event-flow.md](./event-flow.md)，需求列表见 [requirements.md](./requirements.md)。

---

## 一、职责说明

Fulfillment 负责订单的履约执行：接收 Order 的创建请求后，按业务规则拆单，管理每个履约单的生命周期，通过 Kafka 事件通知 Order 推进状态。实体履约单：CREATED → ALLOCATING → SHIPPED → DELIVERED；🔲 虚拟服务履约单：CREATED → ACTIVATED（来自业务需求 [虚拟商品](../../business-requirements/virtual-product/overview.md)）。🔲 镭雕：雕刻内容附属实体履约单，须 completeEngraving 后才能 ship（来自业务需求 [镭雕服务](../../business-requirements/laser-engraving/overview.md)）。

---

## 二、模型图

![Fulfillment 限界上下文 - 领域模型](domain-model.png)

<details>
<summary>PlantUML 源码（点击展开）</summary>

```plantuml
@startuml fulfillment-domain
skinparam classAttributeIconSize 0
skinparam linetype ortho
left to right direction

title Fulfillment 限界上下文 - 领域模型

class FulfillmentOrder <<聚合根>> {
  - fulfillmentOrderId: Long
  - orderId: Long
  - fulfillmentType: FulfillmentType {PHYSICAL|VIRTUAL}
  - status: FulfillmentOrderStatus
  - shippingAddress: ShippingAddress
  - shippingInfo: ShippingInfo
  - engravingInfo: EngravingInfo {0..1}
  - engravingCompletedAt: Instant {0..1}
  - createdAt: Instant
  - updatedAt: Instant
  --
  不变式: orderId 必填, items 非空, fulfillmentType 必填
  engravingInfo 非空时 ship() 要求 engravingCompletedAt 已设
  --
  + allocate(): void {仅 PHYSICAL}
  + ship(carrier, trackingNumber): void {仅 PHYSICAL}
  + completeEngraving(): void {仅 PHYSICAL 且 engravingInfo 非空}
  + confirmDelivery(): void {仅 PHYSICAL}
  + activate(): void {仅 VIRTUAL}
  + cancel(): void
}

class EngravingInfo <<值对象>> {
  - patternId: Long {0..1}
  - patternName: String {0..1}
  - text: String {0..1}
  --
  不变式: 至少 patternId 或 text 其一非空
}

enum FulfillmentType {
  PHYSICAL
  VIRTUAL
}

class FulfillmentItem <<实体>> {
  - fulfillmentItemId: Long
  - skuId: Long
  - quantity: Integer
  - itemType: ItemType {PHYSICAL|SERVICE}
  --
  不变式: skuId 必填, quantity > 0
}

class ShippingAddress <<值对象>> {
  - recipientName: String
  - phone: String
  - province: String
  - city: String
  - district: String
  - detail: String
}

class ShippingInfo <<值对象>> {
  - carrier: String
  - trackingNumber: String
  - shippedAt: Instant
  - deliveredAt: Instant
}

enum FulfillmentOrderStatus {
  CREATED
  ALLOCATING
  SHIPPED
  DELIVERED
  ACTIVATED
  CANCELLED
}

class FulfillmentOrderCreated <<领域事件>> {
  orderId: Long
  fulfillmentOrderIds: List<Long>
  occurredAt: Instant
}

class FulfillmentOrderAllocated <<领域事件>> {
  orderId: Long
  fulfillmentOrderId: Long
  occurredAt: Instant
}

class FulfillmentShipped <<领域事件>> {
  orderId: Long
  fulfillmentOrderId: Long
  occurredAt: Instant
}

class FulfillmentDelivered <<领域事件>> {
  orderId: Long
  fulfillmentOrderId: Long
  occurredAt: Instant
}

class ServiceActivated <<领域事件>> {
  orderId: Long
  fulfillmentOrderId: Long
  serviceSkuId: Long
  activatedAt: Instant
  expiresAt: Instant
  occurredAt: Instant
}

FulfillmentOrder "1" *-- "1..*" FulfillmentItem : items
FulfillmentOrder "1" *-- "0..1" EngravingInfo : engravingInfo
FulfillmentOrder ..> FulfillmentType : fulfillmentType
FulfillmentOrder "1" *-- "1" ShippingAddress
FulfillmentOrder "1" *-- "0..1" ShippingInfo
FulfillmentOrder ..> FulfillmentOrderStatus : status
FulfillmentOrder ..> FulfillmentOrderCreated : 创建成功时发布
FulfillmentOrder ..> FulfillmentOrderAllocated : allocate 成功时发布
FulfillmentOrder ..> FulfillmentShipped : ship 成功时发布
FulfillmentOrder ..> FulfillmentDelivered : confirmDelivery 成功时发布
FulfillmentOrder ..> ServiceActivated : activate 成功时发布（VIRTUAL）

note right of FulfillmentOrder
  一笔 orderId 可对应 1~N 个 FulfillmentOrder
  PHYSICAL：CREATED → ALLOCATING → SHIPPED → DELIVERED
  VIRTUAL：CREATED → ACTIVATED（创建后即激活）
end note

@enduml
```

</details>

---

## 三、实体与属性

### FulfillmentOrder — 聚合根

| 属性 | 类型 | 说明 |
|------|------|------|
| fulfillmentOrderId | Long | 唯一标识 |
| orderId | Long | 关联的订单 ID，引用 Order BC |
| fulfillmentType | FulfillmentType | 🔲 PHYSICAL / VIRTUAL |
| status | FulfillmentOrderStatus | 履约单状态 |
| items | List\<FulfillmentItem\> | 履约商品明细 |
| shippingAddress | ShippingAddress | 配送地址（PHYSICAL 必填，VIRTUAL 可选） |
| shippingInfo | ShippingInfo | 物流信息（仅 PHYSICAL，发货后填充） |
| engravingInfo | EngravingInfo | 🔲 镭雕内容（可选），来自 Order SERVICE items 的 serviceAttributes 合并 |
| engravingCompletedAt | Instant | 🔲 镭雕完成时间（可选），非空表示雕刻已完成 |
| createdAt | Instant | 创建时间 |
| updatedAt | Instant | 更新时间 |

**FulfillmentOrderStatus**：CREATED | ALLOCATING | SHIPPED | DELIVERED | 🔲 ACTIVATED | CANCELLED

**不变式**：orderId 必填；items 非空且每项 quantity > 0；fulfillmentType 必填；PHYSICAL 类型 shippingAddress 必填。🔲 engravingInfo 非空时，ship() 要求 engravingCompletedAt 已设。

> 🔲 新增属性/状态来自业务需求 [虚拟商品](../../business-requirements/virtual-product/overview.md)、[镭雕服务](../../business-requirements/laser-engraving/overview.md)

**领域行为**：
- `allocate()`：仅 PHYSICAL，CREATED → ALLOCATING（开始配货）
- `completeEngraving()`：🔲 仅 PHYSICAL 且 engravingInfo 非空，设 engravingCompletedAt，可选发布 EngravingCompleted 事件
- `ship(carrier, trackingNumber)`：仅 PHYSICAL，ALLOCATING → SHIPPED；🔲 门禁：engravingInfo 非空时须 engravingCompletedAt 已设
- `confirmDelivery()`：仅 PHYSICAL，SHIPPED → DELIVERED，记录 deliveredAt
- 🔲 `activate()`：仅 VIRTUAL，CREATED → ACTIVATED，发布 ServiceActivated
- `cancel()`：CREATED 或 ALLOCATING → CANCELLED（已发货/已激活不可取消）

### FulfillmentItem — 实体

| 属性 | 类型 | 说明 |
|------|------|------|
| fulfillmentItemId | Long | 唯一标识 |
| skuId | Long | SKU ID，引用 Catalog |
| quantity | Integer | 数量，> 0 |
| itemType | ItemType | 🔲 PHYSICAL / SERVICE，创建时从 Order items 带入 |

**不变式**：skuId 必填；quantity > 0；itemType 必填。

### ShippingAddress — 值对象

创建履约单时由 Order 传入的配送地址快照。履约单创建后地址不再变更。

| 属性 | 类型 | 说明 |
|------|------|------|
| recipientName | String | 收件人 |
| phone | String | 电话 |
| province | String | 省 |
| city | String | 市 |
| district | String | 区 |
| detail | String | 详细地址 |

**不变式**：收件人、电话、省市区、详细地址必填。

### ShippingInfo — 值对象

物流信息，发货时填充。

| 属性 | 类型 | 说明 |
|------|------|------|
| carrier | String | 承运商（如"顺丰"、"中通"） |
| trackingNumber | String | 物流单号 |
| shippedAt | Instant | 发货时间 |
| deliveredAt | Instant | 签收时间（签收后填充） |

### EngravingInfo — 值对象（🔲 镭雕）

> 以下变更来自业务需求 [镭雕服务](../../business-requirements/laser-engraving/overview.md)

镭雕内容，附属在实体履约单上。创建时从 Order 的 SERVICE items（relatedSkuId + serviceAttributes）合并。

| 属性 | 类型 | 说明 |
|------|------|------|
| patternId | Long | 图案 ID，可选 |
| patternName | String | 图案名称快照，展示用 |
| text | String | 雕刻文字，≤20 字，可选 |

**不变式**：至少 patternId 或 text 其一非空。

---

## 四、状态流转

### PHYSICAL 履约单

```mermaid
stateDiagram-v2
    [*] --> CREATED: 创建履约单
    CREATED --> ALLOCATING: allocate（开始配货）
    CREATED --> CANCELLED: cancel（取消）
    ALLOCATING --> SHIPPED: ship（发货）
    ALLOCATING --> CANCELLED: cancel（取消）
    SHIPPED --> DELIVERED: confirmDelivery（签收）
```

### 🔲 VIRTUAL 履约单（服务）

```mermaid
stateDiagram-v2
    [*] --> CREATED: 创建虚拟履约单
    CREATED --> ACTIVATED: activate（服务激活）
    CREATED --> CANCELLED: cancel（取消）
```

| 转换 | 适用类型 | 前置条件 | 动作 |
|------|---------|----------|------|
| → CREATED | 全部 | Order 同步调用创建 | 保存履约单（🔲 实体单可含 engravingInfo），发布 FulfillmentOrderCreated |
| CREATED/ALLOCATING → engravingCompletedAt 已设 | PHYSICAL | 🔲 管理端调用 completeEngraving（仅 engravingInfo 非空时） | 设 engravingCompletedAt，可选发布 EngravingCompleted |
| CREATED → ALLOCATING | PHYSICAL | 管理端/内部调用开始配货 | 发布 FulfillmentOrderAllocated |
| ALLOCATING → SHIPPED | PHYSICAL | 提供 carrier + trackingNumber；🔲 有 engravingInfo 时须 engravingCompletedAt 已设 | 填充 shippingInfo，发布 FulfillmentShipped |
| SHIPPED → DELIVERED | PHYSICAL | 物流签收确认 | 记录 deliveredAt，发布 FulfillmentDelivered |
| 🔲 CREATED → ACTIVATED | VIRTUAL | 创建后自动激活 | 发布 ServiceActivated |
| CREATED / ALLOCATING → CANCELLED | 全部 | Order 补偿调用 | 标记取消，不发布事件 |

---

## 五、领域事件

| 事件 | 时机 | 载荷 |
|------|------|------|
| FulfillmentOrderCreated（履约单已创建） | 创建履约单成功 | orderId, fulfillmentOrderIds, occurredAt |
| FulfillmentOrderAllocated（已开始配货） | allocate 成功 | orderId, fulfillmentOrderId, occurredAt |
| FulfillmentShipped（已发货） | ship 成功 | orderId, fulfillmentOrderId, occurredAt |
| FulfillmentDelivered（已签收） | confirmDelivery 成功 | orderId, fulfillmentOrderId, occurredAt |
| 🔲 ServiceActivated（虚拟服务已激活） | activate 成功 | orderId, fulfillmentOrderId, serviceSkuId, activatedAt, expiresAt, occurredAt |
| 🔲 EngravingCompleted（镭雕已完成） | completeEngraving 成功 | orderId, fulfillmentOrderId, completedAt, occurredAt |

事件的订阅方、Topic、传输通道、JSON 消息体等集成细节见 [event-flow.md](./event-flow.md)。

---

## 六、聚合边界

- **FulfillmentOrder** 为聚合根，FulfillmentItem 和 ShippingInfo 为其内部对象。
- 同一 orderId 可对应多个 FulfillmentOrder（1:N 拆单），但每个 FulfillmentOrder 独立管理生命周期。
- 创建时的幂等键为 orderId：同一 orderId 重复调用返回已有结果。

---

## 七、实体与表

| 模型 | 表名 |
|------|------|
| FulfillmentOrder | fulfillment_order |
| FulfillmentItem | fulfillment_item |

ShippingAddress 和 ShippingInfo 为值对象，均嵌入 fulfillment_order 表（recipient_name、phone、province、city、district、detail、carrier、tracking_number、shipped_at、delivered_at 列）。
