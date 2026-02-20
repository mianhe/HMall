# HMall 项目状态

> 本文件记录项目当前进度、路线图与关键决策。每完成一个阶段后更新。BC 间关系与集成方式见 [context-map.md](context-map.md)。

---

## 一、项目愿景

HMall 是一个以 DDD + ATDD 驱动的电商系统练习项目，覆盖商品、用户、购物车、订单、支付、履约等电商核心域，前后端完整实现。

---

## 二、BC 路线图与进度

### 推进顺序

```
Catalog ✅ → User ✅ → Order ✅ → Inventory ✅ → Payment 🔄 → Activity ✅ → Fulfillment 🔲 → [Pricing / Cart 按需]
```

### 各 BC 状态

| BC | 职责 | 状态 | 说明 |
|----|------|------|------|
| **Catalog** | 类目、商品(SPU)、规格维度、SKU、展示图 | ✅ 已完成 | 4 个 feature，45 个 scenario，全部通过 |
| **User** | 用户注册、登录(JWT)、收货地址管理 | ✅ 已完成 | 3 个 feature（user、login、address），19 个 scenario，全部通过 |
| **Order** | 订单创建、取消、查询、事件驱动、状态流转 | ✅ 已完成 | 4 个 feature，23 个 scenario，全部通过 |
| **BFF** | frontend 统一 API 入口，代理 Catalog/User/Order/Inventory | ✅ POC 完成 | 透传代理、CORS、4xx/5xx 转发，端口 8085 |
| **Inventory** | 同步占用/释放库存、库存管理 | ✅ 已完成 | 3 feature、11 scenario 全绿；已与 Order 集成 |
| **Payment** | 扣款/退款/超时检测 | 🔄 开发中 | 5 feature、19 scenario 全绿；超时检测定时自动执行；事件通知 Order（Kafka：PaymentCompleted/Failed/Expired） |
| **Activity** | 消费 Order/Payment/Inventory 事件，活动记录、查询与统计仪表盘 | ✅ 已完成 | 3 个 feature（consume/query/stats），16 个 scenario，全部通过 |
| **Fulfillment** | 拆单、发货、配送 | 🔲 规划中 | 依赖 Order，目前 Order 以 Port 桩对接 |
| **Pricing** | 算价、优惠 | 🔲 规划中 | 创建订单时同步调用 |
| **Cart** | 购物车增删改查、结算预览 | 🔲 需求已完成 | 5 feature、17 scenario（待实现）；依赖 Catalog + User，结算由前端编排 |

### 已完成：Order 与 Inventory 集成

Order 通过 `RestOccupyInventoryAdapter`、`RestReleaseInventoryAdapter` 调用 Inventory 的 `POST /api/inventory/occupy`、`POST /api/inventory/release`。验收测试用 Stub，集成测试用 WireMock 验证 HTTP 调用。详见 `services/order-service/README.md`。

### 下一步

1. **Cart BC 实现**：需求与领域模型已完成（5 feature、17 scenario），待创建骨架并实现。
2. **Payment 集成**：Order → Payment 同步创建支付单/退款，当前用 NoOp 桩。
3. **Kafka 事件**：Order、Inventory、Payment 的领域事件默认不发 Kafka（排除了 KafkaAutoConfiguration），加 `--spring.profiles.active=kafka` 可启用。Payment 已实现 Kafka 发布（PaymentCompleted/Failed/Expired），Order 已实现 Kafka 消费。
4. **Activity BC 前端**：frontend-admin 统计仪表盘页面（可选）。

---

## 三、前端进度

| 前端 | 职责 | 状态 | 已实现页面 |
|------|------|------|-----------|
| **frontend-admin** | 管理后台，展示+库存管理 | ✅ 基本完成 | HomePage、CatalogPage、ProductDetailPage、InventoryPage |
| **frontend-web** | 消费者端 | ✅ 阶段完成 | HomePage、LoginPage、RegisterPage、ProductDetailPage、CheckoutPage、OrderListPage、OrderDetailPage、AddressPage、MyPage |

