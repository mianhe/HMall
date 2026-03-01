# Order 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。契约：`docs/bounded-contexts/order/api.yaml`。Feature 目录：`services/order-service/src/test/resources/features/order/`。事件流见 [event-flow.md](./event-flow.md)，领域模型见 [domain-model.md](./domain-model.md)。

### 状态图例

- ✅ 已实现（后端 + 测试 + 契约均已完成）
- 🔄 部分完成（部分场景已实现）
- 🔲 待实现（全新需求）

---

## 1. 创建订单  
`order-create.feature`

- ✅ 1.1 提交订单（含明细、收货地址）时应成功并返回 orderId 及 PENDING_PAYMENT 状态
- ✅ 1.1a 创建成功时应发布 OrderCreated 领域事件
- ✅ 1.1b 库存占用成功后订单才落库并返回成功；库存不足时应失败并返回库存不足错误（可打桩实现）
- ✅ 1.2 订单明细为空时创建订单应失败并返回错误
- ✅ 1.3 收货地址缺省或格式不合法时应失败并返回错误
- ✅ 1.4 明细中 skuId 不存在时应失败并返回 404
- ✅ 1.5 数量≤0 或单价<0 时应失败并返回错误
- ✅ 1.6 userId 不存在时应失败并返回 404
- 🔲 1.7 明细中含 SERVICE 类型商品时，仅对 PHYSICAL 类型明细调用库存占用，SERVICE 类型跳过（来自业务需求 [虚拟商品](../../business-requirements/virtual-product/overview.md)）
- 🔲 1.8 纯服务订单（全部明细均为 SERVICE）时应跳过库存占用，直接创建成功

---

## 2. 取消订单  
`order-cancel.feature`

- ✅ 2.1 待支付（已占用库存）状态下取消应成功并发布 OrderCancelled，同步释放库存
- ✅ 2.2 已支付时取消应触发退款、释放库存等补偿
- ✅ 2.2a 已履约（FULFILLING）或已支付（PAID）状态取消应同步取消履约单（若已创建）+ 退款 + 释放库存
- ✅ 2.2b 已发货（SHIPPED）状态下取消应失败并返回错误
- ✅ 2.2c 已签收（DELIVERED）状态下取消应失败并返回错误
- ✅ 2.3 已取消或已完成状态下取消订单应失败并返回错误
- ✅ 2.4 订单不存在时取消应返回 404

---

## 3. 查询订单  
`order-query.feature`

- ✅ 3.1 按 ID 查询订单时应返回订单详情（含明细、收货地址、status）
- ✅ 3.2 订单不存在时查询应返回 404
- ✅ 3.3 按 userId 查询订单列表时应返回该用户的订单（可分页）

---

## 4. 事件驱动（Order 订阅）与同步调用 Fulfillment

- ✅ 4.1 收到 PaymentCompleted 后应将 status 置为 PAID，**同步调用 Fulfillment 创建履约单**，返回后**保持 PAID**（不置 FULFILLING）
- ✅ 4.2a 收到 PaymentFailed 后订单保持 PENDING_PAYMENT（用户可重试支付），不释放库存
- ✅ 4.2b 收到 PaymentExpired 后应取消订单（含释放库存、取消履约单（若已创建））
- ✅ 4.3 收到 FulfillmentOrderAllocated 后应将 status 置为 FULFILLING（履约已开始配货，订单页可显示「正在配货」）
- ✅ 4.4 收到 FulfillmentShipped 后应更新 status 为 SHIPPED
- ✅ 4.5 收到 FulfillmentDelivered 后应将 status 置为 DELIVERED 并发布 OrderCompleted
- 🔲 4.6 收到 ServiceActivated 后应等效 FulfillmentDelivered 处理；混合订单按最慢原则——实体 Delivered + 虚拟 Activated 全部到达才推进 OrderCompleted（来自业务需求 [虚拟商品](../../business-requirements/virtual-product/overview.md)）

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 状态 | Scenario 数 | 备注 |
|------|----------------|------|-------------|------|
| 1. 创建订单 | order-create.feature | 🔄 需变更 | 8 + 2 | 1.7-1.8 来自虚拟商品业务需求 |
| 2. 取消订单 | order-cancel.feature | ✅ 已完成 | 8 | PAID/FULFILLING 取消调用 CancelFulfillmentPort；SHIPPED/DELIVERED 不可取消；ACTIVATED 虚拟单 MVP 不可取消 |
| 3. 查询订单 | order-query.feature | ✅ 已完成 | 3 | — |
| 4. 事件驱动 | order-events.feature | 🔄 需变更 | 6 + 1 | 4.6 消费 ServiceActivated 来自虚拟商品业务需求 |

---

## Fulfillment 集成带来的 Order 变更汇总（已完成）

> 以下变更已实现。详见 [fulfillment/requirements.md](../fulfillment/requirements.md) 的「Order BC 变更清单」。

- **order-cancel**：取消仅允许 PENDING_PAYMENT / PAID / FULFILLING；PAID 或 FULFILLING 取消时同步调用 CancelFulfillmentPort；SHIPPED / DELIVERED 取消失败（2.2b、2.2c）。
- **order-events**：4.1 PaymentCompleted 后置 PAID 并同步创建履约单，保持 PAID；4.3 消费 FulfillmentOrderAllocated 后置 FULFILLING。
