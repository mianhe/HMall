# Fulfillment 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。Feature 目录：`services/fulfillment-service/src/test/resources/features/fulfillment/`。事件流见 [event-flow.md](./event-flow.md)，领域模型见 [domain-model.md](./domain-model.md)。

### 状态图例

- ✅ 已实现
- 🔄 部分完成
- 🔲 待实现

---

## 一、职责说明

Fulfillment 负责订单的履约执行：接收 Order 的创建履约请求，按业务规则拆单，管理每个履约单从创建→配货→发货→签收的完整生命周期。

**核心设计决策**：

| # | 决策 | 说明 |
|---|------|------|
| F1 | 拆单由 Fulfillment 负责 | Order 传入 orderId + items + shippingAddress，Fulfillment 决定拆成几个履约单 |
| F2 | 创建履约单为同步调用 | Order 同步调用，Fulfillment 返回 fulfillmentOrderIds，Order 保持 PAID（不推进 FULFILLING） |
| F2a | 开始配货后发布事件推进 Order | 履约单 CREATED → ALLOCATING 时发布 FulfillmentOrderAllocated，Order 消费后置 FULFILLING |
| F3 | 发货/签收通过 Kafka 事件通知 | Fulfillment 发布 FulfillmentShipped/Delivered，Order 消费后推进状态 |
| F4 | 取消履约单为同步调用 | Order 补偿时同步调用，仅 CREATED、ALLOCATING 状态可取消 |
| F5 | 多履约单按「最慢」原则推进 Order 状态 | 全部发货才 SHIPPED，全部签收才 DELIVERED |
| F6 | MVP 先做 1:1 不拆单 | 架构预留 1:N 能力，但首版所有商品放入同一个履约单 |

---

## 1. 创建履约单

`fulfillment-create.feature`

- ✅ 1.1 Order 调用创建履约单接口（传入 orderId + items + shippingAddress），应创建履约单并返回 fulfillmentOrderIds
- ✅ 1.2 创建成功时应发布 FulfillmentOrderCreated 事件（含 orderId、fulfillmentOrderIds）
- ✅ 1.3 同一 orderId 重复调用时应幂等处理（返回已有的 fulfillmentOrderIds，不重复创建）
- ✅ 1.4 入参缺失 orderId 或 items 为空时应返回 400 错误

---

## 2. 开始配货

`fulfillment-allocate.feature`

- ✅ 2.0.1 CREATED 状态的履约单执行开始配货应成功，状态变为 ALLOCATING
- ✅ 2.0.2 开始配货成功时应发布 FulfillmentOrderAllocated 事件（含 orderId、fulfillmentOrderId）
- ✅ 2.0.3 非 CREATED 状态执行开始配货应失败并返回错误
- ✅ 2.0.4 履约单不存在时应返回 404

---

## 3. 发货

`fulfillment-ship.feature`

- ✅ 3.1 ALLOCATING 状态的履约单执行发货应成功，状态变为 SHIPPED（CREATED → ALLOCATING → SHIPPED）
- ✅ 3.2 发货成功时应发布 FulfillmentShipped 事件（含 orderId、fulfillmentOrderId）
- ✅ 3.3 非 ALLOCATING 状态（含 CREATED）执行发货应失败并返回错误
- ✅ 3.4 履约单不存在时应返回 404

---

## 4. 签收确认

`fulfillment-deliver.feature`

- ✅ 4.1 SHIPPED 状态的履约单确认签收应成功，状态变为 DELIVERED
- ✅ 4.2 签收成功时应发布 FulfillmentDelivered 事件（含 orderId、fulfillmentOrderId）
- ✅ 4.3 非 SHIPPED 状态确认签收应失败并返回错误
- ✅ 4.4 履约单不存在时应返回 404

---

## 5. 取消履约单

`fulfillment-cancel.feature`

- ✅ 5.1 CREATED 或 ALLOCATING 状态的履约单取消应成功，状态变为 CANCELLED
- ✅ 5.2 SHIPPED 或 DELIVERED 状态取消应失败并返回错误（已发货不可取消）
- ✅ 5.3 按 orderId 取消该订单的所有未发货履约单（供 Order 补偿调用）

---

## 6. 查询履约单

`fulfillment-query.feature`

- ✅ 6.1 按 fulfillmentOrderId 查询应返回履约单详情（含商品明细、状态、物流信息）
- ✅ 6.2 按 orderId 查询应返回该订单的所有履约单
- ✅ 6.3 履约单不存在时应返回 404

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 状态 | 预计 Scenario 数 |
|------|----------------|------|-----------------|
| 1. 创建履约单 | fulfillment-create.feature | ✅ 已实现 | 4 |
| 2. 开始配货 | fulfillment-allocate.feature | ✅ 已实现 | 4 |
| 3. 发货 | fulfillment-ship.feature | ✅ 已实现 | 5 |
| 4. 签收确认 | fulfillment-deliver.feature | ✅ 已实现 | 4 |
| 5. 取消履约单 | fulfillment-cancel.feature | ✅ 已实现 | 4 |
| 6. 查询履约单 | fulfillment-query.feature | ✅ 已实现 | 3 |
| **合计** | | | **24** |

---

## Order BC 变更清单

Fulfillment 上线时需同步调整 Order BC，变更项如下（在集成阶段执行）：

| # | 变更 | 说明 | 状态 |
|---|------|------|------|
| O1 | 取消规则收紧 | SHIPPED / DELIVERED / COMPLETED 不可取消 | ✅ 已完成 |
| O2 | `CreateFulfillmentPort` 接口变更 | 签名改为 `createFulfillment(orderId, items, shippingAddress) → List<fulfillmentOrderIds>`；Order 拿到返回值后**保持 PAID**（不置 FULFILLING） | ✅ 已完成 |
| O3 | 新增 `CancelFulfillmentPort` | Order 取消 PAID/FULFILLING 订单时同步调用 Fulfillment 取消履约单 | ✅ 已完成 |
| O4 | 消费 FulfillmentOrderAllocated | Order 订阅 `fulfillment.order.allocated`，收到后置 FULFILLING | ✅ 已完成 |
| O5 | 保留 onFulfillmentShipped / onFulfillmentDelivered | 行为不变 | ✅ 已完成 |
| O6 | Shipped/Delivered 事件处理适配 1:N | MVP 阶段 1:1 无需改动，架构预留 | 🔲 待需要时实现 |
