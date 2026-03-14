# HMall 智能运营本体（Intelligent Ops Ontology）

智能运营领域的结构化知识。定义事件类型语义、Payload 结构、业务流程状态机、因果链与健康指标。
本文件既是面向人的参考文档，也通过 MCP Resource 注入给 AI，作为运营分析的知识基础。

---

## 一、核心概念

### 事件驱动运营

HMall 的运营数据来自各限界上下文（BC）发布的领域事件。Activity BC 消费这些事件，构建统一的业务活动记录（BusinessActivity），为运营分析提供查询和统计能力。

### 事件三要素

| 要素 | 说明 |
|------|------|
| **eventType** | 事件类型标识（如 OrderCreated），唯一标识一种业务行为 |
| **category** | NORMAL（正向推动流程）/ COMPENSATION（补偿回滚）/ EXCEPTION（异常） |
| **origin** | DOMAIN（BC 状态变化）/ BEHAVIORAL（用户行为，预留）/ DERIVED（规则推断，预留） |

---

## 二、事件类型全表

### Order BC

| eventType | 中文标签 | category | 流程角色 | Payload 字段 |
|-----------|---------|----------|---------|-------------|
| OrderCreated | 订单创建 | NORMAL | trading:MILESTONE | orderId, userId, totalAmountCents, items[]{skuId, spuId, quantity, unitPriceCents} |
| OrderCancelled | 订单取消 | COMPENSATION（补偿 OrderCreated） | trading:MILESTONE | orderId, userId, totalAmountCents, items[] |
| OrderCompleted | 订单完成 | NORMAL | trading:MILESTONE, user_development:PROGRESSION, product_ops:PROGRESSION | orderId, userId, totalAmountCents, items[] |

### Inventory BC

| eventType | 中文标签 | category | 流程角色 | Payload 字段 |
|-----------|---------|----------|---------|-------------|
| StockReserved | 库存锁定 | NORMAL | trading:PROGRESSION | orderId, items[]{skuId, quantity} |
| StockReleased | 库存释放 | COMPENSATION（补偿 StockReserved） | trading:PROGRESSION | orderId |

### Payment BC

| eventType | 中文标签 | category | 流程角色 | Payload 字段 |
|-----------|---------|----------|---------|-------------|
| PaymentCompleted | 支付成功 | NORMAL | trading:MILESTONE | orderId, paymentId, amountCents |
| PaymentFailed | 支付失败 | EXCEPTION | trading:PROGRESSION | orderId |
| PaymentExpired | 支付超时 | EXCEPTION | trading:PROGRESSION | orderId |

### Fulfillment BC

| eventType | 中文标签 | category | 流程角色 | Payload 字段 |
|-----------|---------|----------|---------|-------------|
| FulfillmentOrderCreated | 履约单创建 | NORMAL | trading:PROGRESSION | orderId, fulfillmentOrderIds[] |
| FulfillmentOrderAllocated | 开始配货 | NORMAL | trading:PROGRESSION | orderId, fulfillmentOrderId |
| FulfillmentShipped | 已发货 | NORMAL | trading:PROGRESSION | orderId, fulfillmentOrderId |
| FulfillmentDelivered | 已签收 | NORMAL | trading:MILESTONE | orderId, fulfillmentOrderId |
| ServiceActivated | 服务已激活 | NORMAL | trading:PROGRESSION | orderId, fulfillmentOrderId |
| EngravingCompleted | 镭雕已完成 | NORMAL | trading:PROGRESSION | orderId, fulfillmentOrderId |

---

## 三、业务流程状态机

### 交易主流程（Happy Path）

```
OrderCreated → StockReserved → PaymentCompleted → FulfillmentOrderCreated
  → FulfillmentOrderAllocated → FulfillmentShipped → FulfillmentDelivered → OrderCompleted
```

### 异常路径

```
OrderCreated → StockReserved → PaymentFailed → （用户可重试支付）
OrderCreated → StockReserved → PaymentExpired → OrderCancelled → StockReleased
```

### 因果链（一个事件触发另一个事件）

| 触发事件 | 下游事件 | 触发条件 |
|---------|---------|---------|
| OrderCreated | StockReserved | 下单自动占库存 |
| PaymentCompleted | FulfillmentOrderCreated | 支付成功后自动创建履约单 |
| PaymentExpired | OrderCancelled | 超时未支付自动取消 |
| OrderCancelled | StockReleased | 取消订单释放库存 |
| FulfillmentDelivered | OrderCompleted | 签收后自动完成订单 |

---

## 四、Payload 字段语义

### 通用字段

