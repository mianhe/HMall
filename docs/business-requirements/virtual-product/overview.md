# Epic：虚拟商品（Virtual Product）

## 一、背景与目标

支持在电商平台售卖虚拟服务类商品（碎屏险、延保、维修服务等），覆盖两个核心场景：

1. **随购**：购买实体商品时勾选附带的服务（如买手机时加购碎屏险）
2. **补购**：针对过去已购买的实体商品，补购相关服务（如为已有手机补购延保）

服务的核心定义：
- **种类可扩展**：碎屏险、维修服务、延长保修、镭雕、上门回收等，将来可新增
- **三种定价模式**：①独立售卖（无 binding，用 SKU 标准价）②限定适用范围但统一价格（binding + priceCents=null，继承 SKU 标准价）③上下文定价（binding + priceCents≠null，按目标商品差异定价）
- **可独立或关联实体产品**：部分服务（如上门回收）可独立售卖；部分服务（如镭雕、维修保障）需关联实体商品

---

## 二、业务场景与事件流

### 场景 A：随购（购买实体商品 + 服务）

```
用户选择 手机(实体SKU) + 碎屏险(服务SKU)
  → ⌘ PlaceOrder（含实体 items + 服务 items）
  → [Order] 同步调用 Inventory 占用（仅实体 items，服务 items 跳过）
  → 🟧 StockReserved [Inventory]（仅实体商品）
  → [Order] 同步调用 Payment 创建支付单（总金额 = 实体 + 服务）
  → 🟧 OrderCreated [Order]
  → 用户支付 → 网关回调
  → 🟧 PaymentCompleted [Payment]
  → [Order] 同步调用 Fulfillment 创建履约单
  → [Fulfillment] 按商品类型拆单：实体履约单 + 虚拟服务履约单
  → 🟧 FulfillmentOrderCreated [Fulfillment]（含两个履约单 ID）

  实体履约单走现有流程：
  → 🟧 FulfillmentOrderAllocated → 🟧 FulfillmentShipped → 🟧 FulfillmentDelivered

  虚拟服务履约单：
  → 创建后即激活（CREATED → ACTIVATED）
  → 🟧 ServiceActivated [Fulfillment]（新事件，等效 Delivered）

  → [Order] 按「最慢原则」：实体 Delivered + 虚拟 Activated 全部到达
  → 🟧 OrderCompleted [Order]
```

### 场景 B：补购（为已购产品补购服务）

```
用户从已签收订单中选择补购延保服务
  → ⌘ PlaceOrder（仅服务 items，携带 relatedOrderId + relatedSkuId）
  → [Order] 校验原订单存在且实体商品已签收
  → [Order] 跳过 Inventory 占用（纯服务订单，无实体商品）
  → [Order] 同步调用 Payment 创建支付单
  → 🟧 OrderCreated [Order]
  → 用户支付 → 🟧 PaymentCompleted [Payment]
  → [Order] 同步调用 Fulfillment 创建履约单
  → [Fulfillment] 仅创建虚拟服务履约单 → 立即激活
  → 🟧 ServiceActivated [Fulfillment]
  → 🟧 OrderCompleted [Order]
```

### 补偿路径

与现有补偿路径一致，差异点：

- **取消含服务的订单**：释放实体库存（服务无库存占用）；取消服务履约单（CREATED 可取消；ACTIVATED 不可取消，MVP 暂不支持退保）
- **支付超时**：正常补偿链，服务无额外处理

### 新增事件

| 事件 | BC | Topic | 订阅方 | Payload |
|------|-----|-------|--------|---------|
| ServiceActivated | Fulfillment | `fulfillment.service.activated` | Order, Activity | orderId, fulfillmentOrderId, serviceSkuId, activatedAt, expiresAt, occurredAt |

---

## 三、设计决策

| # | 决策 | 说明 |
|---|------|------|
| VP1 | 服务是独立的 SERVICE 类型 SPU | 复用 SPU-SKU 模型，通过 productType 区分；服务期限为 SpecDimension（如"服务期限"→"一年"/"两年"），SKU 按规格组合生成；ServiceBinding 绑定 serviceSkuId + targetSpuId，priceCents 可选（null 时继承 SKU 标准价，非 null 时为上下文定价）；无 binding 的 SERVICE SKU 可独立售卖 |
| VP2 | 服务类商品不占库存 | Order 创建时过滤 SERVICE items，仅对 PHYSICAL items 调用 Inventory。MVP 不支持服务限量售卖 |
| VP3 | 混合订单 + Fulfillment 拆单 | 一次下单生成一个 Order（含实体+服务 items），Fulfillment 按 itemType 拆为实体履约单 + 虚拟履约单。符合已有决策 F1（拆单由 Fulfillment 负责） |
| VP4 | ServiceActivated 等效 Delivered | 虚拟履约单激活后视为交付完成。Order 用最慢原则——实体 Delivered + 虚拟 Activated 全到达才 OrderCompleted |
| VP5 | 补购是独立新订单 | 补购生成新 Order（仅含 SERVICE items），通过 relatedOrderId / relatedSkuId 关联原购买。不修改已完成的原订单 |
| VP6 | 已激活服务 MVP 不可取消 | 虚拟履约单 CREATED 可取消；ACTIVATED 后不可取消（退保是未来能力） |
| VP7 | 先做随购，后做补购 | 随购涵盖的模型变更更基础，补购在此基础上仅增加关联校验 |

---

## 四、各 BC 影响摘要

