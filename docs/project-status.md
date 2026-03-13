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
| **Catalog** | 类目、SPU、规格维度、SKU、展示图、商品类型与服务绑定、镭雕图案库 | ✅ 已完成 | 6 feature，72 scenario |
| **User** | 注册、登录(JWT)、收货地址 | ✅ 已完成 | 3 feature，19 scenario |
| **Order** | 创建、取消、查询、事件驱动状态流转、补购服务 | ✅ 已完成 | 5 feature，32 scenario |
| **Inventory** | 同步占用/释放、库存管理 | ✅ 已完成 | 3 feature，14 scenario |
| **Payment** | 扣款/退款/超时检测 | ✅ 已完成 | 5 feature，19 scenario |
| **Activity** | 事件消费、活动查询、统计仪表盘、订单旅程回放 | ✅ 已完成 | 3 feature，16 scenario |
| **Cart** | 增删改查、结算预览 | ✅ 已完成 | 5 feature，17 scenario |
| **Fulfillment** | 创建、配货、发货、签收、取消、查询 | ✅ 已完成 | 6 feature，24 scenario |
| **Smart Interaction** | LLM + MCP 智能交互、Skill 管理与自动匹配 | 🔄 演进中 | 4 feature，24 scenario |
| **BFF** | 前端统一 API 入口 | ✅ POC | 透传代理，端口 8085 |
| **Pricing** | 算价、优惠 | 🔲 规划中 | — |

### BC 间集成总览

- **同步调用（REST）**：Order → Inventory（占用/释放）、Order → Payment（创建/退款）、Order → Fulfillment（创建/取消）、Cart → Catalog（SKU 查询）
- **事件驱动（Kafka）**：Payment → Order（Completed/Failed/Expired）、Fulfillment → Order（Allocated/Shipped/Delivered）、Order/Payment/Inventory/Fulfillment → Activity

### 下一步

#### 功能演进路线（按依赖顺序）

| 顺序 | 功能 | 主要影响 BC | 前置依赖 | 状态 |
|------|------|------------|---------|------|
| **1** | 虚拟商品（保障服务、碎屏险等） | Catalog, Order, Fulfillment, Cart, Activity | 无 | ✅ 迭代 1（Catalog）已完成（5 feature, 63 scenario 全绿；含 SKU 级 ServiceBinding + 三种定价模式 + 前端 admin/web 集成 + MCP 工具 + AI Skill 更新）；✅ 迭代 2（交易流程）已完成（Order/Fulfillment/Cart/Activity 后端 + 前端 web/admin 全链路）。[业务需求方案](business-requirements/virtual-product/overview.md) |
| **1a** | 保障服务补购 | Order | 虚拟商品迭代 1+2 | ✅ 迭代 1 已完成（后端 7 scenario 全绿 + 前端订单详情页补购区域）。[业务需求方案](business-requirements/supplementary-purchase/overview.md) |
| **2** | 履约拆单（按仓库/商品类型拆单） | Inventory, Fulfillment, Order, Activity | 虚拟商品（提供拆分维度） | 🔲 待开发 |
| **3** | 镭雕服务（激光雕刻定制） | Catalog, Order, Fulfillment, Cart, Activity | 虚拟商品 | ✅ 迭代 0、1、2、3 已完成（含 BIZ-LE-007）；需重启 order-service 后 E2E 全绿 |

> 镭雕复用虚拟商品基础设施，不依赖拆单。镭雕 SERVICE 不拆成 VIRTUAL 履约单，雕刻内容附属实体履约单，须 completeEngraving 后才能 ship。[业务需求方案](business-requirements/laser-engraving/overview.md)

#### 智能运营（Intelligent Operations）

以业务流程为中心的事件架构演进，支撑四级智能运营能力（效率分析 → 实时监控 → 智能洞察 → 自主策略）。Smart Interaction 在 Step 1 完成后即接入（Step 2），此后每步同时交付：后端数据 + 前端展示 + AI 可感知。架构设计见 [business-process-architecture.md](business-process-architecture.md)。