### frontend-web 已实现

- **Order 交易流程**：立即购买 → 结账页（选地址/新增地址、订单确认）→ 提交订单 → 模拟支付 → 订单列表/详情（支持按状态筛选）
- **收货地址管理**：地址列表、新增、编辑、删除；结账页可选已保存地址
- **「我的」聚合页**：用户信息块、收货地址入口、我的订单块（待付款/待收货/待评价）；Atomic Design（atoms/molecules/organisms）
- **文档**：`docs/frontend-web/design-input.md`、`docs/design-principles.md` 前端节、frontend-development Skill 与 Design Input 定位已整理

---

## 四、MCP Server 进度

| 模块 | 状态 | 说明 |
|------|------|------|
| Catalog MCP tools | ✅ 已完成 | 类目/商品/规格/SKU/展示图（ProductImage、OptionImage）CRUD 及图片上传 |
| User MCP tools | 🔲 待实现 | — |

---

## 五、关键决策记录

| # | 决策 | 原因 | 日期 |
|---|------|------|------|
| 1 | 先跑通交易流程，Inventory 优先于 Cart | Order 已完成；优先 Inventory→Payment→Fulfillment 跑通主流程，Cart 按需 | 2026-02-14 |
| 2 | 先做 Order 前端，延后 Inventory/Payment/Fulfillment 后端 | Order 后端已完成且以 Port 桩对接下游；先打通消费者端交易 UI 体验，支付/履约用模拟，后端后续补 | 2025-02-13 |
| 3 | User 地址簿推迟到 Order 前实现 | 创建订单需要收货地址，但 Cart 阶段暂不需要 | 2025-02-12 |
| 4 | Inventory 采用同步占用而非事件驱动 | 业务合理性：用户下单需即时获知库存结果；行业惯例为同步预占 | 2026-02-15 |
| 5 | Payment 采用同步调用 + Kafka 事件 | Order 同步调用创建支付单/退款；Payment 通过 Kafka 发布 PaymentCompleted/Failed/Expired，Order 消费事件驱动状态流转 | 2026-02-19 |
| 6 | Cart 不快照价格，结算由前端编排 | 购物车展示时实时拉取 Catalog 价格；结算时前端从 Cart 取选中项 → Order API 创建订单，复用 CheckoutPage | 2026-02-20 |

---

## 六、变更日志

| 日期 | 变更内容 |
|------|---------|
| 2026-02-20 | Cart BC 需求分析与领域建模完成（requirements.md + domain-model.md）；5 feature、17 scenario 待实现 |
| 2026-02-19 | Activity BC 全部完成（consume/query/stats 三个 feature，16 scenario）；eventId 幂等、orderId 可空查询维度、统计仪表盘 API |
| 2026-02-19 | Activity BC 纳入路线图与状态表；需求与契约已对齐 Order/Payment/Inventory 事件，准备开发 |
| 2026-02-19 | Payment→Order 全面切换到 Kafka 事件；移除 Spring 进程内事件、HTTP 回调、internal API；Payment 测试改用 stub 替身，Order 测试直接调用 OrderEventService |
| 2026-02-17 | Order–Inventory 集成完成（适配器 + 集成测试 + BFF 4xx 转发 + Kafka 默认排除保证无 Kafka 时可用） |
| 2026-02-16 | Inventory BC 完成（占用/释放/库存管理，11 scenario）；frontend-admin 库存管理页（平铺表格+过滤） |
| 2026-02-15 | Order 同步占用与支付、Payment/Inventory 方案落定（决策#4/#5）；frontend-web 阶段完成 |
| 2026-02-14 | Order BC 全部 feature 完成，23 scenario |
| 2026-02-12 | Catalog BC 新增展示图(OptionImage) |
| 2025-02-13 | BFF 创建，frontend 经 BFF 代理 |
| 2025-02-12 | 项目初始化；Catalog、User BC 已完成 |
