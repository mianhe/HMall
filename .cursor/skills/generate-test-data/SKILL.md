---
name: generate-test-data
description: 通过自然语言交互生成、查询、删除 Activity BC 的测试数据。支持指定环境（本地/阿里云）、时间范围、数据量、异常比例等。触发词：生成测试数据、生成事件数据、删除测试数据、测试数据批次、seed data。
---

# 生成/管理测试数据

通过自然语言与用户交互，将需求转化为 Activity Service 的 seed API 调用。

> **与 `generate-seed-data` Skill 的区别**：本 Skill 用于**交互式操作数据**（生成/查询/删除）；`generate-seed-data` 用于**维护 SeedDataGenerator 代码**（对齐 Ontology、修复因果链）。

---

## 环境

| 环境名 | Base URL | 说明 |
|--------|----------|------|
| 本地 | `http://localhost:8086` | 本地开发环境，直连 activity-service |
| 阿里云 | `http://47.115.230.90` | 生产部署环境（Nginx → BFF → activity-service） |

**默认使用本地环境。** 当用户提到"线上"、"阿里云"、"服务器"、"生产"时切换到阿里云环境。

两个环境的 API 路径格式一致：`{base}/api/activities/...`。

---

## 操作一览

| 操作 | API | 说明 |
|------|-----|------|
| 生成测试数据 | `POST {base}/api/activities/seed` | JSON Body |
| 查询已有批次 | `GET {base}/api/activities/seed/batches` | 返回摘要列表 |
| 按批次删除 | `DELETE {base}/api/activities/seed?batchTag=xxx` | 仅删该批次 |
| 按时间段删除 | `DELETE {base}/api/activities/seed?from=yyyy-MM-dd&to=yyyy-MM-dd` | 仅删范围内种子数据 |
| 删除所有测试数据 | `DELETE {base}/api/activities/seed` | 仅删 seedBatch 非空的 |
| 删除全部数据（含真实） | `DELETE {base}/api/activities` | ⚠️ 危险，需用户明确要求 |

---

## 自然语言 → 参数映射

### 生成请求体 (POST /api/activities/seed)

```json
{
  "startDate": "2025-01-01",
  "endDate": "2025-06-30",
  "ordersPerDay": 5,
  "maxOrders": 0,
  "batchTag": "demo-2025-h1",
  "cancelRatio": 0.08,
  "paymentExpiredRatio": 0.04,
  "paymentFailedRatio": 0.03,
  "engravingRatio": 0.10,
  "warrantyRatio": 0.10,
  "multiItemRatio": 0.25
}
```

所有 ratio 字段和 batchTag 均可省略（使用默认值）。

### 映射规则

| 用户表述 | 映射目标 | 示例 |
|---------|---------|------|
| "从1月到3月" / "2025年第一季度" | startDate + endDate | `"2025-01-01"`, `"2025-03-31"` |
| "最近三个月" | endDate=今天, startDate=今天-90天 | 计算具体日期 |
| "每天10单" | ordersPerDay | `10` |
| "总共生成500单" | maxOrders | `500` |
| "异常数据占20%" | cancelRatio + paymentExpiredRatio + paymentFailedRatio | 按 8:4:3 比例分配到 20% |
| "多一些取消订单" | 提高 cancelRatio | 如 `0.15` |
| "取消10% 支付超时5% 支付失败3%" | 分别设置三个 ratio | 直接对应 |
| "镭雕订单占20%" | engravingRatio | `0.20` |
| "延保订单多一些" | warrantyRatio | 如 `0.20` |
| "给这批数据打标 q1-test" | batchTag | `"q1-test"` |

### "异常数据"比例分配

当用户说"异常数据占 X%"时，将 X 按默认 8:4:3 比例分配：

- cancelRatio = X × 8/15
- paymentExpiredRatio = X × 4/15
- paymentFailedRatio = X × 3/15

---

## 交互协议

### 生成数据时

1. **确认环境**：用户未指定时，询问"在本地还是阿里云环境生成？"
2. **确认时间范围**：必须明确 startDate 和 endDate
3. **确认数据量**：ordersPerDay 或 maxOrders 至少确认一个
4. **可选参数**：ratio 类参数用户未提则用默认值，不必逐一询问
5. **执行并报告**：调用 API，展示结果（订单数、事件数、时间范围、batchTag）

### 删除数据时

1. **确认环境**
2. **确认删除范围**：按批次 / 按时间段 / 所有测试数据
3. **二次确认**：对"删除全部数据（含真实）"必须明确用户意图
4. **执行并报告**

### 查询批次时

直接调用 `GET /seed/batches`，以表格形式展示结果。

---

## 执行示例

### 示例 1：用户说"在本地生成2025年1月到3月的数据，每天5单"

```bash
curl -s -X POST 'http://localhost:8086/api/activities/seed' \
  -H 'Content-Type: application/json' \
  -d '{"startDate":"2025-01-01","endDate":"2025-03-31","ordersPerDay":5}'
```

### 示例 2：用户说"查看阿里云上有哪些测试数据批次"

```bash
curl -s 'http://47.115.230.90/api/activities/seed/batches'
```

### 示例 3：用户说"删除批次 demo-q1 的数据"

```bash
curl -s -X DELETE 'http://localhost:8086/api/activities/seed?batchTag=demo-q1'
```

### 示例 4：用户说"把阿里云上的测试数据全部清掉"

```bash
curl -s -X DELETE 'http://47.115.230.90/api/activities/seed'
```

---

## 安全约束

- `DELETE /api/activities/seed`（无参数）只删除 `seedBatch IS NOT NULL` 的记录，不影响真实数据
- `DELETE /api/activities`（删全部）是危险操作，仅在用户明确要求"清空所有数据包括真实数据"时使用
- 对阿里云环境的删除操作，执行前给予额外提醒

---

## OrderFact 自动投影

`POST /api/activities/seed` 在生成种子数据后会**自动调用 OrderFact 批量重建**（`rebuildOrderFacts()`），无需手动触发 `POST /api/order-facts/rebuild`。响应中包含 `orderFactsRebuilt` 字段，表示重建的订单数。

删除种子数据后，OrderFact 数据不会自动清理。若需清理，可手动调用：

```bash
# 清理：删除种子数据后重建（会自动排除已删除的事件）
curl -X POST '{base}/api/order-facts/rebuild'
```

## 注意事项

- 阿里云环境需要服务已部署且 activity-service 可访问
- 如果 curl 调用失败（连接拒绝、超时），提示用户检查服务状态
- batchTag 自动生成格式：`seed-{日期}-{6位随机}`，用户也可自定义
