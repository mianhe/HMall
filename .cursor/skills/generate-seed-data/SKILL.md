---
name: generate-seed-data
description: 生成或修复 Activity BC 的演示种子数据（SeedDataGenerator）。确保事件 payload 对齐 Ontology、业务流程因果链正确、userId 覆盖范围与真实管道一致。触发词：种子数据、seed data、生成测试数据、修复种子数据。
---

# 生成/修复种子数据

确保 `SeedDataGenerator` 生成的演示数据**忠实反映真实事件管道**，可作为智能运营分析的可靠数据源。

## 信任链

种子数据的正确性取决于三份权威文档的一致：

```
docs/intelligent-ops-ontology.md     ← Payload 字段定义（什么字段该有）
docs/business-process-architecture.md ← 因果链与流程（事件的先后关系）
EventMetadataRegistry.java           ← 已注册的事件类型（哪些事件合法）
```

任何修改种子数据的操作，必须先读取这三份文档，确认对齐后再改代码。

---

## 检查清单

修改 `SeedDataGenerator` 时，逐项核对：

### 1. 事件类型合法性

- [ ] 每个 `eventType` 都已在 `EventMetadataRegistry` 中注册
- [ ] 不存在虚假事件类型（如曾有的 `PaymentAttempted`）
- [ ] topic 与 eventType 对应正确（参考 `ActivityKafkaEventConsumer` 的 `@KafkaListener` 声明）

### 2. Payload 字段对齐 Ontology

对照 `intelligent-ops-ontology.md` 第二章「事件类型全表」的 Payload 字段列：

| 事件 | 必须包含的 payload 字段 |
|------|----------------------|
| OrderCreated | orderId, userId, totalAmountCents, items[]{skuId, spuId, quantity, unitPriceCents} |
| OrderCancelled | orderId, userId, totalAmountCents, items[] |
| OrderCompleted | orderId, userId, totalAmountCents, items[] |
| StockReserved | orderId, items[]{skuId, quantity} |
| StockReleased | orderId |
| PaymentCompleted | orderId, paymentId, amountCents |
| PaymentFailed | orderId |
| PaymentExpired | orderId |
| FulfillmentOrderCreated | orderId, fulfillmentOrderIds[] |
| FulfillmentOrderAllocated | orderId, fulfillmentOrderId |
| FulfillmentShipped | orderId, fulfillmentOrderId |
| FulfillmentDelivered | orderId, fulfillmentOrderId |
| EngravingCompleted | orderId, fulfillmentOrderId, completedAt |
| ServiceActivated | orderId, fulfillmentOrderId, serviceSkuId, activatedAt, expiresAt |

### 3. userId 覆盖范围（与真实管道一致）

- [ ] **Order BC 事件**（OrderCreated / OrderCancelled / OrderCompleted）：`BusinessActivity.userId` 有值
- [ ] **非 Order BC 事件**（Payment / Inventory / Fulfillment）：`BusinessActivity.userId` 为 **null**
- [ ] 使用两个不同的工厂方法区分（如 `orderEvent()` vs `nonOrderEvent()`）

### 4. 因果链（流程正确性）

对照 `intelligent-ops-ontology.md` 第三章「因果链」：

**Happy Path:**
```
OrderCreated → StockReserved → PaymentCompleted
  → FulfillmentOrderCreated → FulfillmentOrderAllocated
  → [EngravingCompleted?] → [ServiceActivated?]
  → FulfillmentShipped → FulfillmentDelivered → OrderCompleted
```

**Cancel Path (用户取消):**
```
OrderCreated → StockReserved → OrderCancelled → StockReleased
```

**Cancel Path (支付超时):**
```
OrderCreated → StockReserved → PaymentExpired → OrderCancelled → StockReleased
```

**Cancel Path (支付失败后超时):**
```
OrderCreated → StockReserved → PaymentFailed → PaymentExpired → OrderCancelled → StockReleased
```

