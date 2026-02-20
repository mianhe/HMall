# Activity 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。契约：`docs/bounded-contexts/activity/api.yaml`。Feature 目录：`services/activity-service/src/test/resources/features/activity/`。领域模型见 [domain-model.md](./domain-model.md)。

### 状态图例

- ✅ 已实现
- 🔄 部分完成
- 🔲 待实现

---

## 1. 事件消费与记录

`activity-consume.feature`

订阅的领域事件由各 BC 的 event-flow 与 Kafka topic 约定，**不在此穷举**。随业务流程演进事件会增多，验收按「行为」不按「逐个事件」。

- ✅ 1.1 收到**任一已订阅的领域事件**后应记录一条业务活动（eventId、eventType、topic、orderId、occurredAt、payload、receivedAt 正确落库）
- ✅ 1.2 重复事件（同一 eventId）不应重复记录

**测试策略**：用 **Scenario Outline + Examples** 覆盖当前已订阅事件类型；新增事件时在 Examples 表增加一行即可，不新增 Scenario。

## 2. 活动查询

`activity-query.feature`

- ✅ 2.1 按 orderId 查询：同一订单的多条事件按 occurredAt **正序**返回（事件时间线）
- ✅ 2.2 按 orderId 查询：orderId 不存在时返回空列表
- ✅ 2.3 查询最近活动：跨所有订单，按 occurredAt **倒序**返回，默认 limit=20

## 3. 统计与仪表盘

`activity-stats.feature`

基于事件聚合的统计，支持**时间范围**查询（今日、最近 N 天、或自定义起止日期），用于仪表盘展示。

**通用约定**：

- 时间范围筛选基于 **occurredAt**（事件发生时间）
- 时间均以**服务器本地日期**为准
- `from/to` 与 `period` 二选一；同时传入时 `from/to` 优先；都不传默认 `period=today`
- 无数据时所有指标返回 **0**

### 3.1 统计指标设计

| 分类 | 指标 | 说明 | 事件来源 |
|------|------|------|----------|
| **订单** | 已开出订单总数 | 统计周期内创建的订单数 | OrderCreated |
| | 已取消订单数 | 统计周期内取消的订单数 | OrderCancelled |
| | 已完成订单数 | 统计周期内配送完成的订单数 | OrderCompleted |
| **支付** | 支付尝试总数 | = 成功 + 失败 + 过期（派生字段） | PaymentCompleted + Failed + Expired |
| | 成功支付数 | 支付成功笔数 | PaymentCompleted |
| | 支付失败数 | 网关返回失败 | PaymentFailed |
| | 支付过期数 | 超时未支付 | PaymentExpired |
| **库存（可选）** | 库存占用次数 | 占库成功次数 | StockReserved |
| | 库存释放次数 | 释放次数 | StockReleased |

### 3.2 仪表盘示意图

仪表盘为**单页概览**：顶部选择时间范围，下方为卡片式指标区。

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  Activity 统计仪表盘                          [今日 ▼] [应用]                  │
│  可选：今日 | 最近7天 | 最近30天 | 自定义开始日期 - 结束日期                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─ 订单概览 ──────────────────┐  ┌─ 支付概览 ──────────────────────────┐   │
│  │  已开出订单总数        1,234 │  │  支付尝试总数            1,180      │   │
│  │  已取消订单数            89  │  │  成功支付数              1,050      │   │
│  │  已完成订单数        1,020   │  │  支付失败数                 12      │   │
│  │                              │  │  支付过期数                 18      │   │
│  └──────────────────────────────┘  └─────────────────────────────────────┘   │
│  ┌─ 库存活动（可选）────────────────────────────────────────────────────┐   │
│  │  库存占用次数        1,234  │  库存释放次数                89         │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **订单概览**：来自 Order 事件，反映下单与履约情况。
- **支付概览**：来自 Payment 事件，反映支付成功率与失败/过期占比。
- **库存活动**：来自 Inventory 事件，用于监控占用与释放是否均衡（可选首版不做）。

前端可调用 `GET /api/activities/stats?from=...&to=...`（或 `period=today|last7|last30`）获取当前时间范围内的聚合数据，按上述卡片布局渲染。

### 3.2.1 后端 API 与仪表盘对照

| 仪表盘能力 | API | 结论 |
|------------|-----|------|
| 时间范围选择（今日/最近7天/最近30天） | `GET /api/activities/stats?period=today\|last7\|last30` | ✅ 已支持 |
| 自定义起止日期 | `GET /api/activities/stats?from=YYYY-MM-DD&to=YYYY-MM-DD` | ✅ 已支持 |
| 订单概览三指标 | StatsDto：ordersCreated, ordersCancelled, ordersCompleted | ✅ 已支持 |
| 支付概览四指标 | StatsDto：paymentAttempts, paymentSuccess, paymentFailed, paymentExpired | ✅ 已支持 |
| 库存活动二指标 | StatsDto：stockReserved, stockReleased | ✅ 已支持 |
| 展示当前统计区间 | StatsDto：from, to（响应回显） | ✅ 已支持 |

**结论**：当前 Activity 后端契约（`api.yaml`）已覆盖仪表盘所需的全部统计与时间范围能力，前端可直接基于 `GET /api/activities/stats` 与 StatsDto 实现单页仪表盘。若仪表盘后续增加「最近活动」流水，可使用现有 `GET /api/activities/recent`。

### 3.3 需求场景（待实现）

| # | Scenario | 验收要点 | 状态 |
|---|----------|----------|------|
| 3.3.1 | 查询今日统计 | 不传参数或 `period=today` 时，返回当日各指标（基于 occurredAt）；paymentAttempts = success + failed + expired；无数据时各指标为 0 | ✅ |
| 3.3.2 | 按起止日期查询统计 | `from` + `to` 传入时，返回该时间范围内的聚合指标，响应中回显 from/to | ✅ |
| 3.3.3 | 按快捷周期查询统计 | `period=last7` / `period=last30` 时，返回最近 7 天 / 30 天的聚合指标 | ✅ |
