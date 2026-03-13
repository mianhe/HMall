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
- ✅ 1.3 收到含 **userId** 的事件后，BusinessActivity 的 userId 字段正确填充（来自业务需求 [智能运营 Step 1](../../business-requirements/intelligent-ops-step1/overview.md)）
- ✅ 1.4 收到含 **items** 的事件后，从 items 提取 spuId/skuId 去重，correlationKeys 正确落库为 JSON `{"spuIds":[...],"skuIds":[...]}`

> **镭雕服务**（来自业务需求 [镭雕服务](../../business-requirements/laser-engraving/overview.md)）：🔲 若 Fulfillment 发布 EngravingCompleted 事件，Activity 订阅 `fulfillment.engraving.completed`，订单旅程可展示「镭雕已完成」节点。新增事件时在 Examples 表增加一行，并注册 event-metadata。

**测试策略**：用 **Scenario Outline + Examples** 覆盖当前已订阅事件类型；新增事件时在 Examples 表增加一行即可，不新增 Scenario。

## 2. 活动查询

`activity-query.feature`

- ✅ 2.1 按 orderId 查询：同一订单的多条事件按 occurredAt **正序**返回（事件时间线）
- ✅ 2.2 按 orderId 查询：orderId 不存在时返回空列表
- ✅ 2.3 查询最近活动：跨所有订单，按 occurredAt **倒序**返回，默认 limit=20
- ✅ 2.4 活动记录中嵌入事件元数据：每条 ActivityDto 附带 `metadata` 字段（boundedContext、label、category、compensatesEventType、**origin、processRoles**），以及 **userId** 字段（可空）；前端直接使用，无需硬编码（来自 [智能运营 Step 1](../../business-requirements/intelligent-ops-step1/overview.md)）
- ✅ 2.5 查询事件元数据列表：`GET /api/activities/event-metadata` 返回所有已注册事件类型的元数据（含 **origin、processRoles**），供前端初始化使用
- ✅ 2.6 按 **userId** 查询：`GET /api/activities?userId=...` 返回该用户关联的所有事件，按 occurredAt 正序
- ✅ 2.7 按 **spuId** 查询：`GET /api/activities?spuId=...` 返回 correlationKeys 中含该 SPU 的所有事件
- ✅ 2.8 按 **skuId** 查询：`GET /api/activities?skuId=...` 返回 correlationKeys 中含该 SKU 的所有事件

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
| | 成功支付金额 | 统计周期内成功支付总金额（分） | PaymentCompleted.amountCents |
| **履约** | 履约单创建数 | 统计周期内创建的履约单数 | FulfillmentOrderCreated |
| | 开始配货数 | 统计周期内开始配货的履约单数 | FulfillmentOrderAllocated |
| | 已发货数 | 统计周期内发货的履约单数 | FulfillmentShipped |
| | 已签收数 | 统计周期内签收的履约单数 | FulfillmentDelivered |
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

---

## 4. 订单旅程回放

`order-journey.feature`（手工验收为主，后端无新增 API）

面向**产品经理、开发者、业务人员**的可视化功能：通过回放一笔真实订单的领域事件序列，直观展示交易全生命周期——包括正常路径与 Saga 补偿路径。

### 4.1 目标与受众

| 维度 | 说明 |
|------|------|
| **目标** | 让非技术/半技术人员理解系统的交易流程、跨 BC 协作方式、以及异常情况下的补偿机制 |
| **受众** | 产品经理、业务人员、新加入的开发者 |
| **入口** | `frontend/admin` 活动监控页（ActivityPage）新增"订单旅程"入口 |
| **数据来源** | 现有 `GET /api/activities?orderId={id}` 返回的事件列表（已按 occurredAt 正序），无需新增后端 API |

### 4.2 核心设计决策

| # | 决策 | 说明 |
|---|------|------|
| A1 | 纯前端实现，不新增后端 API | 现有 `GET /api/activities?orderId={id}` 已返回完整事件序列（含 payload），前端按 eventType 分组、标注颜色、推断因果关系即可 |
| A2 | 事件按 BC 泳道分组 | 纵轴分为 Order / Inventory / Payment / Fulfillment 四条泳道，横轴为时间轴，直观展示跨 BC 协作 |
| A3 | 正向事件与补偿事件用颜色区分 | 正向事件（OrderCreated、StockReserved、PaymentCompleted 等）用蓝/绿色；补偿事件（OrderCancelled、StockReleased）用红/橙色 |
| A4 | 因果关系用连线表达 | 关键因果关系用箭头连线（如 PaymentCompleted → FulfillmentOrderCreated，PaymentExpired → OrderCancelled → StockReleased）|
| A5 | 渐进式实现：先分组时间线，再泳道图 | 第一步实现增强版分组时间线（低成本、效果好），后续可迭代为泳道式可视化 |

### 4.3 事件分类与展示

#### 4.3.1 事件归属 BC

| BC | 事件类型 | 中文标签 | 性质 |
|----|----------|----------|------|
| **Order** | `OrderCreated` | 订单创建 | 正向 |
| | `OrderCancelled` | 订单取消 | 补偿 |
| | `OrderCompleted` | 订单完成 | 正向（终态） |
| **Inventory** | `StockReserved` | 库存锁定 | 正向 |
| | `StockReleased` | 库存释放 | 补偿 |
| **Payment** | `PaymentCompleted` | 支付成功 | 正向 |
| | `PaymentFailed` | 支付失败 | 异常（非补偿，可重试） |
| | `PaymentExpired` | 支付超时 | 异常（触发补偿） |
| **Fulfillment** | `FulfillmentOrderCreated` | 履约单创建 | 正向 |
| | `FulfillmentOrderAllocated` | 开始配货 | 正向 |
| | `FulfillmentShipped` | 已发货 | 正向 |
| | `FulfillmentDelivered` | 已签收 | 正向（终态） |
| | 🔲 `ServiceActivated` | 服务已激活 | 正向（虚拟服务终态，来自业务需求 [虚拟商品](../../business-requirements/virtual-product/overview.md)） |

