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
| **Catalog** | 类目、SPU、规格维度、SKU、展示图、商品类型与服务绑定 | ✅ 已完成 | 5 feature，63 scenario |
| **User** | 注册、登录(JWT)、收货地址 | ✅ 已完成 | 3 feature，19 scenario |
| **Order** | 创建、取消、查询、事件驱动状态流转 | ✅ 已完成 | 4 feature，25 scenario |
| **Inventory** | 同步占用/释放、库存管理 | ✅ 已完成 | 3 feature，12 scenario |
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
| **1** | 虚拟商品（保障服务、碎屏险等） | Catalog, Order, Fulfillment, Cart, Activity | 无 | ✅ 迭代 1（Catalog）已完成（5 feature, 63 scenario 全绿；含 SKU 级 ServiceBinding + 三种定价模式 + 前端 admin/web 集成 + MCP 工具 + AI Skill 更新）；🔲 迭代 2（交易流程）、迭代 3（补购）待开发。[业务需求方案](business-requirements/virtual-product/overview.md) |
| **2** | 履约拆单（按仓库/商品类型拆单） | Inventory, Fulfillment, Order, Activity | 虚拟商品（提供拆分维度） | 🔲 待开发 |
| **3** | 镭雕服务（激光雕刻定制） | Catalog, Order, Fulfillment | 虚拟商品 + 拆单 | 🔲 待开发 |

> 三者存在耦合：虚拟商品引入商品类型区分（实物/虚拟），是拆单的基本维度之一；拆单引擎支撑不同履约方式的分流；镭雕服务的建模方式及履约流程依赖前两者的基础设施。

#### 其他待推进

1. **Smart Interaction 后续**：对话历史持久化、MCP 工具扩展（Fulfillment/Activity/Address）
2. **Pricing BC**：创建订单时同步算价（规划中）

---

## 三、前端进度

| 前端 | 职责 | 状态 | 已实现页面 |
|------|------|------|-----------|
| **frontend/admin** | 管理后台 | ✅ 基本完成 | Catalog、Inventory、Fulfillment、Activity、AI Chat |
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
| VP5 | 补购是独立新订单 | 通过 relatedOrderId 关联原购买 | 2026-02-24 |
| VP6 | 已激活服务 MVP 不可取消 | 退保是未来能力 | 2026-02-24 |
| VP7 | 先做随购，后做补购 | 随购模型变更更基础 | 2026-02-24 |

---

## 六、变更日志

| 日期 | 变更内容 |
|------|---------|
| 2026-02-27 | 迭代 1 收尾：修复 available-services API 返回最终售价（binding.priceCents ?? sku.priceCents）；新增 5.11～5.12 验收场景（null priceCents 创建与查询），63 场景全绿；全文档一致性检查与更新 |
| 2026-02-27 | 开发流程优化：Skill 流程强制约束写入 project-context.md；实现顺序原则写入 design-principles.md §2.4a；各 Skill 交叉引用统一 |
| 2026-02-27 | ServiceBinding.priceCents 模型决策：从必填调整为可选（nullable），支持三种定价模式（独立售卖/统一价格限定范围/上下文差异定价） |
| 2026-02-27 | SPU 移除 serviceCategory 属性：服务分类由类目体系表达，SPU 仅保留 productType 一个新增属性；全链路清理（后端 + 前端 + 文档 + 契约） |
| 2026-02-26 | ServiceBinding 模型调整：从 SPU 级绑定改为 SKU 级绑定 + 上下文定价（serviceSkuId + targetSpuId + priceCents）；SPU 移除 serviceDurationDays，服务期限改用 SpecDimension + SpecOption；11 场景全绿，总 61 场景 |
| 2026-02-26 | 虚拟商品迭代 1 完成：Catalog 支持 SERVICE 类型商品 + ServiceBinding（11 场景全绿，总 61 场景）；前端 admin/web 集成 productType 展示与可选服务列表 |
| 2026-02-24 | 虚拟商品 Epic 需求分析完成：overview.md + 各 BC 文档增量变更；analyze-requirement Skill 演进支持 Epic 范围 |
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
