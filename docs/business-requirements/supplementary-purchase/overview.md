# 业务需求（Business Requirement）：保障服务补购（Supplementary Service Purchase）

## 一、背景与目标

用户购买实体商品并签收后，可以事后单独补购该商品关联的虚拟服务（碎屏险、延保等）。

核心场景：用户查看已交付的订单 → 发现可补购的服务 → 下单 → 支付 → 服务即时激活。

**前置依赖**：[虚拟商品](../virtual-product/overview.md)（Catalog ServiceBinding + 交易流程服务支持 + 虚拟履约）。

**与随购的区别**：

| 维度 | 随购（虚拟商品迭代 2） | 补购（本需求） |
|------|----------------------|--------------|
| 时机 | 购买实体商品时同时勾选 | 实体商品已交付后单独购买 |
| 订单结构 | 混合订单（实体 + 服务） | 纯服务订单 |
| 入口 | 商品详情页 / 购物车 | 订单详情页（已交付订单） |
| 库存 | 实体占库存，服务跳过 | 全部跳过 |
| 收货地址 | 需要（有实体配送） | 不需要（纯服务） |

---

## 二、业务场景与事件流

### 场景 B：补购（事后单独购买关联服务）

```
用户查看已交付订单（DELIVERED / COMPLETED）
  → ⌘ QueryPurchasableServices（orderId）
  → [Order] 提取订单中 PHYSICAL items
       → 调用 Catalog 查各实体 SPU 的可选服务
       → 排除用户已补购的服务
       → 返回可补购服务列表
  → 用户选择服务
  → ⌘ PlaceOrder（纯 SERVICE items，relatedSkuId 指向实体 SKU）
  → [Order] 校验：relatedSkuId 在用户已交付订单中；未重复补购
  → [Order] 纯 SERVICE items → 跳过 Inventory
  → [Order] 纯服务订单 → ShippingAddress 可选
  → [Order] 同步调用 Payment 创建支付单
  → 🟧 OrderCreated [Order]
  → 用户支付 → 网关回调
  → 🟧 PaymentCompleted [Payment]
  → [Order] 同步调用 Fulfillment 创建履约单
  → [Fulfillment] 纯虚拟履约单 → 立即激活
  → 🟧 ServiceActivated [Fulfillment]
  → [Order] 仅虚拟 Activated → 满足完成条件
  → 🟧 OrderCompleted [Order]
```

### 补偿路径

与现有一致：

- **支付超时**：PaymentExpired → OrderCancelled（无库存释放，无已激活服务）
- **用户取消（PENDING_PAYMENT）**：OrderCancelled

### 新增事件

无。完全复用现有事件（OrderCreated、PaymentCompleted、ServiceActivated、OrderCompleted）。

### 复用分析

| 能力 | 来源 | 状态 |
|------|------|------|
| ServiceBinding 查询 | Catalog BC | ✅ 可复用 |
| 纯服务订单（跳过库存） | Order BC（虚拟商品 1.7/1.8） | 🔲 需先实现 |
| 虚拟履约（CREATED→ACTIVATED） | Fulfillment BC（虚拟商品 1.5-1.7） | 🔲 需先实现 |
| ServiceActivated → OrderCompleted | Order BC（虚拟商品 4.6） | 🔲 需先实现 |
| 补购查询 API | Order BC | 🔲 全新 |
| 补购前置校验 | Order BC | 🔲 全新 |

---

## 三、设计决策

| # | 决策 | 说明 |
|---|------|------|
| SP1 | 补购入口在订单详情页 | 已交付订单（DELIVERED/COMPLETED）的实体商品旁显示可补购服务；最自然的用户路径 |
| SP2 | 补购不走购物车，直接下单 | 补购是明确意图，无需"暂存"；降低复杂度 |
| SP3 | 实体商品必须已交付 | DELIVERED 或 COMPLETED 状态才能补购；服务（如碎屏险）应在商品到手后才有意义 |
| SP4 | 不允许重复补购 | 同一 relatedSkuId（实体 SKU）的同一服务 SKU 不允许重复购买 |
| SP5 | Order BC 提供聚合查询 API | `GET /api/orders/{orderId}/purchasable-services`；Order 内部调用 Catalog 获取可选服务并去重已购；前端一个调用搞定 |
| SP6 | 纯服务订单 ShippingAddress 可选 | 补购订单无物理配送，收货地址非必填；Order 不变式需按订单类型放宽（产出阶段发现） |

---

## 四、各 BC 影响摘要

| BC | 影响程度 | 核心变更 |
|----|----------|---------|
| **Order** | 🟡 中等 | 新增可补购服务查询 API（聚合 Catalog 数据 + 去重已购）；补购订单创建校验（前置条件 + 重复检查）；纯服务订单 ShippingAddress 可选 |
| **Catalog** | ⚪ 无变更 | 已有 `available-services` 查询 API |
| **Fulfillment** | ⚪ 无变更 | 纯虚拟履约已支持（前提：虚拟商品的 1.5-1.7 已实现） |
| **Cart** | ⚪ 无变更 | 补购不走购物车 |
| **Payment** | ⚪ 无变更 | |
| **Inventory** | ⚪ 无变更 | |
| **Activity** | ⚪ 无变更 | 现有事件订阅自动覆盖补购订单 |

> 各 BC 的具体特性变更、模型变更、集成变更见对应 BC 文档（requirements.md、domain-model.md、event-flow.md）。

### BC 间关联

- Order → Catalog（REST）：查询实体 SPU 的可选服务（复用已有集成方向）
- 补购订单 → Fulfillment → Order：复用纯虚拟履约链路（ServiceActivated → OrderCompleted）
- 补购订单的 `OrderLineItem.relatedSkuId` → 原订单的实体 SKU（跨订单语义引用）

---

## 五、迭代计划

### 迭代 1：补购服务全链路

**涉及 BC**：Order
**前置依赖**：虚拟商品迭代 1+2（Catalog ServiceBinding + 纯服务订单支持 + 虚拟履约）
**状态**：✅ 已完成

**后端变更**：

- Order：新增 `GET /api/orders/{orderId}/purchasable-services` 聚合查询 API
  - 提取订单中 PHYSICAL items → 按 skuId 调用 Catalog 查可选服务 → 排除用户已补购的服务 → 返回
- Order：新增 `CatalogServiceQueryPort` 出站端口（查询实体 SPU 的可选服务列表）
- Order：补购订单创建校验——relatedSkuId 在用户已交付订单中 + 不允许重复补购
- Order：纯服务订单 ShippingAddress 可选（放宽不变式，按 items 是否全为 SERVICE 判断）
- Order：OrderLineItem 快照 spuId（用于补购查询时 SKU → SPU 映射）

**前端变更**（后端完成后集成）：

- `frontend/web` 订单详情页：已交付订单（DELIVERED/COMPLETED）显示"可补购服务"区域
  - 调用 `purchasable-services` API
  - 展示可补购服务列表（服务名、规格、价格）
  - 每项服务提供"补购"按钮 → 创建补购订单 → 跳转支付
- `frontend/web` 补购完成后：订单详情页不再显示已补购的服务

**验收标准**：用户购买手机并签收 → 订单详情页显示可补购碎屏险 → 点击补购 → 创建纯服务订单（无需填写收货地址）→ 支付 → 碎屏险激活 → 再次查看订单详情页不再显示该服务 → 查看补购订单可见碎屏险明细与激活状态。
