# Inventory 限界上下文 - 事件流

与 Order 的集成方式、调用契约与事件。领域结构见 [domain-model.md](./domain-model.md)。Order 侧同步调用约定见 [order/event-flow.md](../order/event-flow.md) 四、同步调用。

---

## 一、集成方式

**同步调用**：Order BC 通过 REST/Port 直接调用 Inventory，库存占用与释放均为同步操作。用户下单时可立即获得库存结果反馈（成功或库存不足）。

---

## 二、参与方

| BC | 关系 |
|----|------|
| Order | 上游：调用 Inventory 的占用、释放接口 |
| Catalog | 上游：SKU 存在性（可选校验，Inventory 以 skuId 引用，不强制调用） |
| Inventory | 本 BC，提供同步的库存占用与释放能力 |

---

## 三、调用契约

### 占用库存（Occupy）

| 调用方 | 时机 | 入参 | 返回值 |
|--------|------|------|--------|
| Order | 创建订单时，在保存订单后同步调用 | orderId, items: [{ skuId, quantity }] | 成功 / 失败（如任一 skuId 库存不足） |

- 成功：Inventory 扣减可用库存、记录占用，Order 继续后续流程（如发起支付）
- 失败：Order 回滚或不落库订单，直接返回「库存不足」等错误给用户

### 释放库存（Release）

| 调用方 | 时机 | 入参 | 返回值 |
|--------|------|------|--------|
| Order | 取消订单时，同步调用 | orderId | 成功（幂等：无占用记录时为空操作） |

---

## 四、事件契约

### Inventory 发布

| 事件 | 时机 | 载荷 |
|------|------|------|
| StockReserved（库存已占用） | occupy 成功 | orderId, items: [{ skuId, quantity }], occurredAt |
| StockReleased（库存已释放） | release 成功 | orderId, occurredAt |

### Kafka 发布（进程外可订阅）

领域事件在进程内发布后，同时发送至 Kafka，供其他应用（如读写分离、报表、审计）订阅。

| Topic | 事件 | 消息体（JSON） |
|-------|------|----------------|
| `inventory.stock.reserved` | StockReserved | eventType, orderId, items: [{ skuId, quantity }], occurredAt |
| `inventory.stock.released` | StockReleased | eventType, orderId, occurredAt |

- 配置：`application.yml` 中 `spring.kafka.bootstrap-servers`、`inventory.kafka.topic.*`
- 运行前需启动 Kafka（如 `docker compose -f infra/docker-compose.yml up -d`，含 Kafka 容器）

---

## 五、与 Order Saga 的对应

| Saga 步骤 | Order 动作 | Inventory 角色 |
|-----------|------------|----------------|
| T2（同步） | placeOrder 流程中调用 occupy | 提供 occupy 接口，同步返回成功/失败 |
| C2 | cancelOrder 流程中调用 release | 提供 release 接口，同步释放 |

---

## 六、幂等

- **occupy**：同一 orderId 重复调用时，若已存在该订单的占用记录，可视为幂等（返回成功，不重复扣减）
- **release**：同一 orderId 重复调用时，若无占用记录，为空操作，不报错
