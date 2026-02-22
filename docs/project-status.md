# HMall 项目状态

> 本文件记录项目当前进度、路线图与关键决策。每完成一个阶段后更新。BC 间关系与集成方式见 [context-map.md](context-map.md)。

---

## 一、项目愿景

HMall 是一个以 DDD + ATDD 驱动的电商系统练习项目，覆盖商品、用户、购物车、订单、支付、履约等电商核心域，前后端完整实现。

---

## 二、BC 路线图与进度

### 推进顺序

```
Catalog ✅ → User ✅ → Order ✅ → Inventory ✅ → Payment ✅ → Activity ✅ → Cart ✅ → Fulfillment ✅ → Smart Interaction ✅ → [Pricing 按需]
```

### 各 BC 状态

| BC | 职责 | 状态 | 说明 |
|----|------|------|------|
| **Catalog** | 类目、商品(SPU)、规格维度、SKU、展示图 | ✅ 已完成 | 4 个 feature，50 个 scenario，全部通过；新增类目树查询 + 商品搜索 API |
| **User** | 用户注册、登录(JWT)、收货地址管理 | ✅ 已完成 | 3 个 feature（user、login、address），19 个 scenario，全部通过 |
| **Order** | 订单创建、取消、查询、事件驱动、状态流转 | ✅ 已完成 | 4 个 feature，25 个 scenario，全部通过；已与 Fulfillment 集成（同步创建/取消） |
| **BFF** | frontend 统一 API 入口，代理 Catalog/User/Order/Inventory/Cart/Fulfillment | ✅ POC | 透传代理、CORS、4xx/5xx 转发，端口 8085 |
| **Smart Interaction** | LLM + MCP 智能交互（对话式操作）+ Skill 管理 | 🔄 演进中 | 端口 8089；2 feature（ai-chat + skill-management）、16 scenario 全绿；后端：Skill CRUD + 默认设置 + 通配符工具过滤 + 对话集成 Skill（systemPrompt 动态加载、allowedTools 过滤、maxToolCallRounds 限制）；前端：Skill 选择器 + Skill 管理面板（Drawer 内 CRUD + 设默认）；待完成：对话历史持久化、系统设置、MCP 工具扩展 |
| **Inventory** | 同步占用/释放库存、库存管理 | ✅ 已完成 | 3 feature、11 scenario 全绿；已与 Order 集成 |
| **Payment** | 扣款/退款/超时检测 | ✅ 已完成 | 5 feature、19 scenario 全绿；超时检测定时自动执行；事件通知 Order（Kafka：PaymentCompleted/Failed/Expired）；已与 Order 集成（同步创建支付单/退款） |
| **Activity** | 消费 Order/Payment/Inventory/Fulfillment 事件，活动记录、查询与统计仪表盘 | ✅ 已完成 | 3 个 feature（consume/query/stats），16 个 scenario，全部通过；已订阅 Fulfillment 全部 4 个事件 |
| **Fulfillment** | 拆单、开始配货、发货、签收、取消、查询 | ✅ 已完成 | 端口 8088；6 feature、24 scenario 全绿；ALLOCATING 状态与「开始配货」API；同步创建/取消 + Kafka 事件发布（Created/Allocated/Shipped/Delivered）；已与 Order、Activity 集成 |
| **Pricing** | 算价、优惠 | 🔲 规划中 | 创建订单时同步调用 |
| **Cart** | 购物车增删改查、结算预览 | ✅ 已完成 | 5 feature（+ smoke），17 scenario，全部通过；已与 Catalog 集成（CatalogSkuQueryAdapter 实时查询 SKU）+ User；结算由前端编排 |

### 已完成：Order 与 Inventory 集成

Order 通过 `RestOccupyInventoryAdapter`、`RestReleaseInventoryAdapter` 调用 Inventory 的 `POST /api/inventory/occupy`、`POST /api/inventory/release`。验收测试用 Stub，集成测试用 WireMock 验证 HTTP 调用。详见 `services/order-service/README.md`。

### 已完成：Order 与 Fulfillment 集成

