# Payment 限界上下文 - 事件流与集成契约

与 Order 的集成方式、入站 API、出站事件及支付网关边界。需求列表见 [requirements.md](./requirements.md)。

---

## 一、集成方式

- **入站**：Order 通过 **REST/同步调用** 创建支付单（createPayment）、退款（refund）；支付网关通过 **HTTP 回调** 通知支付结果。
- **出站**：Payment 根据支付结果发布 **领域事件**（PaymentCompleted / PaymentFailed / PaymentExpired），Order 订阅并驱动状态与下游履约。

---

## 二、参与方

| BC / 系统 | 关系 |
|-----------|------|
| Order | 上游：同步调用 createPayment、refund |
| Payment | 本 BC：支付单生命周期、回调处理、超时、事件发布 |
| 支付网关 | 外部：用户完成支付后回调 Payment；真实实现为支付宝/微信等，当前可为模拟网关 |

---

## 三、调用契约（Order → Payment）

### 创建支付单

| 调用方 | 时机 | 入参 | 返回值 |
|--------|------|------|--------|
| Order | PlaceOrder 且库存占用成功后 | orderId, amountCents | paymentId, payUrl；失败时 4xx |

- 幂等：同一 orderId 再次调用返回已有支付单信息。

### 退款

| 调用方 | 时机 | 入参 | 返回值 |
|--------|------|------|--------|
| Order | CancelOrder 且订单已支付时 | orderId | 成功 / 业务错误（未支付等） |

- 幂等：已退款则返回成功。

---

## 四、支付网关回调（Gateway → Payment）

| 回调结果 | Payment 行为 | 发布事件 |
|----------|--------------|----------|
| 成功 | 支付单置 COMPLETED | PaymentCompleted（orderId, paymentId） |
| 失败 | 支付单置 FAILED | PaymentFailed（orderId） |

**超时**：由 Payment 内部定时/延迟任务检测，不依赖网关回调；状态置 EXPIRED，发布 PaymentExpired（orderId）。

回调 API 形式建议：`POST /api/payments/callback` 或网关约定的 notify URL，请求体含 paymentId、结果状态、签名等（具体与网关契约一致；模拟时可简化为 paymentId + status）。

---

## 五、事件契约（Payment 发布）

### 事件列表

| 事件 | 时机 | 载荷 | 订阅方（典型） |
|------|------|------|----------------|
| PaymentCompleted | 网关回调支付成功 / 模拟成功 | orderId, paymentId, occurredAt | Order（置 PAID、创建履约单） |
| PaymentFailed | 网关回调支付失败 | orderId, occurredAt | Order（取消、补偿） |
| PaymentExpired | 超时检测到未支付 | orderId, occurredAt | Order（取消、补偿） |

### Kafka 发布（可选，与 Order/Inventory 一致）

| Topic | 事件 | 消息体（JSON）示例 |
|-------|------|---------------------|
| `payment.completed` | PaymentCompleted | eventType, orderId, paymentId, occurredAt |
| `payment.failed` | PaymentFailed | eventType, orderId, occurredAt |
| `payment.expired` | PaymentExpired | eventType, orderId, occurredAt |

---

## 六、流程概览

```
Order                          Payment                        网关
  |                               |                              |
  | createPayment(orderId, amount)|                              |
  |------------------------------>|                              |
  |   paymentId, payUrl           |                              |
  |<------------------------------|                              |
  |                               |                              |
  | (前端跳转 payUrl)              |                              |
  |                               |<-------- 用户支付 ---------->|
  |                               |<-------- 回调(成功/失败) -----|
  |                               | 发布 PaymentCompleted/Failed |
  |<-------- 事件 ----------------|                              |
  | 更新订单状态 / 创建履约单等     |                              |
  |                               |                              |
  | refund(orderId)               |                              |
  |------------------------------>|                              |
  |   success                     |                              |
  |<------------------------------|                              |
```

超时：Payment 内部在约定时间后未收到成功/失败回调时，将支付单置为 EXPIRED 并发布 PaymentExpired，Order 订阅后执行取消与补偿。

---

## 七、与 Order Saga 的对应

| Saga 步骤 | Order 动作 | Payment 角色 |
|-----------|------------|--------------|
| T3（同步） | placeOrder 流程中调用 createPayment | 提供 createPayment，返回 payUrl；后续由回调驱动事件 |
| 事件驱动 | 订阅 PaymentCompleted / Failed / Expired | 发布上述事件 |
| C3（补偿） | cancelOrder 且已支付时调用 refund | 提供 refund 接口，同步退款 |

以上为 Payment BC 的事件流与集成契约，可与 [order/event-flow.md](../order/event-flow.md) 对照实现。
