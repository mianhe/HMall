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
- ✅ 2.2 已支付时取消应触发退款、释放库存、取消履约单（若已创建）等补偿
- ✅ 2.3 已取消或已完成状态下取消订单应失败并返回错误
- ✅ 2.4 订单不存在时取消应返回 404

---

## 3. 查询订单  
`order-query.feature`

- ✅ 3.1 按 ID 查询订单时应返回订单详情（含明细、收货地址、status）
- ✅ 3.2 订单不存在时查询应返回 404
- ✅ 3.3 按 userId 查询订单列表时应返回该用户的订单（可分页）

---

## 4. 事件驱动（Order 订阅）

- ✅ 4.1 收到 PaymentCompleted 后应将 status 置为 PAID 并创建履约单
- ✅ 4.2a 收到 PaymentFailed 后订单保持 PENDING_PAYMENT（用户可重试支付），不释放库存
- ✅ 4.2b 收到 PaymentExpired 后应取消订单（含释放库存、取消履约单（若已创建））
- ✅ 4.3 收到 FulfillmentOrderCreated 后应更新 fulfillmentRef 并将 status 置为 FULFILLING
- ✅ 4.4 收到 FulfillmentShipped 后应更新 fulfillmentStatus 为 SHIPPED
- ✅ 4.5 收到 FulfillmentDelivered 后应将 status 置为 DELIVERED 并发布 OrderCompleted

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 状态 | Scenario 数 |
|------|----------------|------|-------------|
| 1. 创建订单 | order-create.feature | ✅ 已完成 | 8 |
| 2. 取消订单 | order-cancel.feature | ✅ 已完成 | 6 |
| 3. 查询订单 | order-query.feature | ✅ 已完成 | 3 |
| 4. 事件驱动 | order-events.feature | ✅ 已完成 | 5 |

**当前 Order 验收**：共 22 个业务 scenario（另含技术脚手架 smoke 1 个）。
