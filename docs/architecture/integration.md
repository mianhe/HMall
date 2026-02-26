# HMall 集成技术选型

描述 BC 间通信及前后端集成所采用的技术栈。Context Map 见 [context-map.md](../context-map.md)。

---

## 一、集成方式概览

| 方式 | 用途 | 技术 |
|------|------|------|
| **BFF** | 前端统一 API 入口，代理到各 BC | HTTP 透传、路径路由 |
| **REST** | 同步调用（如 Order 拉取 Catalog SKU、User 地址；Order 调用 Inventory 占用/释放、Payment 创建支付/退款；BFF 代理到 Catalog/User/Order） | HTTP + JSON |
| **事件** | 异步编排（OrderCreated、PaymentCompleted 等） | 事件总线 |

---

## 二、BFF（前端聚合层）

`frontend/admin`、`frontend/web` 所有 API 请求统一经 BFF（端口 8085）转发到 Catalog、User、Order 等下游服务。

- **开发环境**：Vite proxy 将 `/api` 指向 BFF
- **路由**：按路径前缀转发（`/api/categories`、`/api/products` → Catalog；`/api/users`、`/api/login` → User；`/api/orders` → Order）
- **透传**：请求与响应原样转发，不聚合、不转换
- **CORS**：BFF 在 Filter 层添加 CORS 头，支持跨域直连

详见 [bounded-contexts/bff/requirements.md](../bounded-contexts/bff/requirements.md)。

---

## 三、REST

- **协议**：HTTP/1.1，JSON 序列化
- **前端 → 后端**：经 BFF 代理，单一入口
- **BC 间**（如 Order 调用 Catalog、User）：HTTP，`localhost` 或服务发现

---

## 四、事件总线

跨 BC 的领域事件通过**消息队列**传递。微服务拆分后，各 BC 发布/订阅事件需依赖独立的消息中间件。

### 技术选型

| 方案 | 适用场景 | 说明 |
|------|----------|------|
| **Kafka** | 高吞吐、事件日志、事件溯源 | 推荐用于 Order 编排等事件驱动场景 |
| **RabbitMQ** | 灵活路由、传统消息队列 | 可作为替代方案，运维相对简单 |

**当前决策**：**Kafka** 作为事件总线。

- 与 Order 的 Saga 编排、事件流（event-flow）匹配
- 支持事件持久化、重放、多消费者
- 便于后续扩展事件溯源、CQRS 等

### 使用约定

| 约定 | 说明 |
|------|------|
| **Topic 命名** | 按事件类型，如 `order.created`、`order.cancelled` |
| **消息格式** | JSON，含 `eventType`、`aggregateId`、`payload`、`occurredAt` 等 |
| **消费语义** | 至少一次（at-least-once），消费者需保证幂等 |
| **当前** | 所有 BC 的领域事件**统一发往 Kafka**（不使用 Spring 进程内事件），供跨 BC 订阅。Topic：`inventory.stock.*`、`order.created` / `order.cancelled` / `order.completed`、`payment.completed` / `payment.failed` / `payment.expired`、`fulfillment.order.created` / `fulfillment.shipped` / `fulfillment.delivered`。验收测试使用 **EventCapture**（实现发布端口并记录事件），Step 断言基于 Capture，无需真实 Kafka。 |

---

## 五、形态演进

| 形态 | 前端接入 | REST（BC 间） | 事件 |
|------|----------|---------------|------|
| **当前** | BFF 代理（frontend/admin、frontend/web → BFF → Catalog/User/Order） | REST/HTTP | Kafka |
| **微服务** | BFF 代理 + 服务发现 | HTTP + 服务发现 | Kafka |
