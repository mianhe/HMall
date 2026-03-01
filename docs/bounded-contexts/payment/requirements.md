# Payment 限界上下文 - 需求分析

Payment BC 负责**支付单的创建、支付结果处理、退款与超时检测**。上游 Order 通过同步调用创建支付/退款；支付结果由支付网关回调驱动，Payment 发布领域事件供 Order 等订阅。契约与事件详见 [event-flow.md](./event-flow.md)。

### 状态图例

- ✅ 已实现
- 🔄 部分完成
- 🔲 待实现

---

## 一、BC 职责概览

| 职责 | 说明 |
|------|------|
| **扣款（创建支付）** | Order 同步调用创建支付单，返回支付链接；用户跳转网关完成支付，网关回调 Payment |
| **退款** | Order 取消且已支付时，同步调用退款 |
| **超时检测** | 支付单在配置的超时时长内未支付则自动关闭，发布 PaymentExpired（未配置时默认 30 分钟） |

---

## 二、功能需求

### 1. 创建支付单  
`payment-create.feature`（建议）

- ✅ 1.1 Order 调用创建支付接口（orderId, amountCents）时应成功创建支付单并返回 paymentId 与支付链接（payUrl）
- ✅ 1.2 同一 orderId 重复创建支付时应幂等（返回已有支付单信息，不重复创建）
- ✅ 1.3 orderId 或 amountCents 缺失/非法（如 amountCents ≤ 0）时应返回 400 等错误
- ✅ 1.4 支付单创建后状态为 PENDING；支付链接可供前端跳转至支付网关（或模拟网关）

**说明**：金额单位与 Order 一致，建议**分**（amountCents）。payUrl 在真实实现中为网关生成链接；当前可为模拟链接（如 `/mock-pay?paymentId=xxx`），由测试或模拟网关触发回调。

---

### 2. 支付网关回调（支付结果）  
`payment-callback.feature`（建议）

- ✅ 2.1 收到网关「支付成功」回调时，应将支付单置为 COMPLETED 并发布 **PaymentCompleted**（orderId, paymentId, occurredAt）
- ✅ 2.2 收到网关「支付失败」回调时，支付单保持 PENDING（用户可重试），发布 **PaymentFailed**（orderId, occurredAt）通知外界本次尝试失败
- ✅ 2.3 对同一支付单的重复成功回调应幂等（不重复发布 PaymentCompleted）
- ✅ 2.4 回调中 paymentId 或签名等不合法时应拒绝并返回 4xx，不发布事件

**说明**：真实实现需校验网关签名、防伪造。当前可先约定回调 API 与载荷格式，用测试或模拟请求驱动。

---

### 3. 超时检测  
`payment-expire.feature`（建议）

- ✅ 3.1 支付单在配置的超时时长内未支付时，应自动将状态置为 EXPIRED 并发布 **PaymentExpired**（orderId, occurredAt）；超时时长未配置时默认 30 分钟
- ✅ 3.2 已 COMPLETED 或已 EXPIRED 的支付单不再参与超时检测
- ✅ 3.3 超时检测应自动执行（定时任务，间隔可配置，默认 1 分钟）；超时时长可配置，未配置时默认 30 分钟

---

### 4. 退款  
`payment-refund.feature`（建议）

- ✅ 4.1 Order 调用退款接口（orderId）且该订单已支付（支付单 COMPLETED）时应退款成功
- ✅ 4.2 退款成功后应将支付单置为 REFUNDED（或保留 COMPLETED 并记录退款标识，按实现选择）
- ✅ 4.3 同一 orderId 重复退款应幂等（已退款则返回成功，不重复退）
- ✅ 4.4 订单未支付（PENDING/EXPIRED）时调用退款应返回业务错误（如「未支付不可退款」）
- ✅ 4.5 orderId 缺失或对应支付单不存在时应返回 400/404

**说明**：真实实现会调用支付网关退款接口；当前可为本地状态更新 + 事件（若需要 PaymentRefunded 可后续补充）。

---

### 5. 查询支付单（可选）

- ✅ 5.1 按 paymentId 或 orderId 查询支付单时应返回状态、金额、创建时间等
- ✅ 5.2 支付单不存在时返回 404

**说明**：Order 或 BFF 可能需查询支付状态以展示；若 Order 仅依赖事件更新状态，可先不做查询接口，按需再补。

---

## 三、与 Order 的契约小结

| Order 动作 | Payment 提供 | 说明 |
|------------|--------------|------|
| PlaceOrder 成功（库存占用后） | **同步** createPayment(orderId, amountCents) → paymentId, payUrl | 创建支付单，返回链接 |
| 用户完成/失败/超时 | Payment 发布事件 | PaymentCompleted / PaymentFailed / PaymentExpired，Order 订阅 |
| CancelOrder（已支付） | **同步** refund(orderId) | 退款，幂等 |

---

## 四、功能与 feature 对应（建议）

| 功能 | .feature 文件 | 状态 | 预估 Scenario 数 |
|------|----------------|------|------------------|
| 1. 创建支付单 | payment-create.feature | ✅ 已实现 | 4 |
| 2. 支付回调 | payment-callback.feature | ✅ 已实现 | 4 |
| 3. 超时检测 | payment-expire.feature | ✅ 已实现 | 3 |
| 4. 退款 | payment-refund.feature | ✅ 已实现 | 5 |
| 5. 查询（可选） | payment-query.feature | ✅ 已实现 | 2 |

---

## 五、非功能与实现要点

- **幂等**：createPayment 按 orderId 幂等；refund 按 orderId 幂等；回调按 paymentId 幂等。
- **网关边界**：真实支付网关（支付宝/微信等）由 Payment 通过「支付网关适配器」对接；当前可用模拟网关（返回固定 payUrl，回调由测试或脚本触发）。
- **事件总线**：PaymentCompleted / PaymentFailed / PaymentExpired 通过 Kafka 发布，跨 BC 通信统一走 Kafka（见 [context-map.md](../../context-map.md)）。

以上为 Payment BC 的需求分析，可作为后续领域模型、API 契约与 ATDD 场景的输入。领域模型见 [domain-model.md](./domain-model.md)。