| 字段 | 类型 | 说明 |
|------|------|------|
| eventType | string | 事件类型标识 |
| orderId | long | 关联的订单 ID（贯穿全流程的关联键） |
| occurredAt | ISO-8601 instant | 事件发生时间 |

### userId 覆盖范围

仅 **Order BC** 发布的事件（OrderCreated、OrderCancelled、OrderCompleted）在 Kafka 消息中携带 `userId`。Payment、Inventory、Fulfillment 事件不含 `userId`，需在消费侧通过 `orderId` 关联获取。Activity BC 的 `BusinessActivity.userId` 字段仅在收到含 userId 的事件时有值。

### 金额字段

| 字段 | 类型 | 说明 |
|------|------|------|
| totalAmountCents | long | 订单总金额（单位：分），如 599900 = ¥5999.00 |
| amountCents | long | 支付金额（单位：分） |
| unitPriceCents | long | 商品单价（单位：分） |

### 商品快照（items 数组）

仅出现在 OrderCreated/OrderCancelled/OrderCompleted/StockReserved 事件中。

| 字段 | 类型 | 说明 |
|------|------|------|
| skuId | long | SKU ID（规格变体） |
| spuId | long（可空） | SPU ID（商品），Order 事件有值，Inventory 事件无 |
| quantity | int | 购买数量 |
| unitPriceCents | long（可空） | 单价，仅 Order 事件有值 |

---

## 五、统计指标与数据查询

### 统计 API 字段

activity_query stats 和 stats_daily 返回以下聚合字段：

| 字段 | 含义 | 对应事件 |
|------|------|---------|
| ordersCreated | 已创建订单数 | OrderCreated |
| ordersCancelled | 已取消订单数 | OrderCancelled |
| ordersCompleted | 已完成订单数 | OrderCompleted |
| paymentAttempts | 支付尝试总数 | PaymentCompleted + PaymentFailed + PaymentExpired |
| paymentSuccess | 成功支付数 | PaymentCompleted |
| paymentFailed | 支付失败数 | PaymentFailed |
| paymentExpired | 支付过期数 | PaymentExpired |
| paymentTotalCents | 成功支付总额（分） | PaymentCompleted 的 amountCents 之和 |
| **distinctBuyerCount** | **下单用户数（去重）** | **COUNT(DISTINCT userId) WHERE eventType = OrderCreated**。仅 Order BC 事件携带 userId，故基于 OrderCreated 计数。结合 ordersCancelled 可推导实际购买用户数 |
| fulfillmentCreated | 履约单创建数 | FulfillmentOrderCreated |
| fulfillmentAllocated | 开始配货数 | FulfillmentOrderAllocated |
| fulfillmentShipped | 已发货数 | FulfillmentShipped |
| fulfillmentDelivered | 已签收数 | FulfillmentDelivered |
| stockReserved | 库存占用次数 | StockReserved |
| stockReleased | 库存释放次数 | StockReleased |

### 健康指标推导

| 指标 | 计算方式 | 正常阈值 | 异常信号 |
|------|---------|---------|---------|
| 支付成功率 | paymentSuccess / paymentAttempts | > 85% | 低于 70% 需排查支付通道 |
| 履约完成率 | fulfillmentDelivered / fulfillmentCreated | > 90% | 差值大说明积压 |
| 订单取消率 | ordersCancelled / ordersCreated | < 15% | 高取消率排查支付超时 |
| 库存异常 | stockReleased 远大于 stockReserved | — | 可能大量取消或退款 |
| 支付金额 | paymentTotalCents（分→元显示） | — | 与历史均值对比判断趋势 |

### 多维查询

| 维度 | 用途 | 工具调用示例 |
|------|------|-------------|
| 按订单 | 查看单笔订单完整生命周期 | activity_query list orderId=42 |
| 按用户 | 查看用户行为轨迹 | activity_query list userId=1 |
| 按商品(SKU) | 查看某规格的销售事件 | activity_query list skuId=10 |
| 按商品(SPU) | 查看某商品（含所有规格）事件 | activity_query list spuId=3 |
| 统计概览 | 某时段的聚合指标 | activity_query stats period=today |
| 每日统计 | 逐日趋势数据（用于趋势图） | activity_query stats_daily period=last7 |
| 最近动态 | 全系统最新事件流 | activity_query recent |

### 时间范围

- 快捷周期：`today`（今日）、`last7`（过去 7 天）、`last30`（过去 30 天）
- 自定义范围：`from=YYYY-MM-DD&to=YYYY-MM-DD`
- 默认：今日

---

## 六、业务流程概览

