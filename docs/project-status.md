# HMall 项目状态

> 本文件记录项目当前进度、路线图与关键决策。每完成一个阶段后更新。BC 间关系与集成方式见 [context-map.md](context-map.md)。

---

## 一、项目愿景

HMall 是一个以 DDD + ATDD 驱动的电商系统练习项目，覆盖商品、用户、购物车、订单、支付、履约等电商核心域，前后端完整实现。

---

## 二、BC 路线图与进度

### 推进顺序

```
Catalog ✅ → User ✅ → Order ✅ → Inventory ✅ → Payment 🔲 → Fulfillment 🔲 → [Pricing / Cart 按需]
```

### 各 BC 状态

| BC | 职责 | 状态 | 说明 |
|----|------|------|------|
| **Catalog** | 类目、商品(SPU)、规格维度、SKU、展示图 | ✅ 已完成 | 4 个 feature，45 个 scenario，全部通过 |
| **User** | 用户注册、登录(JWT)、收货地址管理 | ✅ 已完成 | 3 个 feature（user、login、address），19 个 scenario，全部通过 |
| **Order** | 订单创建、取消、查询、事件驱动、状态流转 | ✅ 已完成 | 4 个 feature，23 个 scenario，全部通过 |
| **BFF** | frontend-admin、frontend-web 统一 API 入口，代理 Catalog/User/Order | ✅ POC 完成 | 透传代理、CORS，端口 8085 |
| **Inventory** | 同步占用/释放库存、库存管理 | ✅ 已完成 | 3 feature、11 scenario 全绿 |
| **Payment** | 扣款/退款/超时检测 | 🔲 规划中 | 依赖 Order，目前 Order 以 Port 桩对接 |
| **Fulfillment** | 拆单、发货、配送 | 🔲 规划中 | 依赖 Order，目前 Order 以 Port 桩对接 |
| **Pricing** | 算价、优惠 | 🔲 规划中 | 创建订单时同步调用 |
| **Cart** | 购物车管理 | 🔲 规划中 | 依赖 Catalog + User，按需实现 |

### 当前：Inventory BC（库存）

**Inventory BC 已完成**：`inventory-service`、占用/释放/库存管理（PUT·GET `/api/inventory/stock/{skuId}`），11 个 scenario 全绿。下一步：Order 对接释放、Payment/Fulfillment。

---

## 三、前端进度

| 前端 | 职责 | 状态 | 已实现页面 |
|------|------|------|-----------|
| **frontend-admin** | 管理后台，展示为主 | ✅ 基本完成 | HomePage、CatalogPage（类目→商品→SKU 分层展示）、ProductDetailPage；API 经 BFF 代理 |
| **frontend-web** | 消费者端 | ✅ 阶段完成 | HomePage、LoginPage、RegisterPage、ProductDetailPage、CheckoutPage、OrderListPage、OrderDetailPage、AddressPage、**MyPage（我的）**；API 经 BFF 代理 |

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
| 5 | Payment 采用同步调用+回调 | Order 同步调用创建支付单；支付完成由网关回调 Payment，Payment 发布 PaymentCompleted 等事件 | 2026-02-15 |

---

## 六、变更日志

| 日期 | 变更内容 |
|------|---------|
| 2026-02-16 | Inventory 库存管理完成：inventory-stock.feature 2 scenario、GET/PUT /stock/{skuId}；BC 共 3 feature、11 scenario 全绿 |
| 2026-02-16 | Inventory 释放库存功能完成：inventory-release.feature 4 scenario、POST /release、StockReleased 事件；共 2 feature、9 scenario |
| 2026-02-16 | Inventory 占用库存功能完成：inventory-service、inventory-occupy.feature 5 scenario、api.yaml、领域模型与事件发布 |
| 2026-02-16 | Project Status 与 Context Map 对齐；Order 23 scenario；Inventory 文档就绪，可进入开发 |
| 2026-02-15 | Order 实现同步占用/支付，删除 InventoryReserved/Failed 事件，合并冗余 scenario，23 scenario |
| 2026-02-15 | Payment 改为同步调用+回调：Order 同步创建支付；支付完成由网关回调；决策#5 |
| 2026-02-15 | Inventory 改为同步占用：更新 Inventory/Order/context-map 等文档；决策#4 |
| 2026-02-15 | frontend-web 阶段完成：我的页、User 地址管理、Order 交易流程；设计文档与 Skill 整理；下一步 Inventory BC |
| 2025-02-13 | BFF 创建，frontend 经 BFF 代理；开发路径改为先 Order 前端、后 Inventory/Payment/Fulfillment |
| 2026-02-14 | Order BC 全部 feature 完成（创建/取消/查询/事件驱动），23 scenario |
| 2026-02-12 | Catalog BC 新增展示图(OptionImage)，scenario 45 |
| 2025-02-12 | 项目初始化；Catalog、User BC 已完成 |