关键规则：
- [ ] Happy path 中**不出现** StockReleased（StockReleased 是 COMPENSATION 事件，仅出现在补偿路径）
- [ ] PaymentExpired 后才有 OrderCancelled（因果链：超时→取消）
- [ ] PaymentFailed 不直接导致 StockReleased（用户可重试支付；最终走 PaymentExpired 路径）
- [ ] FulfillmentDelivered 后才有 OrderCompleted（因果链：签收→完成）
- [ ] EngravingCompleted 仅出现在有镭雕服务的订单（~10%）
- [ ] ServiceActivated 仅出现在有延保服务的订单（~10%）

### 5. 镭雕与延保（增值服务）

镭雕和延保是两种**独立的**增值服务，各自 ~10% 的订单命中：

- [ ] `hasEngraving` 和 `hasWarranty` 独立采样（约 1% 同时命中）
- [ ] 镭雕订单：items 第一项含 `serviceAttributes`（engravingPatternId/Name/Text），生成 `EngravingCompleted` 事件
- [ ] 延保订单：items 第一项含 `warrantyAttributes`（warrantyName/Duration），生成 `ServiceActivated` 事件
- [ ] 两者可同时出现在同一订单中

### 6. correlationKeys

- [ ] 含 items 的事件同时设置 `correlationKeys`（JSON：`{"spuIds":[...],"skuIds":[...]}`）
- [ ] 不含 items 的事件 correlationKeys 可为 null 或沿用上游

### 7. 数据合理性

- [ ] 事件时间严格递增（同一订单内后续事件的 occurredAt > 前一个）
- [ ] 金额一致：OrderCreated 的 totalAmountCents = items 各行 quantity × unitPriceCents 之和
- [ ] PaymentCompleted 的 amountCents 与同一订单的 totalAmountCents 一致
- [ ] 近期订单可能未走完全流程（如今天的订单可能还在配送中，没有 Delivered/Completed）
- [ ] ~25% 订单有多商品行（2-3 个 items），支持多 spuId/skuId 的 correlationKeys

---

## 数据量规划

API 参数：`POST /api/activities/seed?days=<天数>&ordersPerDay=<每日均值>&maxOrders=<上限>`

| 场景 | days | ordersPerDay | maxOrders | 预估订单 | 预估事件 |
|------|------|--------------|-----------|---------|---------|
| 快速演示 | 7 | 5 | 0 | ~35 | ~280 |
| 标准演示 | 30 | 5 | 0 | ~150 | ~1200 |
| 完整演示 | 90 | 5 | 400 | ~400 | ~3200 |

订单流程比例：~85% happy path, ~8% 用户取消, ~4% 支付超时, ~3% 支付失败后超时

---

## 新增事件类型时的扩展步骤

当 Ontology 新增事件类型后，种子数据需要同步更新：

1. 读取 `intelligent-ops-ontology.md` 确认新事件的 payload 字段
2. 读取 `EventMetadataRegistry.java` 确认新事件已注册
3. 确定新事件在哪条流程路径中出现
4. 在 `SeedDataGenerator` 的对应路径中插入新事件
5. 按上方检查清单核对

---

## 验证

修改后运行：

```bash
cd services/activity-service && mvn test -q
```

全绿后，可通过 API 验证种子数据质量：

```bash
# 清除旧数据
curl -X DELETE 'http://localhost:8086/api/activities'

# 生成种子数据（3个月、约400订单）
curl -X POST 'http://localhost:8086/api/activities/seed?days=90&ordersPerDay=5&maxOrders=400'

# 验证：检查 OrderCompleted 事件的 payload 是否完整
curl -s 'http://localhost:8086/api/activities?orderId=<某orderId>&limit=20' | jq '.[].payload | fromjson'

# 验证：检查 distinctBuyerCount 是否 > 0
curl -s 'http://localhost:8086/api/activities/stats?period=last30' | jq '.distinctBuyerCount'

# 验证：检查镭雕和延保事件各约占 10%
curl -s 'http://localhost:8086/api/activities?limit=5000' | jq '[.[] | .eventType] | group_by(.) | map({type: .[0], count: length})'
```