Order 通过 `RestCreateFulfillmentAdapter`、`RestCancelFulfillmentAdapter` 调用 Fulfillment 的 `POST /api/fulfillment/create`、`POST /api/fulfillment/cancel`。验收测试用 Stub，配置 `fulfillment.base-url` 后启用真实适配器。变更要点：
- `CreateFulfillmentPort` 签名：`createFulfillment(orderId, items, shippingAddress) → List<Long>`；Order 创建履约单后**保持 PAID**（不置 FULFILLING）
- Fulfillment 新增 **ALLOCATING** 状态与「开始配货」`POST /api/fulfillment/{id}/allocate`；发布 **FulfillmentOrderAllocated** 事件（Topic `fulfillment.order.allocated`）
- Order 消费 **FulfillmentOrderAllocated** 后置 FULFILLING（订单页「正在配货」仅在真正开始配货时显示）
- 新增 `CancelFulfillmentPort`：Order 取消时若 status 为 **PAID 或 FULFILLING** 则同步调用（原仅 FULFILLING）
- 取消规则：SHIPPED / DELIVERED 状态不可取消

### 已完成：Order 与 Payment 集成

Order 通过 `RestCreatePaymentAdapter`、`RestRefundPaymentAdapter` 调用 Payment 服务。Payment 通过 Kafka 发布 PaymentCompleted/Failed/Expired 事件，Order 通过 `KafkaPaymentEventConsumer` 消费。

### 已完成：Kafka 事件联通

各 BC 的 Kafka 事件发布/消费已实现。测试中排除 KafkaAutoConfiguration（使用桩替身），生产环境有 Kafka 时自动启用 `@Primary` 的 Kafka 实现。已联通链路：Payment → Order（PaymentCompleted/Failed/Expired）、Fulfillment → Order（OrderAllocated/Shipped/Delivered）、Order/Payment/Inventory/Fulfillment → Activity。

### 已完成：Cart 与 Catalog 集成

Cart 通过 `CatalogSkuQueryAdapter`（`@Component`，REST 调用 `catalog.base-url`，默认 `http://localhost:8080`）实时查询 SKU 信息（名称、价格、库存）。验收测试用 `StubSkuQueryPort`（`@Primary`），生产环境自动使用真实适配器。前端购物车页面 → BFF → Cart → Catalog 链路完整。

### 下一步

1. **Smart Interaction V2**：对话历史持久化、系统设置、MCP 工具扩展（Inventory/Order/Fulfillment/Activity）、前端对话历史/设置面板
2. **Pricing BC**：创建订单时同步算价（规划中）

---

## 三、前端进度

| 前端 | 职责 | 状态 | 已实现页面 |
|------|------|------|-----------|
| **frontend-admin** | 管理后台，展示+库存管理+履约管理+监控仪表盘+AI 对话 | ✅ 基本完成 | HomePage、CatalogPage、ProductDetailPage、InventoryPage、FulfillmentPage、ActivityPage、AI Chat（全局 Drawer） |
| **frontend-web** | 消费者端 | ✅ 阶段完成 | HomePage、LoginPage、RegisterPage、ProductDetailPage、CartPage、CheckoutPage、OrderListPage、OrderDetailPage、AddressPage、MyPage |

### frontend-web 已实现

- **Order 交易流程**：立即购买 → 结账页（选地址/新增地址、订单确认）→ 提交订单 → 模拟支付 → 订单列表/详情（支持按状态筛选）
- **收货地址管理**：地址列表、新增、编辑、删除；结账页可选已保存地址
- **「我的」聚合页**：用户信息块、收货地址入口、我的订单块（待付款/待收货/待评价）；Atomic Design（atoms/molecules/organisms）
- **文档**：`docs/frontend-web/design-input.md`、`docs/design-principles.md` 前端节、frontend-development Skill 与 Design Input 定位已整理

---

## 四、MCP Server 进度

