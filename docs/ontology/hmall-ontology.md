# HMall 电商本体（E-Commerce Ontology）

系统级统一业务模型。定义核心业务对象、关联关系、可用操作和领域事件。
本体是组织的共同语言——业务、产品、开发、AI 共享同一套业务概念。

## 全局约定

- 标识：每个对象有唯一 ID（Long），跨 BC 引用通过 ID
- 金额：统一以"分"为单位（如 599900 = ¥5999.00）
- 全局关联键：orderId 贯穿 Order → Payment → Inventory → Fulfillment → Activity
- 商品类型：PHYSICAL（实体）和 SERVICE（服务/虚拟）贯穿 Catalog → Cart → Order → Fulfillment

---

## 一、对象类型

### Catalog（商品目录）

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| Category | 聚合根 | categoryId | 两级类目树（一级→二级），商品挂在叶子类目 |
| Spu | 聚合根 | spuId | 商品（标准产品单元），分 PHYSICAL / SERVICE 两种 productType |
| Sku | 实体 | skuId | 规格变体 = 选项组合 + 价格（priceCents），属于 Spu，全局唯一 |
| SpecDimension | 实体 | specDimensionId | 规格维度（如"颜色"、"版本"），属于 Spu |
| SpecOption | 实体 | specOptionId | 规格选项（如"黑色"、"12+256G"），属于 SpecDimension |
| ServiceBinding | 实体 | serviceBindingId | 服务 SKU 对实体 SPU 的适用关系 + 可选价格覆盖 |
| EngravingPattern | 聚合根 | patternId | 镭雕图案库，供下单时选择雕刻图案 |

### User（用户）

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| User | 聚合根 | userId | 用户账号（username 全局唯一） |
| Address | 实体 | addressId | 收货地址：收件人、电话、省、市、区、详细地址 |

### Cart（购物车）

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| Cart | 聚合根 | cartId | 用户购物车（每人一个，首次操作自动创建） |
| CartItem | 实体 | cartItemId | 购物车项：skuId + quantity + 可选 relatedSkuId（服务关联实体 SKU） |

### Order（订单）— 全局关联键

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| Order | 聚合根 | orderId | 订单 = 用户 + 商品明细 + 地址 + 支付引用 + 履约引用 |
| OrderLineItem | 实体 | lineItemId | 订单行：skuId + quantity + unitPriceCents + itemType（PHYSICAL/SERVICE） |

OrderStatus: PENDING_PAYMENT → PAID → FULFILLING → SHIPPED → DELIVERED → COMPLETED | CANCELLED

### Inventory（库存）

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| SkuStock | 聚合根 | skuId | SKU 库存水位：available（可售）+ reserved（已占用）；仅 PHYSICAL 商品需要库存 |

### Payment（支付）

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| Payment | 聚合根 | paymentId | 支付单：orderId + amountCents + status + payUrl |

PaymentStatus: PENDING → COMPLETED | EXPIRED | REFUNDED

### Fulfillment（履约）

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| FulfillmentOrder | 聚合根 | fulfillmentOrderId | 履约单，分 PHYSICAL（物流配送）和 VIRTUAL（即时激活）两种 |
| FulfillmentItem | 实体 | fulfillmentItemId | 履约商品明细：skuId + quantity + itemType |

PHYSICAL 状态: CREATED → ALLOCATING → SHIPPED → DELIVERED
VIRTUAL 状态: CREATED → ACTIVATED

### Activity（运营数据）

| 对象 | 类型 | 标识 | 说明 |
|------|------|------|------|
| BusinessActivity | 聚合根 | eventId | 事件物化视图，消费所有 BC 的 Kafka 事件，按 orderId 可查完整时间线 |

---

## 二、关联关系

### 聚合内部（组合）

Category ←parent── Category（自引用：父子类目）
Spu ──has──→ SpecDimension ──has──→ SpecOption
Spu ──has──→ Sku ──has──→ SkuSpecValue → SpecOption
Spu ──has──→ ProductImage
Order ──has──→ OrderLineItem
Cart ──has──→ CartItem
FulfillmentOrder ──has──→ FulfillmentItem

### 跨对象引用

Spu.categoryId              → Category         商品所属类目
ServiceBinding.serviceSkuId → Sku(SERVICE)      服务 SKU
ServiceBinding.targetSpuId  → Spu(PHYSICAL)     适用的实体商品
User ──owns──→ Address                          用户的收货地址
Cart.userId                 → User              购物车归属
CartItem.skuId              → Sku               加购的 SKU
CartItem.relatedSkuId       → Sku(PHYSICAL)     服务关联的实体 SKU（可选）
Order.userId                → User              下单用户
OrderLineItem.skuId         → Sku               购买的 SKU
SkuStock.skuId              → Sku               库存对应的 SKU（仅 PHYSICAL）
Payment.orderId             → Order             支付对应的订单
FulfillmentOrder.orderId    → Order             履约对应的订单（1:N，一单可拆多个履约单）
BusinessActivity.orderId    → Order             事件关联的订单

### 导航路径

从 Order（全局关联键）出发：
  → User（下单用户）
  → OrderLineItem → Sku → Spu → Category（购买了什么商品）
  → Payment（支付状态与金额）
  → FulfillmentOrder（履约进度，可能多个）
  → BusinessActivity（完整事件时间线）

从 Sku（商品变体）出发：
  → Spu → Category（商品归属与分类）
  → SkuStock（库存水位）
  → ServiceBinding → Spu(PHYSICAL)（服务适用哪些实体商品）

从 User 出发：
  → Address（收货地址列表）
  → Cart → CartItem → Sku（购物车内容）
  → Order → ...（订单历史）

---

## 三、工具映射（MCP Tools）

| 工具 | 操作对象 | 能力 |
|------|----------|------|
| catalog_categories | Category | list / tree / get / create / update / delete |
| catalog_products | Spu | list / get / create / update / delete |
| catalog_dimensions | SpecDimension, SpecOption | list / add_dimension / add_option / delete_option |
| catalog_skus | Sku | list / get / get_by_id / create / update / delete |
| catalog_service_bindings | ServiceBinding | list / create / update / delete |
| catalog_available_services | ServiceBinding | 查某实体 SPU 的可选服务列表（含最终售价） |
| catalog_upload_image | ProductImage | 上传本地图片 |
| catalog_product_images | ProductImage（产品级） | list / add / delete |
| catalog_option_images | ProductImage（选项级） | list / add / delete |
| inventory_stock | SkuStock | list / get / update |
| cart_manage | Cart, CartItem | list / add / update_quantity / remove / checkout_preview |
| order_query | Order | get / list / cancel |
| order_create | Order | 创建订单（联动 Inventory 占库存 + Payment 创建支付单） |
| fulfillment_orders | FulfillmentOrder | get / list / allocate / ship / deliver |
| user_manage | User | list / get / create |
| user_addresses | Address | list / get / create / update / delete |
| activity_query | BusinessActivity | list / recent / stats（四大统计维度） |

系统编排（AI 不直接调用）：
- Inventory occupy/release — 由 Order 同步调用
- Fulfillment create/cancel — 由 Order 同步调用
- Payment create/complete/expire/refund — 由 Order + 网关 + 定时任务调用