| BC | 影响程度 | 核心变更 |
|----|----------|---------|
| **Catalog** | 🔴 重大 | SPU 新增 productType；服务期限通过 SpecDimension + SpecOption 表达；新增 ServiceBinding 实体（SKU 级绑定 + 上下文定价）；新增服务查询 API |
| **Order** | 🔴 重大 | OrderLineItem 新增 itemType / relatedSkuId / relatedOrderId；条件性库存占用；消费 ServiceActivated；补购校验 |
| **Fulfillment** | 🔴 重大 | 按类型拆单；虚拟履约单 CREATED→ACTIVATED 生命周期；ServiceActivated 事件；FulfillmentItem 新增服务属性 |
| **Cart** | 🟡 中等 | 展示关联服务；CartItem 标记关联的实体 SKU |
| **Activity** | 🟢 轻微 | 消费 ServiceActivated 事件 |
| **Inventory** | ⚪ 无变更 | Order 端过滤服务 items |
| **Payment** | ⚪ 无变更 | 总金额含服务费用即可 |

### BC 间关联

- Catalog `Spu.productType` → Order `OrderLineItem.itemType`（创建订单时从 Catalog 带入）
- Catalog `ServiceBinding`（SKU 级 + 上下文定价）→ Cart / 前端（查询关联服务用于展示和勾选）
- Order items 中的 `itemType` → Fulfillment 拆单依据（PHYSICAL → 实体履约单，SERVICE → 虚拟履约单）
- Fulfillment `ServiceActivated` → Order 状态推进（等效 FulfillmentDelivered）
- Fulfillment `ServiceActivated` → Activity 事件时间线

---

## 五、迭代计划

### 迭代 1：Catalog 支持服务类商品 ✅ 已完成

**涉及 BC**：Catalog
**前置依赖**：无
**状态**：✅ 已完成（5 feature, 63 scenario 全绿；后端 + 前端 admin/web + MCP + AI Skill 全部交付）

**后端变更**：
- SPU 新增 productType（PHYSICAL / SERVICE）
- 服务期限通过 SpecDimension + SpecOption 表达（如"服务期限"→"一年期"/"两年期"），与实体商品规格统一
- 新增 ServiceBinding 实体（serviceSkuId + targetSpuId + priceCents(nullable)，SKU 级绑定，支持上下文定价或继承 SKU 标准价）
- 新增 API：管理 ServiceBinding（POST/DELETE `/api/skus/{skuId}/service-bindings`）、查询实体 SPU 的可选服务列表
- 服务 SPU 下可创建 SKU，SKU.priceCents 为标准售价；需关联实体商品的服务通过 ServiceBinding 限定适用范围和可选的价格覆盖

**前端变更**（后端完成后集成）：
- `frontend/admin` CatalogPage：商品节点展示 productType 标签（SERVICE 类型需可辨识）
- `frontend/admin` ProductDetailPage：SERVICE 类型 SPU 展示/管理 ServiceBinding（已关联的实体 SPU + 售价列表）
- `frontend/web` 商品详情页：展示实体商品的可选服务列表（调用 AvailableService API），含服务名称、SKU 规格值（如期限）、binding 售价

**验收标准**：后台可创建碎屏险服务 SPU（期限为 SpecDimension）→ 创建 SKU → ServiceBinding 关联到手机 SPU 并定价 → 查询手机时可返回可选服务列表（含 SKU 规格和 binding 售价）→ 管理后台可展示服务类商品 → 消费者端商品详情页可展示可选服务。

### 迭代 2：交易流程支持随购服务

**涉及 BC**：Order、Fulfillment、Cart、Activity
**前置依赖**：迭代 1

**后端变更**：
- Order：OrderLineItem 新增 itemType；创建订单时条件性库存占用；消费 ServiceActivated
- Fulfillment：按 itemType 拆单；虚拟履约单 CREATED→ACTIVATED；发布 ServiceActivated
- Cart：展示可选服务、CartItem 关联
- Activity：消费 ServiceActivated

**前端变更**（后端完成后集成）：
- `frontend/web` 商品详情页：可选服务支持勾选，「立即购买」时携带服务 items
- `frontend/web` 购物车页：服务项与关联的实体商品分组展示；结算预览含服务费用
- `frontend/web` 结账页：混合订单摘要（实体 + 服务分组，总价含服务）
- `frontend/web` 订单详情页：展示服务类明细的 itemType 和激活状态
- `frontend/admin` FulfillmentPage：展示虚拟履约单（fulfillmentType=VIRTUAL）；ACTIVATED 状态列；虚拟单无需发货操作
- `frontend/admin` OrderJourneyPage：ServiceActivated 事件展示（Fulfillment 泳道，正向事件样式）

**验收标准**：购买手机 + 碎屏险 → 下单 → 支付 → 实体走物流、碎屏险立即激活 → 全部完成后 OrderCompleted → 前端全链路可操作可查看。

### 迭代 3：补购服务

**涉及 BC**：Order、前端
**前置依赖**：迭代 2

**后端变更**：
- Order：OrderLineItem 新增 relatedOrderId；补购创建逻辑（校验原订单已签收）

**前端变更**（后端完成后集成）：
- `frontend/web` 订单详情页：已签收订单展示「购买服务」入口（查询可关联的服务列表）
- `frontend/web` 补购下单流程：选择服务 → 结账 → 支付 → 服务激活

**验收标准**：查看已签收手机订单 → 选择补购延保 → 下单 → 支付 → 服务激活 → 前端可完成完整补购流程。