| 模块 | 状态 | 说明 |
|------|------|------|
| Catalog MCP tools | ✅ 已完成 | 类目/商品/规格/SKU/展示图 CRUD 及图片上传；新增 catalog_get_category_tree、catalog_search_products、catalog_get_product_full；dimensions 返回选项 ID；输出格式精简（减少 token） |
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
| 7 | Fulfillment 同步创建 + 事件通知 | 创建/取消履约单为同步调用（与 Inventory、Payment 一致）；Shipped/Delivered 走 Kafka 事件；FulfillmentOrderCreated 事件仅 Activity 消费 | 2026-02-20 |
| 8 | 拆单由 Fulfillment 负责 | Order 只传 orderId + items，Fulfillment 决定拆单策略；MVP 先 1:1 | 2026-02-20 |
| 9 | Order 取消规则收紧 | SHIPPED 及之后不可取消（发货后走退货流程）；多履约单按「最慢」原则推进 Order 状态 | 2026-02-20 |

---

## 六、变更日志

| 日期 | 变更内容 |
|------|---------|
| 2026-02-22 | Catalog BC 新增类目树查询（GET /api/categories/tree）和商品搜索（GET /api/products/search）API；MCP tools 优化：新增 catalog_get_category_tree/catalog_search_products/catalog_get_product_full 三个聚合查询工具，catalog_list_dimensions 返回选项 ID，所有列表输出精简为紧凑格式（减少 LLM token 消耗） |
| 2026-02-22 | Smart Interaction 前端 Skill UI：SkillSelector（下拉选择/切换 Skill）+ SkillManager（Drawer 内 CRUD + 设默认）；skill.js API 封装；useAiChat composable 集成 Skill 状态管理（loadSkills/createSkill/updateSkill/removeSkill/setDefaultSkill）；sendMessage 自动附带 skillId；Vite proxy 新增 /api/ai → 8089 路由 |
| 2026-02-22 | Smart Interaction 对话集成 Skill：ChatRequest 新增 skillId/maxToolCallRounds；AiChatService 加载 Skill systemPrompt、按 allowedTools 过滤工具、可配置 tool call 轮次限制；新增 4 个验收场景（16 scenario 全绿） |
| 2026-02-21 | Fulfillment Kafka 事件发布实现：新增 KafkaDomainEventPublisher（@Primary 覆盖 LoggingDomainEventPublisher），发布 Created/Allocated/Shipped/Delivered 到 Kafka；Activity 订阅 Fulfillment 全部 4 个 topic；端到端链路打通（Fulfillment → Kafka → Order + Activity） |
| 2026-02-21 | Smart Interaction BC 拆分：AI Chat 模块从 BFF 拆分为独立限界上下文 smart-interaction-service（端口 8089）；代码、测试、配置完整迁移；BFF 清理（移除 ai 包、webflux/Cucumber/WireMock 依赖）；脚本支持 `--bc smart-interaction` |
| 2026-02-21 | BFF AI Chat 模块完成：LLM + MCP Tool Calling 对话式操作；后端（AiChatService/LlmClient/McpToolBridge）+ frontend-admin 全局 Drawer；验收测试 4 场景（WireMock stub LLM/MCP）全绿 |
| 2026-02-20 | Order + Fulfillment 集成完成：CreateFulfillmentPort 签名变更（新增 items/addr，返回 fulfillmentOrderIds）、新增 CancelFulfillmentPort、移除 onFulfillmentOrderCreated 消费、取消规则收紧（SHIPPED/DELIVERED 不可取消）；Order 25 scenario 全绿 |
| 2026-02-20 | Fulfillment BC 全部完成（5 feature、18 scenario 全绿）；DDD 四层架构、同步创建/取消 + 事件发布（FulfillmentOrderCreated/Shipped/Delivered）；领域模型含领域事件定义 |
| 2026-02-20 | Fulfillment BC 需求分析完成（requirements.md + domain-model.md + event-flow.md）；5 feature、18 scenario 待实现；Order 变更清单已记录（取消规则、接口变更、移除 FulfillmentOrderCreated 消费） |
| 2026-02-20 | Cart BC 全部完成（5 feature + smoke，17 scenario 全绿）；DDD 四层架构、SkuQueryPort 出站端口（测试用 Stub）、结算预览 API |
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