| Step | 名称 | 可见结果 | 主要影响 BC | 状态 |
|------|------|---------|-----------|------|
| **1** | 多维事件基座 | Admin 多维查询事件（orderId/userId/skuId/spuId） | Order, Activity | ✅ 已完成 |
| **2** | Smart Interaction 接入 + 对话驱动页面 MVP | `/ops` 智能运营页面；对话-画布联动；AI 统一可见出口 | Smart Interaction, MCP, 前端 admin | ✅ 已完成 |
| **3** | 生命周期 + Level 1 分析 | 用户转化漏斗 + 商品经营概览仪表盘；AI 解读首购/首销 | Catalog, Activity, 前端 admin, Smart Interaction | 🔲 待开发 |
| **4** | 派生引擎 + Level 2 监控 | 预警页（流失/库存告急/销量下滑）；AI 解读预警并溯源 | Activity, Smart Interaction | 🔲 待开发 |
| **5** | 跨流程洞察 Level 3 | AI 跨用户/商品/交易三流程关联分析，回答「为什么」并给建议 | Smart Interaction, Activity | 🔲 待开发 |
| **6** | 行为事件 + 导购流程 | 转化漏斗（浏览→加购→下单）；AI 覆盖浏览到成交全链路 | 前端, Activity, Smart Interaction | 🔲 待开发 |
| **7** | 自主智能 Level 4 | 策略引擎；安全边界内跨流程自主行动；可审计日志 | Activity, 相关 BC | 🔲 待开发 |

> 每个 Step 进入执行时，通过 `analyze-requirement` 产出详细设计文档。

#### 其他待推进

1. **Smart Interaction 后续**：对话历史持久化、User MCP 工具、Fulfillment/Address MCP 工具扩展
2. **Pricing BC**：创建订单时同步算价（规划中）

---

## 三、前端进度

| 前端 | 职责 | 状态 | 已实现页面 |
|------|------|------|-----------|
| **frontend/admin** | 管理后台 | ✅ 基本完成 | Catalog、Inventory、Fulfillment、Activity、AI Chat、镭雕图案库、智能运营（/ops） |
| **frontend/web** | 消费者端 | ✅ 阶段完成 | 首页、登录注册、商品详情、购物车、结账、订单列表/详情、地址管理、我的、AI Chat |

---

## 四、MCP Server 进度

| 模块 | 状态 | 说明 |
|------|------|------|
| Catalog MCP tools | ✅ 已完成 | 类目/商品/规格/SKU/展示图 CRUD + 聚合查询 |
| Inventory MCP tools | ✅ 已完成 | 库存查询与管理（不暴露占用/释放） |
| Cart MCP tools | ✅ 已完成 | 购物车操作 + 结算预览，userId 自动注入 |
| Order MCP tools | ✅ 已完成 | 订单查询 + 创建，userId 自动注入 |
| User MCP tools | 🔲 待实现 | — |

---

## 五、关键决策记录

| # | 决策 | 原因 | 日期 |
|---|------|------|------|
| 4 | Inventory 采用同步占用 | 用户下单需即时获知库存结果 | 2026-02-15 |
| 5 | Payment 同步调用 + Kafka 事件 | 创建支付单同步；结果通知走事件驱动 | 2026-02-19 |
| 6 | Cart 不快照价格，结算由前端编排 | 展示时实时拉取 Catalog 价格 | 2026-02-20 |
| 7 | Fulfillment 同步创建 + 事件通知 | 创建/取消同步；发货/签收走 Kafka | 2026-02-20 |
| 8 | 拆单由 Fulfillment 负责 | Order 只传 orderId + items，Fulfillment 决定拆单策略 | 2026-02-20 |
| 9 | Order 取消规则收紧 | SHIPPED 及之后不可取消；多履约单按「最慢」原则推进状态 | 2026-02-20 |
| VP1 | 服务是独立的 SERVICE 类型 SPU | 复用 SPU-SKU 模型，通过 productType 区分；服务期限为 SpecDimension；ServiceBinding 绑定 SKU + 目标 SPU，priceCents 可选（null=继承 SKU 标准价，非 null=上下文定价）；无 binding 可独立售卖 | 2026-02-24 |
| VP2 | 服务类商品不占库存 | Order 过滤 SERVICE items，仅 PHYSICAL 调用 Inventory | 2026-02-24 |
| VP3 | 混合订单 + Fulfillment 拆单 | 一单含实体+服务，Fulfillment 按类型拆单 | 2026-02-24 |
| VP4 | ServiceActivated 等效 Delivered | 最慢原则推进 OrderCompleted | 2026-02-24 |
| VP5 | 已激活服务 MVP 不可取消 | 退保是未来能力 | 2026-02-24 |
| SP1 | 补购入口在订单详情页 | 已交付订单的实体商品旁显示可补购服务 | 2026-03-01 |
| SP2 | 补购不走购物车 | 直接下单，降低复杂度 | 2026-03-01 |
| SP3 | 实体商品必须已交付才能补购 | DELIVERED/COMPLETED 状态；服务在商品到手后才有意义 | 2026-03-01 |
| SP4 | 不允许重复补购 | 同一 relatedSkuId + 同一服务 SKU 不允许重复购买 | 2026-03-01 |
| SP5 | Order BC 提供补购聚合查询 API | Order 内部调用 Catalog 获取可选服务并去重已购 | 2026-03-01 |
| SP6 | 纯服务订单 ShippingAddress 可选 | 补购订单无物理配送，收货地址非必填 | 2026-03-01 |
| LE1 | 镭雕不拆成 VIRTUAL 履约单 | 雕刻内容附属实体履约单；须 completeEngraving 后才能 ship | 2026-03-04 |
| IO1 | 以业务流程为中心重构事件架构 | 事件按 BC 分类不足以支撑多流程智能运营；四个核心一级流程（交易/用户发展/商品运营/导购）；事件分原子/派生 × 里程碑/过程 | 2026-03-11 |
| IO2 | 事件 payload 增强一步到位 | 一次变更完整增加 userId、spuId、unitPriceCents 等多流程字段，避免多轮 schema 变更 | 2026-03-11 |
| IO3 | 智能运营七步迭代、Smart Interaction 提前接入 | Step 2 起立即接入 Smart Interaction，后续每步同时交付后端数据+前端展示+AI 可感知；多流程并行推进不逐流程串行 | 2026-03-11 |
| IO4 | Smart Interaction 在每步完成后同步增强 | 避免前几步只做仪表盘而结果不直观；Step 3/4 各自在内部包含 AI 知识更新交付物 | 2026-03-12 |