#### 4.3.2 典型路径模板

**正常路径（Happy Path）**：

```
OrderCreated → StockReserved → PaymentCompleted → FulfillmentOrderCreated → FulfillmentOrderAllocated → FulfillmentShipped → FulfillmentDelivered → OrderCompleted
```

**支付超时补偿路径**：

```
OrderCreated → StockReserved → PaymentExpired → OrderCancelled → StockReleased
```

**用户取消补偿路径**：

```
OrderCreated → StockReserved → OrderCancelled → StockReleased
```

**支付失败（可重试）路径**：

```
OrderCreated → StockReserved → PaymentFailed → ... → PaymentCompleted → ...（继续正常路径）
```

#### 4.3.3 补偿关系（Saga Compensating Transactions）

| 正向事务 | 补偿事务 | 触发条件 |
|---------|---------|---------|
| `OrderCreated` | `OrderCancelled` | PaymentExpired / 用户主动取消 |
| `StockReserved` | `StockReleased` | OrderCancelled 后同步释放 |

### 4.4 交互方案

#### 第一阶段：增强版分组时间线（MVP）

在 ActivityPage 中新增入口，输入 orderId 后展示：

1. **顶部概要**：orderId、当前状态（根据最后一个事件推断）、事件总数、时间跨度
2. **分组垂直时间线**：
   - 事件按 occurredAt 正序，从上到下排列
   - 每个事件节点包含：BC 标签（彩色 badge）、事件名（中文）、时间戳
   - BC 颜色编码：Order=蓝色、Payment=绿色、Inventory=琥珀色、Fulfillment=靛蓝色
   - 补偿事件用红色/橙色背景突出
   - 点击事件节点可展开 payload 详情（金额、SKU、履约单号等）
3. **路径标注**：
   - 正常路径事件之间用实线连接
   - 补偿事件用虚线标注"补偿"关系（如 StockReleased ← 补偿 → StockReserved）

#### 第二阶段：泳道式可视化（后续迭代）

将时间线升级为横向泳道图：
- 纵轴：Order / Inventory / Payment / Fulfillment 四条泳道
- 横轴：时间轴
- 事件节点在对应泳道上，跨泳道因果关系用箭头连接
- 可选：回放动画（事件按时间逐个出现）

### 4.5 需求场景

| # | Scenario | 验收要点 | 状态 | 阶段 |
|---|----------|----------|------|------|
| 4.1 | 输入 orderId 查看订单旅程 | ActivityPage 新增入口，输入 orderId 后调用 `GET /api/activities?orderId={id}`，展示事件时间线 | ✅ | MVP |
| 4.2 | 事件按 BC 分组，颜色区分 | 每个事件显示 BC 标签（Order/Inventory/Payment/Fulfillment），使用 BC 专属颜色 | ✅ | MVP |
| 4.3 | 补偿事件高亮标注 | 补偿事件（OrderCancelled、StockReleased）使用红/橙色样式，与正向事件视觉区分 | ✅ | MVP |
| 4.4 | 事件详情可展开 | 点击事件节点可展开 payload 关键字段（支付金额、SKU 明细、履约单号等） | ✅ | MVP |
| 4.5 | 正常路径订单展示完整旅程 | 成功订单展示 OrderCreated → ... → OrderCompleted 全链路，所有事件正确排序 | ✅ | MVP |
| 4.6 | 补偿路径订单展示补偿过程 | 取消/超时订单展示补偿事件（OrderCancelled、StockReleased），补偿关系可识别 | ✅ | MVP |
| 4.7 | 顶部概要信息 | 展示 orderId、推断状态、事件数量、时间跨度（首尾事件间隔） | ✅ | MVP |
| 4.8 | 无事件时提示 | orderId 无对应事件时显示空状态提示 | ✅ | MVP |
| 4.9 | 泳道式可视化 | 事件按 BC 分布在横向泳道上，跨泳道因果关系用箭头连接；支持时间线/泳道图视图切换 | ✅ | V2 |
| 4.10 | 回放动画 | 事件按时间顺序逐个出现；支持播放/暂停/显示全部/速度调节（0.5x-4x） | ✅ | V2 |

### 4.6 后端与数据可行性

| 能力 | 现有支持 | 结论 |
|------|---------|------|
| 按 orderId 查事件列表 | `GET /api/activities?orderId={id}`，occurredAt 正序 | ✅ 直接使用 |
| 事件 payload 含业务字段 | payload 为完整 JSON（amountCents、items、fulfillmentOrderId 等） | ✅ 前端解析展示 |
| 事件覆盖交易全生命周期 | 12 种事件，覆盖 Order/Payment/Inventory/Fulfillment | ✅ 无遗漏 |
| 补偿事件有记录 | StockReleased、OrderCancelled 均有 orderId 关联 | ✅ 可展示补偿路径 |
| MCP 工具 | `activity_query`（action=list, orderId）| ✅ AI 对话中也可查 |

**结论**：后端数据层完全就绪，不需要新增 API 或修改领域模型。核心工作量在前端可视化实现。