### 交易流程（trading）

核心流程，贯穿下单→支付→履约→完成全链路。

MILESTONE 事件（关键里程碑）：OrderCreated → PaymentCompleted → FulfillmentDelivered → OrderCompleted
PROGRESSION 事件（过程节点）：StockReserved → FulfillmentOrderCreated → FulfillmentOrderAllocated → FulfillmentShipped

### 用户发展（user_development）

追踪用户从注册到高价值用户的成长路径。

PROGRESSION 事件：OrderCompleted（每次完成订单推动用户价值成长）

### 商品运营（product_ops）

反映商品的市场表现。

PROGRESSION 事件：OrderCompleted（每次成交贡献商品销售数据）

---

## 七、订单事实分析（Order Fact Analytics）

### 概述

基于 BusinessActivity 事件流投影的 CQRS 读模型，提供以订单和商品为中心的多维运营分析能力。通过 `order_fact_query` MCP 工具访问。

### 读模型

| 读模型 | 粒度 | 说明 |
|--------|------|------|
| **OrderFact** | 每个订单一行 | 从事件推导：当前阶段、VAS 标记、时间戳、持续时间 |
| **OrderItemFact** | 每个订单行项一行 | 从 OrderCreated.items[] 展开，冗余订单级字段 |

### 关键派生字段

| 字段 | 推导逻辑 |
|------|---------|
| currentStage | 按优先级：CANCELLED > COMPLETED > DELIVERED > SHIPPED > FULFILLING > PAID > CREATED |
| hasEngraving | EngravingCompleted 事件存在 → true |
| hasWarranty | ServiceActivated 事件存在 → true |
| cancelReason | CANCELLED 时：有 PaymentExpired → TIMEOUT，否则 MANUAL |
| isAbnormal | 存在 PaymentFailed 或 PaymentExpired → true |
| paymentDurationSec | createdAt → paidAt 的秒数差 |
| fulfillmentDurationSec | paidAt → deliveredAt 的秒数差 |

### order_fact_query 工具

| action | 说明 | 典型用途 |
|--------|------|---------|
| stats | 订单事实聚合统计 | VAS 渗透率、客单价对比、取消原因、支付/履约效率、复购率 |
| stats_daily | 按日统计趋势 | VAS 趋势、日订单/收入波动 |
| product_ranking | 商品排名 | 销量/收入排行、含 VAS 商品分布 |
| list | 订单事实列表 | 按 VAS/阶段/用户筛选具体订单 |

### stats 指标字段

| 字段 | 含义 |
|------|------|
| totalOrders | 总订单数 |
| completedOrders / cancelledOrders / inProgressOrders | 各阶段分布 |
| cancelByTimeout / cancelByManual | 取消原因分布 |
| ordersWithEngraving / ordersWithWarranty / ordersWithAnyVas | VAS 渗透 |
| multiItemOrders | 多件订单数 |
| totalRevenueCents / avgOrderAmountCents | 收入与客单价 |
| avgVasOrderAmountCents / avgNonVasOrderAmountCents | VAS vs 非 VAS 客单价 |
| avgPaymentDurationSec / avgFulfillmentDurationSec | 效率指标 |
| distinctBuyerCount / repeatBuyerCount | 用户指标 |

### 与 activity_query 的关系

| 工具 | 视角 | 回答 |
|------|------|------|
| activity_query | 事件级（ODS 原始层） | "发生了什么？"——按事件计数、事件流水 |
| order_fact_query | 订单级（DWD 分析层） | "整体状况如何？"——VAS 渗透率、客单价、商品排名、效率 |

### 典型分析场景

| 场景 | 工具调用 |
|------|---------|
| 近 7 天 VAS 渗透率 | order_fact_query stats period=last7 → ordersWithAnyVas / totalOrders |
| VAS 客单价对比 | order_fact_query stats → avgVasOrderAmountCents vs avgNonVasOrderAmountCents |
| 取消原因分析 | order_fact_query stats → cancelByTimeout / cancelByManual |
| 商品销量排行 | order_fact_query product_ranking rankBy=quantity |
| 含镭雕商品分布 | order_fact_query product_ranking hasEngraving=true |
| 用户复购率 | order_fact_query stats → repeatBuyerCount / distinctBuyerCount |
| 查看某用户含 VAS 订单 | order_fact_query list userId=42 hasEngraving=true |
| 按日 VAS 趋势 | order_fact_query stats_daily → vasOrders / totalOrders 逐日 |
| 商品名称查询 | 先 order_fact_query product_ranking 获取 spuId，再 catalog_query 获取名称 |