---

## 六、变更日志

| 日期 | 变更内容 |
|------|---------|
| 2026-03-12 | 智能运营 Step 2 交付：`/ops` 智能运营页面（三区域布局：固定指标栏 + 动态画布 + 常驻 AI 侧边栏）；`activity_query` 扩展多维参数 + `_raw` 结构化返回；`useOpsCanvas` 画布状态机 + 对话-画布联动；McpToolBridge `_raw` 透传；新增 `intelligent-ops-domain` MCP Resource；「智能运营助手」Skill；BIZ-IO2-001 E2E 3 用例全绿 |
| 2026-03-12 | 智能运营演进路线重整：取消 Step 1.5 编号，整理为 Step 1→7；Smart Interaction 接入提前为 Step 2，Step 3/4 各自包含 AI 同步增强交付物；business-process-architecture.md 完整重写第七章 |
| 2026-03-11 | 智能运营路线调整：Step 1.5 插入「Smart Interaction 接入」，保证每步可见结果 |
| 2026-03-11 | 智能运营 Step 1（多维事件基座）交付：Order 三事件 payload 增强（userId/totalAmountCents/items 快照）；Activity BusinessActivity 新增 userId/correlationKeys、EventMetadata 新增 origin/processRoles、多维查询 API；admin ActivityPage 多维查询区；BIZ-IO1-001 E2E 通过 |
| 2026-03-04 | 镭雕迭代 3（订单与履约详情展示）完成：Order 详情 API 返回 serviceAttributes；Fulfillment 发布 EngravingCompleted 事件；Activity 注册消费；frontend/web OrderDetailPage 展示镭雕内容及「镭雕已完成」；BIZ-LE-007 已添加 |
| 2026-03-04 | 镭雕迭代 2（下单与履约）完成：Order PlaceOrder 接纳 serviceAttributes + createFulfillment 传参；Fulfillment engravingInfo 合并、completeEngraving、ship 门禁、查询返回；web 结账页传 serviceAttributes；admin FulfillmentPage 镭雕列、完成镭雕按钮、ship 门禁；BIZ-LE-005 E2E 全绿（Page Object 修正镭雕选择器） |
| 2026-03-04 | 镭雕迭代 1（镭雕服务配置与选品）完成：Catalog SPU.serviceKind + available-services 返回 serviceKind（74 scenario）；admin 镭雕 Badge；web 详情页镭雕可选 + 图案库 + 文字输入；BIZ-LE-004；Smoke P0 通过 |
| 2026-03-04 | 镭雕迭代 0（图案库）完成：Catalog EngravingPattern CRUD + 6 scenario 全绿（总 72 scenario）；BFF 新增 `/api/engraving-patterns` 路由；frontend/admin 图案库管理页（`/engraving-patterns`）+ Business E2E 3 用例全绿（BIZ-LE-001～003）；admin 测试基础设施首次搭建（Playwright）；deliver-requirement Skill 优化（E2E 闭环 + 环境准备） |
| 2026-03-04 | 镭雕服务 Phase B 落地：overview.md 变更分析已写入各 BC 文档（Catalog EngravingPattern、Order serviceAttributes、Fulfillment engravingInfo/completeEngraving/ship 门禁）；context-map、business-flows、project-status 同步更新；新增 N2O-6、O2F-4 路径 |
| 2026-03-01 | 保障服务补购迭代 1 完成：后端全链路（域对象+基础设施+应用服务+API+验收测试 7 scenario 全绿，总 41 test）；前端订单详情页补购区域（查询可补购服务 + 一键补购下单）；api.yaml/ui-spec.md 同步更新 |
| 2026-03-01 | 保障服务补购业务需求分析完成：overview.md + Order BC 文档增量变更（requirements/domain-model/event-flow）；新增 N2O-5 路径；仅影响 Order BC（🟡 中等），其余 BC 无变更 |
| 2026-02-28 | Smoke E2E 分级机制（P0/P1）：P0 核心交易链路、P1 重要非核心；超时降级审查；`npm run test:smoke:e2e:p0` 仅跑 P0；Business E2E 断言深度统一为"链路能走通"；testing.md / design-principles / Skills 全面同步 |
| 2026-02-28 | 前端测试体系升级：新增 Business E2E 层（关键业务需求前端验收），与 Smoke E2E 共享 Page Object 层（tests/shared/）；虚拟商品 2 条 Business E2E 用例（BIZ-VP-001/002） |
| 2026-02-28 | 虚拟商品迭代 2 完成：Order/Fulfillment/Cart/Activity 后端全到位；frontend/web CartPage 分组、CheckoutPage 分组、OrderDetailPage itemType+激活状态展示；frontend/admin FulfillmentPage 虚拟单类型列+ACTIVATED 状态+隐藏物流操作；OrderDto 补 serviceActivated 字段 |
| 2026-02-27 | 迭代 1 收尾：修复 available-services API 返回最终售价（binding.priceCents ?? sku.priceCents）；新增 5.11～5.12 验收场景（null priceCents 创建与查询），63 场景全绿；全文档一致性检查与更新 |
| 2026-02-27 | 开发流程优化：Skill 流程强制约束写入 project-context.md；实现顺序原则写入 design-principles.md §2.4a；各 Skill 交叉引用统一 |
| 2026-02-27 | ServiceBinding.priceCents 模型决策：从必填调整为可选（nullable），支持三种定价模式（独立售卖/统一价格限定范围/上下文差异定价） |
| 2026-02-27 | SPU 移除 serviceCategory 属性：服务分类由类目体系表达，SPU 仅保留 productType 一个新增属性；全链路清理（后端 + 前端 + 文档 + 契约） |
| 2026-02-26 | ServiceBinding 模型调整：从 SPU 级绑定改为 SKU 级绑定 + 上下文定价（serviceSkuId + targetSpuId + priceCents）；SPU 移除 serviceDurationDays，服务期限改用 SpecDimension + SpecOption；11 场景全绿，总 61 场景 |
| 2026-02-26 | 虚拟商品迭代 1 完成：Catalog 支持 SERVICE 类型商品 + ServiceBinding（11 场景全绿，总 61 场景）；前端 admin/web 集成 productType 展示与可选服务列表 |
| 2026-02-24 | 虚拟商品业务需求分析完成：overview.md + 各 BC 文档增量变更；analyze-requirement Skill 演进支持业务需求范围 |
| 2026-02-24 | 功能演进路线规划：虚拟商品 → 履约拆单 → 镭雕服务 |
| 2026-02-24 | Activity 订单旅程回放 V2 完成（分组时间线 + 泳道式可视化 + 回放动画） |
| 2026-02-23 | Smart Interaction 购物助手 V2：userId 自动注入链路打通；新增 Cart/Order MCP tools |
| 2026-02-22 | Smart Interaction 多轮迭代：消费者端 AI Chat 接入、Skill 自动匹配、三态选择器、Catalog 聚合查询 API + MCP 优化 |
| 2026-02-21 | Fulfillment Kafka 事件发布；Smart Interaction 从 BFF 拆分为独立 BC |
| 2026-02-20 | Order–Fulfillment 集成；Fulfillment BC 完成；Cart BC 完成 |
| 2026-02-19 | Activity BC 完成；Payment 全面切换 Kafka 事件 |
| 2026-02-17 | Order–Inventory 集成完成 |
| 2026-02-16 | Inventory BC 完成 |
| 2026-02-15 | Payment BC 完成；frontend/web 阶段完成 |
| 2026-02-14 | Order BC 完成 |
| 2025-02-12 | 项目初始化；Catalog、User BC 完成 |
