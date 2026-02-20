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

---

## 2. 取消订单  
`order-cancel.feature`

- ✅ 2.1 待支付（已占用库存）状态下取消应成功并发布 OrderCancelled，同步释放库存
- ✅ 2.2 已支付时取消应触发退款、释放库存等补偿
- 🔄 2.2a 已履约（FULFILLING）状态取消应同步取消履约单 + 退款 + 释放库存（**需改**：新增 CancelFulfillmentPort 调用）
- 🔲 2.2b 已发货（SHIPPED）状态下取消应失败并返回错误（**新增**：当前实现允许取消，需收紧）
- 🔲 2.2c 已签收（DELIVERED）状态下取消应失败并返回错误（**新增**：当前实现允许取消，需收紧）
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

- 🔄 4.1 收到 PaymentCompleted 后应将 status 置为 PAID，**同步调用 Fulfillment 创建履约单**，返回 fulfillmentOrderIds 后当场更新 fulfillmentRef 并置 FULFILLING（**需改**：原为置 PAID 后调用 NoOp 桩，不推进状态；现改为同步调用 + 当场推进到 FULFILLING）
- ✅ 4.2a 收到 PaymentFailed 后订单保持 PENDING_PAYMENT（用户可重试支付），不释放库存
- ✅ 4.2b 收到 PaymentExpired 后应取消订单（含释放库存、取消履约单（若已创建））
- ~~✅ 4.3 收到 FulfillmentOrderCreated 后应更新 fulfillmentRef 并将 status 置为 FULFILLING~~（**删除**：改为同步调用返回后当场推进，不再消费此事件）
- ✅ 4.4 收到 FulfillmentShipped 后应更新 fulfillmentStatus 为 SHIPPED
- ✅ 4.5 收到 FulfillmentDelivered 后应将 status 置为 DELIVERED 并发布 OrderCompleted

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 状态 | Scenario 数 | 备注 |
|------|----------------|------|-------------|------|
| 1. 创建订单 | order-create.feature | ✅ 已完成 | 8 | 无变更 |
| 2. 取消订单 | order-cancel.feature | 🔄 需调整 | 6 → ~8 | 新增 SHIPPED/DELIVERED 不可取消；FULFILLING 取消需调用 CancelFulfillmentPort |
| 3. 查询订单 | order-query.feature | ✅ 已完成 | 3 | 无变更 |
| 4. 事件驱动 | order-events.feature | 🔄 需调整 | 5 → ~4 | 4.1 改为同步调用+当场推进；删除 4.3（不再消费 FulfillmentOrderCreated） |

---

## Fulfillment 集成带来的 Order 变更汇总

> 以下变更在 Fulfillment BC 实现后、集成阶段执行。详见 [fulfillment/requirements.md](../fulfillment/requirements.md) 的「Order BC 变更清单」。

### order-cancel.feature 变更

| 变更 | 现状 | 目标 |
|------|------|------|
| 取消规则收紧 | 仅 CANCELLED / COMPLETED 不可取消 | SHIPPED / DELIVERED / COMPLETED / CANCELLED 不可取消 |
| FULFILLING 取消补偿 | 仅释放库存 + 退款 | 新增同步调用 CancelFulfillmentPort 取消履约单 |
| 新增 scenario | — | 2.2b SHIPPED 不可取消、2.2c DELIVERED 不可取消 |

### order-events.feature 变更

| 变更 | 现状 | 目标 |
|------|------|------|
| 4.1 PaymentCompleted 处理 | 置 PAID + 调用 NoOp 桩 | 置 PAID + **同步调用 Fulfillment 创建履约单** + 返回后当场更新 fulfillmentRef、置 FULFILLING |
| 4.3 FulfillmentOrderCreated | Order 消费 Kafka 事件 → 置 FULFILLING | **删除**：不再消费此事件，改为同步调用返回后推进 |
| CreateFulfillmentPort 签名 | `createFulfillment(orderId)` | `createFulfillment(orderId, items, shippingAddress) → List<Long>` |
