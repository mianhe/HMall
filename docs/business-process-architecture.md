# 业务流程架构（Business Process Architecture）

以业务流程为中心的事件架构设计。定义一级业务流程体系、事件分类与管理框架、智能化应用分层、以及系统的演进路线。

> **与其他文档的关系**：
> - [business-flows.md](business-flows.md)：当前交易流程的事件流与路径枚举（本文档的子集，聚焦 N2O/O2F）
> - [context-map.md](context-map.md)：BC 结构与集成方式
> - [ontology/hmall-ontology.md](ontology/hmall-ontology.md)：业务对象与关联关系
> - [design-principles.md](design-principles.md)：架构与实现约定（含 EDA 技术规范）
>
> 本文档回答的问题：**系统的业务流程是什么、事件如何组织、智能化如何分层、系统向何处演进。**

---

## 一、背景与动机

### 1.1 当前状态

系统已建立了以 **限界上下文（BC）** 为中心的事件模型：

- 14 种领域事件分布在 Order、Payment、Inventory、Fulfillment 四个 BC
- Kafka 作为事件总线，Activity BC 订阅全量事件并持久化为 BusinessActivity
- `orderId` 作为全局关联键贯穿所有 BC
- EventMetadataRegistry 按 BC 维度管理事件的标签、分类和补偿关系

这套模型对 **交易流程** 的支撑已经比较完整——可以按 orderId 回放订单旅程、统计交易指标、可视化跨 BC 协作。

### 1.2 局限

当系统从"交易"扩展到更多业务领域时，以 BC 为中心的事件模型面临四个不足：

| 局限 | 表现 |
|------|------|
| **单一关联维度** | 只有 orderId，无法回答"某个用户的完整生命周期经历了什么"或"某个商品的经营表现如何" |
| **事件 payload 为单一流程设计** | 部分事件（如 OrderCompleted、OrderCancelled）仅携带 orderId，缺少 userId 和 skuId，无法自描述地支撑用户和商品维度的分析 |
| **缺乏业务流程视角** | 事件按技术来源（BC）分类，而非按业务价值（流程）组织。运营者关心的是"交易流程是否健康"，而不是"Order BC 发了几个事件" |
| **无法支撑智能化运营** | 流程效率分析、实时监控、智能洞察、自主策略调整——这些都需要以流程为单位来组织数据和知识 |

### 1.3 演进方向

从 **"以 BC 为中心的事件存储"** 演进为 **"以业务流程为中心的事件平台"**：

```
当前：事件 → 按 BC 分类 → 按 orderId 关联 → 基础统计
目标：事件 → 按业务流程组织 → 多维关联 → 智能化运营
```

这不是推翻现有设计，而是在 BC 维度之上叠加流程维度。BC 仍然是事件的技术来源，流程是事件的业务归属。两者正交互补。

---

## 二、核心概念

### 2.1 业务流程

> **业务流程是围绕一个核心业务对象的生命周期展开的价值创造过程。**

流程的四个特征：

1. **价值创造**：流程是业务创造价值的过程。完成一次交易、发展一个用户、运营一个商品，都在创造价值。
2. **核心业务对象**：每个流程围绕一个核心业务对象展开。这个对象承载了流程所创造的价值——Order 承载交易价值，User 承载用户资产价值。核心对象可能已经显式存在于系统中，也可能需要从业务中抽象出来。
3. **生命周期**：核心对象有一个从诞生到终态的状态演进过程。正是这个生命周期构成了流程的骨架。
4. **事件串联**：流程中的关键状态转换由事件标记。我们通过推动这些事件的发生来推进流程、实现价值。

流程是分级的：

| 层级 | 说明 | 示例 |
|------|------|------|
| **一级流程** | 直接创造业务价值的核心过程 | 交易、用户发展 |
| **二级子流程** | 一级流程内的阶段，对应核心对象生命周期的一个环节 | 交易中的支付、交易中的履约 |
| **三级子流程** | 二级子流程的进一步分解 | 履约中的配货、履约中的配送 |

二级子流程是一级流程核心对象生命周期的 **一个阶段**。判断标准：子流程结束后，核心对象继续在一级流程中向前推进。例如支付完成后，Order 继续进入履约阶段。

### 2.2 事件

> **事件是业务流程中发生的重要事实。**

事件是不可变的——它描述的是已经发生的事情，不是请求、不是命令。事件用过去时态命名（Created、Completed、Churned），因为它已经是既成事实。

事件有两个正交的分类维度：

**维度一：产生方式（Origin）**

| 类型 | 定义 | 特征 | 示例 |
|------|------|------|------|
| **原子事件** | 系统中直接观测到的事实 | 有明确的触发源和时间点；由 BC 在状态变更时发布 | OrderCreated、PaymentCompleted |
| **派生事件** | 从原子事件的模式中推断出来的高阶事实 | 不由某个 BC 直接发布，由解读层计算产生 | BecameActiveUser、UserChurned |

原子事件进一步分为两类：
- **领域事件**：BC 状态变更时产生（OrderCreated、PaymentCompleted）—— 当前系统中的 14 种事件全属此类
- **行为事件**：用户行为信号，不一定引起 BC 状态变更（SearchPerformed、ProductViewed）—— 当前系统尚未采集

派生事件有三种产生机制：

| 机制 | 定义 | 示例 |
|------|------|------|
| **触发式** | 某个原子事件发生后立即评估条件 | OrderCompleted → 检查是否首购 → FirstPurchaseCompleted |
| **累积式** | 原子事件在时间窗口内累积达到阈值 | 30 天内 3 笔 OrderCompleted → BecameActiveUser |
| **缺失式** | 检测到一段时间内没有发生预期事件 | 90 天无 OrderCompleted → UserChurned |

**维度二：流程角色（Significance）**

| 类型 | 定义 | 特征 | 示例 |
|------|------|------|------|
| **里程碑事件** | 流程的关键节点，标志核心对象生命周期的重要状态转换 | 数量有限、可穷举，构成流程的"骨架叙事" | OrderCompleted（交易终态）、BecameActiveUser（用户激活） |
| **过程事件** | 推动流程前进的日常步骤 | 数量多、持续发生，是里程碑的"构成材料" | FulfillmentShipped（交易中的一步）、单次 OrderCompleted（用户发展中的一次贡献） |

**关键原则：流程角色不是事件的固有属性，而是事件与流程的关系属性。** 同一个 `OrderCompleted` 在交易流程中是里程碑（流程终态），在用户发展流程中是过程事件（用户走向活跃的一次贡献）。

### 2.3 流程与事件的关系

流程与事件是 **多对多** 的关系：

- 一个流程包含多个事件（里程碑 + 过程事件构成流程的完整图景）
- 一个事件可归属多个流程（OrderCompleted 同时属于交易和用户发展）

事件的 **流程角色** 是相对于具体流程而言的：

```
EventMetadata
├── eventType: "OrderCompleted"
├── boundedContext: "Order"                    ← 技术来源（固定）
└── processRoles:                              ← 业务归属（多个）
    ├── TRADING:        { significance: MILESTONE }
    └── USER_LIFECYCLE: { significance: PROGRESSION }
```

---

## 三、一级业务流程体系

### 3.1 识别准则

一个一级业务流程需要同时满足：

1. **有核心业务对象**：这个对象承载了价值
2. **有生命周期**：该对象从诞生到终态有一个有意义的状态演进过程
3. **生命周期的推进直接创造业务价值**：不是辅助性的，而是业务存在的理由之一
4. **可以被事件串起**：生命周期中的关键状态转换可以用事件来标记

一级流程之间是 **平级但不平等** 的：它们在结构上处于同一层——都有自己的核心对象、生命周期、事件骨架，彼此不存在包含关系。但它们对业务价值的贡献权重不同。

### 3.2 核心一级流程

四个核心一级流程构成电商价值创造的主循环：

```
商品运营 ──提供供给──→ 导购 ──连接供需──→ 交易 ──实现价值──→ 用户发展 ──沉淀资产──→
    ↑                                                                           │
    └─────────────────── 用户需求反馈指导选品与运营 ←──────────────────────────────┘
```

#### 3.2.1 交易流程（Trading）

| 属性 | 说明 |
|------|------|
| **核心对象** | Order（订单） |
| **生命周期** | Created → Paid → Fulfilling → Shipped → Delivered → Completed（或 Cancelled） |
| **创造的价值** | 将用户的购买意图转化为实际的商品/服务交付，直接产生收入 |
| **关联键** | orderId |
| **当前状态** | ✅ 已有完整的事件流和实现 |

二级子流程：

| 子流程 | 对应阶段 | 核心事件 |
|--------|---------|---------|
| 下单 | Created | OrderCreated、StockReserved |
| 支付 | Created → Paid | PaymentCompleted / PaymentFailed / PaymentExpired |
| 履约 | Paid → Delivered | FulfillmentOrderCreated → Allocated → Shipped → Delivered |
| 完成 | Delivered → Completed | OrderCompleted |
| 补偿（逆向） | → Cancelled | OrderCancelled、StockReleased、PaymentRefunded |

> 交易流程的详细事件流、路径枚举、测试覆盖映射见 [business-flows.md](business-flows.md)。

#### 3.2.2 用户发展流程（User Lifecycle）

| 属性 | 说明 |
|------|------|
| **核心对象** | Customer（用户/客户，作为生命周期载体） |
| **生命周期** | Anonymous → Registered → Converted → Active → Loyal → Churned → Rescued |
| **创造的价值** | 扩大用户规模、提升用户活跃度和忠诚度，增长用户生命周期价值（LTV） |
| **关联键** | userId |
| **当前状态** | 🔲 框架待建 — User BC 存在，但无生命周期建模和派生事件 |

生命周期阶段与关键里程碑：

| 阶段 | 含义 | 里程碑事件 | 事件性质 |
|------|------|-----------|---------|
| Anonymous → Registered | 用户注册 | UserRegistered | 原子（领域事件） |
| Registered → Converted | 完成首次购买 | FirstPurchaseCompleted | 派生（触发式） |
| Converted → Active | 达到活跃标准 | BecameActiveUser | 派生（累积式） |
| Active → Loyal | 持续活跃，高价值 | BecameLoyal | 派生（累积式） |
| Active/Loyal → Churned | 长时间无活动 | UserChurned | 派生（缺失式） |
| Churned → Rescued | 失活后重新活跃 | UserRescued | 派生（触发式） |

这个流程的特殊性：
- **生命周期很长**（跨越数月甚至数年），远长于单笔交易
- **大量里程碑是派生事件**，需要派生事件引擎来计算
- **过程事件大量与其他流程共享**——每次 OrderCompleted 既属于交易也属于用户发展

#### 3.2.3 商品运营流程（Product Operations）

| 属性 | 说明 |
|------|------|
| **核心对象** | Product（SPU/SKU） |
| **生命周期** | Created → Listed → Growing → Mature → Declining → Retired |
| **创造的价值** | 让对的商品以对的形态出现在对的位置、以对的价格卖给对的用户 |
| **关联键** | spuId / skuId |
| **当前状态** | 🔲 框架待建 — Catalog BC 支持 CRUD，但无经营视角的生命周期建模 |

**重要澄清**：商品运营流程的目标不是商品管理本身（CRUD），而是围绕商品的 **经营决策过程**——选品、定价、展示、上下架策略、销售分析、生命周期管理，服务于经营目标。

生命周期阶段与关键里程碑：

| 阶段 | 含义 | 里程碑事件 | 事件性质 |
|------|------|-----------|---------|
| Created → Listed | 商品上架可售 | ProductListed | 原子（领域事件） |
| Listed → Growing | 销量进入增长期 | ProductBecamePopular | 派生（累积式） |
| Growing → Mature | 销量稳定 | ProductReachedMaturity | 派生（累积式） |
| Mature → Declining | 销量开始下滑 | ProductSalesDeclined | 派生（累积式/缺失式） |
| Declining → Retired | 商品下架退出 | ProductRetired | 原子（领域事件） |

过程事件（日常运营信号）：
- 被购买（OrderCreated 含该 SKU）
- 被浏览（ProductViewed — 行为事件，待采集）
- 库存变动（StockReserved、StockReleased）
- 调价（PriceAdjusted — 待实现）
- 加入购物车（CartItemAdded — 行为事件，待采集）

#### 3.2.4 导购流程（Merchandising / Shopping Guide）

| 属性 | 说明 |
|------|------|
| **核心对象** | Shopping Journey（购物旅程）—— 用户从模糊意图到明确购买需求的决策过程 |
| **生命周期** | Triggered → Exploring → Evaluating → Decided → Converted（或 Abandoned） |
| **创造的价值** | 将用户的模糊意图转化为明确的购买需求，连接供给与需求 |
| **关联键** | journeyId（待定义）+ userId |
| **当前状态** | 🔲 框架待建 — 系统尚未覆盖价值流的左端（Intention → Needs） |

这个流程目前是四个核心流程中抽象程度最低的。核心对象 Shopping Journey 在当前系统中是隐式的——散落在搜索日志、浏览记录、加购行为中，尚未被显式建模。

生命周期阶段与关键里程碑：

| 阶段 | 含义 | 里程碑事件 | 事件性质 |
|------|------|-----------|---------|
| → Triggered | 用户开始购物旅程 | JourneyStarted | 派生（触发式） |
| Exploring | 浏览和搜索 | — | 过程事件：SearchPerformed、ProductViewed |
| Evaluating | 比较和评估 | NeedsIdentified | 派生（触发式，如进入详情页/加购） |
| Decided | 购买决定 | PurchaseDecided | 原子（提交订单） |
| Converted / Abandoned | 转化或放弃 | JourneyConverted / JourneyAbandoned | 派生 |

导购流程对应价值流中 `Intention → Needs` 这一段——当前系统尚未覆盖的左端。随着系统向该方向扩展，行为事件的采集和 Shopping Journey 的显式建模将成为实现重点。

### 3.3 辅助一级流程

辅助流程与核心流程 **平级但不平等**：结构上是同一层（有自己的核心对象和生命周期），但对业务价值的贡献权重较低。

| 流程 | 核心对象 | 性质 | 与核心流程的关系 | 适用场景 |
|------|---------|------|-----------------|---------|
| **逆向交易** | ReturnOrder（退货/退款单） | 价值止损 | 交易的逆向对应，但有独立的生命周期 | 所有电商 |
| **客户服务** | ServiceTicket（服务工单） | 价值维护 | 支撑用户发展（间接影响用户活跃和留存），但非子流程 | 所有电商 |
| **商家发展** | Merchant（商家） | 价值生态 | 平台型电商的核心，品牌电商弱相关 | 平台型电商 |
| **供应链采购** | PurchaseOrder（采购单） | 价值保障 | 保障商品供给 | 有自营仓储的电商 |

**辅助流程 vs 二级子流程的区别**：二级子流程是核心对象生命周期的一个 **阶段**（如支付是 Order 生命周期的一环，支付完成后 Order 继续前行）。辅助流程有自己 **独立的核心对象和生命周期**，它和核心流程通过事件 **关联** 而非 **包含**。

例如：一个工单的解决（TicketResolved）在客户服务流程中是里程碑，在用户发展流程中也是一个有意义的信号（可能影响用户活跃度评估）。两个流程通过事件交叉关联，但各自保持独立。

### 3.4 流程体系总览

```
一级业务流程
├── 核心流程（价值创造的主循环）
│   ├── 交易流程 ............. Order    ✅ 已实现
│   ├── 用户发展流程 ......... Customer 🔲 待建
│   ├── 商品运营流程 ......... Product  🔲 待建
│   └── 导购流程 ............. Shopping Journey  🔲 待建
│
└── 辅助流程（价值保障与支撑）
    ├── 逆向交易 ............. ReturnOrder  🔲 待建
    ├── 客户服务 ............. ServiceTicket  🔲 待建
    ├── 商家发展 ............. Merchant  🔲 待评估
    └── 供应链采购 ........... PurchaseOrder  🔲 待评估
```

---

## 四、事件分类与管理

### 4.1 事件元数据模型

扩展现有 EventMetadataRegistry，为每个事件声明其在各流程中的角色：

```
EventMetadata
├── eventType: String                          // 事件标识（如 "OrderCompleted"）
├── boundedContext: String                     // 技术来源 BC
├── label: String                              // 中文标签
├── category: EventCategory                    // NORMAL / COMPENSATION / EXCEPTION
├── compensatesEventType: String?              // 补偿目标（仅补偿事件）
│
├── origin: EventOrigin                        // ATOMIC / DERIVED
├── processRoles: Map<ProcessType, EventRole>  // 各流程中的角色
│     └── EventRole
│           ├── significance: MILESTONE / PROGRESSION
│           └── displayOrder: int
│
│   // 仅派生事件：
├── derivationMechanism: TRIGGER / ACCUMULATION / ABSENCE
└── sourceEventTypes: List<String>             // 推断来源
```

### 4.2 派生事件的溯源（Provenance）

每个派生事件实例在产生时携带溯源信息，支持下钻追溯：

```
Provenance
├── ruleId: String            // 产生规则标识（如 "active-user-rule-v1"）
├── ruleDescription: String   // 规则的人类可读描述
├── windowStart: Instant      // 评估窗口起点
├── windowEnd: Instant        // 评估窗口终点
├── triggerEventId: String?   // 触发本次评估的事件（缺失式为 null）
└── detectedAt: Instant       // 实际检测时间
```

**下钻原则**：派生事件不存储贡献事件的 ID 列表——具体的贡献事件在查询时通过 `溯源规则 + 评估窗口 + 关联键` 动态发现。这保持了事件之间的松耦合，也避免了原子事件被补偿后下钻结果不一致的问题。

下钻示例：

```
用户 X 的生命周期（里程碑视图）：

  ● UserRegistered          2024-01-01
  ● FirstPurchaseCompleted  2024-01-05    ← 下钻：OrderCompleted #1001
  ● BecameActiveUser        2024-01-27    ← 下钻：规则"30天内3笔订单"
  │   ├ OrderCompleted #1001  01-05            窗口 01-01 ~ 01-30
  │   ├ OrderCompleted #1025  01-18
  │   └ OrderCompleted #1042  01-27 (触发)
  ○ UserChurned             2024-05-15    ← 下钻：规则"90天无订单"
      └ 最后活动：01-27（110天前）
```

### 4.3 事件跨流程映射

同一事件在不同流程中的角色：

| 事件 | 交易 | 用户发展 | 商品运营 | 导购 |
|------|------|---------|---------|------|
| OrderCreated | 里程碑 | 过程 | 过程 | — |
| PaymentCompleted | 里程碑 | 过程 | — | — |
| OrderCompleted | 里程碑（终态） | 过程 | 过程 | — |
| FulfillmentDelivered | 里程碑 | — | — | — |
| StockReserved | 过程 | — | 过程 | — |
| UserRegistered | — | 里程碑 | — | — |
| FirstPurchaseCompleted | — | 里程碑 | — | — |
| BecameActiveUser | — | 里程碑 | — | — |
| UserChurned | — | 里程碑 | — | — |
| ProductListed | — | — | 里程碑 | — |
| ProductViewed | — | 过程 | 过程 | 过程 |
| SearchPerformed | — | 过程 | — | 过程 |
| JourneyConverted | — | — | — | 里程碑 |

> 表中仅列出主要事件的映射关系。新增事件时应同步更新此映射。

### 4.4 现有事件 payload 的多流程自描述性评估

场景验证发现：现有事件的 payload 是为交易流程（orderId 关联）设计的，部分事件在商品运营和用户发展维度上不满足自描述原则。

| 事件 | 现有 payload | 商品运营所需 | 用户发展所需 | Step 1 改进 |
|------|-------------|------------|------------|------------|
| OrderCreated | orderId, items[{skuId, qty}] | ⚠️ 缺 spuId、价格 | ⚠️ 缺 userId | 增加 userId；items 增加 spuId、unitPriceCents |
| OrderCompleted | orderId | ⚠️ 缺 skuId/spuId | ⚠️ 缺 userId | 增加 userId、totalAmountCents + items[{spuId, skuId, qty, unitPriceCents}] |
| OrderCancelled | orderId | ⚠️ 缺 skuId/spuId | ⚠️ 缺 userId | 增加 userId、totalAmountCents + items[{spuId, skuId, qty, unitPriceCents}] |
| StockReserved | orderId, items[{skuId, qty}] | ⚠️ 缺 spuId | — | items 增加 spuId |
| StockReleased | orderId | ⚠️ 缺 skuId/spuId | — | 增加 items[{spuId, skuId, qty}] |
| PaymentCompleted | orderId, amountCents | — | ⚠️ 缺 userId | 可在消费侧通过 orderId 关联 |

改进原则：**一步到位**，一次变更中完整增加所有多流程所需字段，避免后续再改 schema。遵循事件演进兼容性原则（第六章 6.3）：新增字段为可选，不破坏现有消费者。

---

## 五、智能化应用分层

以业务流程为中心的事件架构为智能化运营提供了基础。智能化能力按递进层次构建，每一层都对所有一级流程通用。

### 5.1 Level 1：流程效率分析（Descriptive Analytics）

> 回答"过去发生了什么"。

通过对流程事件的统计和聚合，量化流程的运行效率。

| 流程 | 分析维度示例 |
|------|------------|
| 交易 | 下单→支付转化率、平均支付耗时、履约周期、退款率 |
| 用户发展 | 注册→首购转化率、首购转化周期、活跃率、流失率、促活成功率 |
| 商品运营 | 商品从上架到首销的周期、各阶段商品分布、滞销率 |
| 导购 | 搜索→购买转化率、平均决策周期、购物车放弃率 |

**实现路径**：扩展 Activity BC 的统计 API，增加按流程维度的聚合查询。Smart Interaction BC 通过 MCP Tool 调用这些 API，支持自然语言查询（如"最近一周交易流程的转化率如何"）。

### 5.2 Level 2：实时监控与预警（Real-time Monitoring）

> 回答"正在发生什么异常"。

在事件消费链路上增加规则评估，实时检测流程异常：

| 流程 | 监控规则示例 |
|------|------------|
| 交易 | 支付失败率 30 分钟内超过 20%；某订单 PaymentCompleted 后 10 分钟无 FulfillmentOrderCreated |
| 用户发展 | 本周新注册用户 7 天首购转化率低于历史均值 50% |
| 商品运营 | 某 SKU 库存占用量 1 小时内超过安全水位；热销商品突然零销量 |
| 导购 | 某商品详情页浏览→加购转化率骤降 |

**实现路径**：Activity BC 事件消费链路中增加规则评估器。规则应为声明式、可配置的阈值规则，避免硬编码。预警结果通过事件发布（AlertTriggered），可被通知系统或 Smart Interaction 消费。

### 5.3 Level 3：动态洞察与改进建议（Prescriptive Analytics）

> 回答"为什么异常，应该怎么改进"。

将流程知识注入 Smart Interaction，让 LLM 基于实时数据提供洞察：

```
运营者 → Smart Interaction → LLM
              ↓
         MCP Tools:
         ├── activity_query        （查事件明细）
         ├── activity_stats        （查统计指标）
         ├── process_analysis      （流程效率分析）
         └── process_alerts        （当前预警）
```

示例对话：
- "交易流程最近有什么异常？" → 调用 process_alerts，结合历史数据给出分析
- "用户 X 处于什么生命周期阶段？为什么？" → 调用 activity_query + userId，解读里程碑
- "上架 3 周仍无销量的商品有哪些？可能原因？" → 调用 process_analysis，对比同类商品

### 5.4 Level 4：自主策略调整（Autonomous Intelligence）

> 不再被动回答，而是主动观察、决策、行动。

**前置条件：跨流程关联**。单流程内的自主行动价值有限——真正有意义的自主策略往往跨越多个流程。例如：商品库存告急（商品运营）导致用户流失（用户发展），系统应在补货到位后自动对受影响用户进行促活（跨流程联动）。因此 Level 4 应在多个流程的 Level 2/3 能力成熟后再引入。

自主策略的结构：**触发条件（多个派生事件的组合）+ 行动 + 安全边界**。

| 策略示例 | 触发条件 | 行动 | 涉及流程 |
|---------|---------|------|---------|
| 库存联动促活 | 商品补货到位 + 存在因缺货流失的用户 | 推送到货通知 | 商品运营 × 用户发展 |
| 滞销商品降权 | 销量持续下滑 + 转化率下降 | 降低推荐权重 | 商品运营 × 导购 |
| 高价值用户拦截 | 忠诚用户活跃度连续下降 | 生成个性化优惠方案 | 用户发展 × 交易 |

**审慎原则**：
- 每个自主行动需要明确的安全边界（什么可以自动做、什么需要人工审批）和回滚机制
- 每个自主行动记录为事件（StrategyExecuted），可回溯和审计
- 在 Level 3 充分成熟后渐进引入 Level 4

### 5.5 分层小结

```
Level 4: 自主策略 ← Agent 自主行动，受控边界
Level 3: 动态洞察 ← LLM + 流程知识 + 实时数据
Level 2: 实时监控 ← 规则引擎 + 预警
Level 1: 效率分析 ← 统计聚合 + 报表
─────── 基础设施 ← 事件平台 + 流程元数据
```

每一层都依赖下一层的能力，逐层构建，逐层交付价值。

---

## 六、EDA 设计原则

### 6.1 事件设计原则

| 原则 | 要求 | 说明 |
|------|------|------|
| **不可变** | 事件是已发生的事实，不可撤回 | 只能发布新事件来表达后续事实（如 Refunded 对应 Completed） |
| **自描述** | 事件携带足够信息，消费者无需回查 | PaymentCompleted 应含 amountCents，不能只有 orderId |
| **松耦合** | 生产者不知道消费者，消费者不知道生产者的实现 | 唯一契约是事件的结构（schema） |
| **单一职责** | 事件只陈述事实，不指挥行动 | 消费者自行决定如何反应 |
| **幂等消费** | 同一事件被消费两次的效果与一次相同 | 通过 eventId 判重 |
| **最终一致** | 接受事件传播的延迟 | 各消费者最终都会收到事件并更新状态 |

### 6.2 编制与编排决策框架

系统中同步调用（编制）与事件驱动（编排）共存。选择依据：

**选编制（Orchestration）当**：
- 调用方需要即时反馈（用户在等待）
- 操作之间有严格的因果依赖（A 失败则不做 B）
- 协调者掌握上下文知识（只有它知道怎么拆分）
- 需要事务性保证

**选编排（Choreography）当**：
- 过程跨时间跨度大（分钟、小时、天）
- 多个独立消费者需要反应
- 生产者不应了解消费者的存在
- 消费者可以独立失败和重试

**选混合（事件触发编制）当**：
- 一个异步信号到达后需要执行一系列有因果依赖的同步操作

> **一句话总结**：编制处理"怎么做"（how），编排处理"然后呢"（what's next）。

当前系统的实践：

| 场景 | 选择 | 理由 |
|------|------|------|
| PlaceOrder 内部（占库存→创支付单） | 编制 | 用户在等、有因果依赖、Order 掌握上下文 |
| Payment → Order（支付结果） | 编排 | 时间不确定、多消费者、领域边界 |
| PaymentCompleted 后创建履约单 | 混合 | 异步信号到达后，Order 同步编制 Fulfillment |
| 所有 BC → Activity | 编排 | 纯监听、无反向依赖 |

### 6.3 事件演进的兼容性原则

事件 schema 会随业务演进而变化。为保证消费者不因 schema 变更而中断：

| 原则 | 说明 |
|------|------|
| **只增不删** | 可以新增字段，不要删除或重命名已有字段 |
| **新字段可选** | 新增字段必须有默认值或允许为空，消费者可忽略 |
| **语义不变** | 已有字段的含义不能改变；如需改变语义，用新字段 |
| **破坏性变更需新事件** | 如果 schema 变更不兼容，发布新事件类型（如 OrderCreatedV2） |

---

## 七、架构演进路线

### 设计原则

- **从第一步起就建立多流程基座**，而不是先做完一个流程再做下一个。用户运营和商品运营共享同一批原子事件（OrderCreated 既是商品的"被购买"信号，也是用户的"又下了一单"信号），扩展一次事件两个流程同时受益。
- **事件 payload 增强一步到位**：既然要让事件自描述地支撑多流程，就在一次变更中完整增加 userId、spuId、skuId 等字段，避免后续再改 schema。
- **Smart Interaction 始终是可见出口**：Step 1 完成后立即接入 Smart Interaction，让「用自然语言查事件」成为统一可见出口；此后每个 Step 的新数据与 API 同步增强 AI 能力，确保每步做完都能在对话中感知到变化。
- **每步同时交付：后端数据 + 前端展示 + AI 可感知**：三者缺一都会导致结果不直观。
- **导购流程放在最后**：它依赖行为事件采集（前端埋点），基础设施投入最大，且前三个流程的智能化已能交付核心业务价值。
- **迭代式推进**：每步执行前通过 `analyze-requirement` 产出详细设计，执行时通过 `deliver-requirement` 编排交付。后续 Step 的具体方案基于前一步的反馈来调整，此处仅定义方向和范围。

---

### Step 1：多维事件基座 ✅

> **看得到** — 让每个事件同时能被"哪笔订单"、"哪个用户"、"哪个商品"三个角度查询。

| # | 交付物 | 说明 |
|---|--------|------|
| 1a | 增强事件 payload（一步到位） | OrderCompleted、OrderCancelled 增加 `userId`、`totalAmountCents` 和 `items[{spuId, skuId, quantity, unitPriceCents}]`；OrderCreated 增加 `userId`，items 增加 `spuId` 和 `unitPriceCents` |
| 1b | BusinessActivity 扩展 | 新增 `userId` 列和 `correlationKeys` JSON 列（存储 spuId、skuId 等） |
| 1c | Activity 消费侧提取多维关联 | 从 payload 提取 userId、spuId、skuIds 写入对应字段 |
| 1d | Activity API 新增查询维度 | `GET /api/activities?userId={id}`、`GET /api/activities?skuId={id}`、`GET /api/activities?spuId={id}` |
| 1e | EventMetadataRegistry 扩展 | 新增 `processRoles`、`origin` 等字段；为现有 14 种事件标注流程归属 |
| 1f | Admin 前端多维查询 | Activity 页面增加 userId / skuId / spuId 查询入口 |

**前置依赖**：无。

**可见结果**：
- Admin Activity 页面输入 userId → 看到该用户的所有交易事件序列
- 输入 skuId 或 spuId → 看到该商品的所有交易事件序列

---

### Step 2：Smart Interaction 接入（最小可用）

> **问得到（初版）** — 用自然语言查询事件，AI 始终是数据的可见出口。后续每个 Step 交付后同步扩展 AI 能力。

| # | 交付物 | 说明 |
|---|--------|------|
| 2a | Activity MCP Tools | 封装 Activity API：按 orderId/userId/skuId/spuId 查事件序列、近期事件列表、统计数据；供 AI 回答「用户 X 发生了什么」「商品 Y 关联了哪些订单」「最近有哪些事件？」 |
| 2b | 流程知识 MCP Resource | 一级流程定义、现有事件类型与含义、EventMetadata processRoles 说明；注入 LLM 上下文，使回答带流程语义而非仅列举数据 |

**前置依赖**：Step 1（提供多维 Activity API）。

**可见结果**（对话示例）：
- "订单 #123 的完整旅程？" → 按 orderId 查事件并逐步解读
- "用户 42 最近有哪些交易事件？" → 按 userId 查事件，AI 指出哪些是交易流程里程碑
- "SKU 1001 关联了哪些订单？" → 按 skuId 查事件，呈现商品被购买的完整记录

---

### Step 3：生命周期 + Level 1 分析

> **看得懂** — 运营者能看到每个用户处于什么生命周期阶段、每个商品处于什么经营阶段；AI 能解读里程碑含义。

| # | 交付物 | 说明 |
|---|--------|------|
| 3a | Catalog BC 新增领域事件 | `ProductListed`（上架）、`ProductDelisted`（下架），发布到 Kafka，Activity 订阅 |
| 3b | 触发式派生事件：FirstPurchaseCompleted | 用户首次出现在 OrderCompleted 中 → 自动识别并记录为派生事件 |
| 3c | 触发式派生事件：FirstSaleCompleted | 某 SPU 首次出现在 OrderCreated 中 → 自动识别并记录 |
| 3d | Activity 统计 API 扩展 | 用户维度：注册数、首购转化数/率/周期；商品维度：上架数、有首销商品数、首销周期、SKU 销量排行 |
| 3e | Admin 仪表盘增强 | 新增"用户转化漏斗"和"商品经营概览"卡片 |
| 3f | Smart Interaction 同步增强 | 更新流程知识 Resource（补充派生事件含义与首购/首销定义）；AI 在解读用户事件时能识别并说明 FirstPurchaseCompleted 里程碑含义；询问「用户 42 是否首购用户？」时能直接回答 |

**前置依赖**：Step 1（多维 Activity API），Step 2（Smart Interaction 已接入）。

**可见结果**：
- 仪表盘展示本周注册→首购转化率和转化周期
- 仪表盘展示上架商品数、有首销商品数、SKU 销量 Top 10
- 用户事件序列中出现 `FirstPurchaseCompleted` 里程碑
- 商品事件序列中出现 `FirstSaleCompleted` 里程碑
- AI 能回答「用户 42 什么时候首购？」「哪些商品还没首销？」

---

### Step 4：派生引擎 + Level 2 监控

> **看得快** — 系统自动识别关键信号并主动预警；AI 能解读预警并给出溯源。

| # | 交付物 | 说明 |
|---|--------|------|
| 4a | 派生事件引擎（最小可用版） | Activity BC 内构建，支持触发式（复用 Step 3）、累积式、缺失式三种机制；规则以代码注册 |
| 4b | 用户发展派生事件 | `BecameActiveUser`（累积式：30天内 ≥ 3 笔 OrderCompleted）、`UserChurned`（缺失式：90天无 OrderCompleted，定时任务每日扫描） |
| 4c | 商品运营派生事件 | `ProductStockCritical`（触发式：StockReserved 后剩余库存 < 3天日均销量）、`ProductSalesDeclined`（累积式：周销量环比下降 > 50%） |
| 4d | 预警机制 | 派生事件产生时同时写入预警记录；Activity API 新增 `GET /api/activities/alerts`；Admin Activity 页面新增"预警"标签页 |
| 4e | Smart Interaction 同步增强 | 新增 `activity_alerts` MCP Tool；更新流程知识 Resource（补充派生规则与预警含义）；AI 能主动汇报预警并按 userId/skuId 溯源到具体事件 |

**前置依赖**：Step 3（统计 API 提供日均销量等基础数据）。

**可见结果**：
- 预警页展示"用户 #42 已流失（90天无购买）"、"SKU #1001 库存告急（预计2天售罄）"
- 用户/商品事件序列中出现 BecameActiveUser、UserChurned、ProductStockCritical 里程碑，可下钻查看溯源事件
- AI 能回答「最近有哪些预警？」「用户 42 为什么被标记流失？」

---

### Step 5：跨流程洞察 + Level 3 深度分析

> **问得到（完整）** — AI 能跨越用户、商品、交易三个流程做关联分析，回答「为什么」并给出建议。

| # | 交付物 | 说明 |
|---|--------|------|
| 5a | 跨流程 MCP Tools | `process_user_analysis`（用户生命周期状态、里程碑序列、关联商品）、`process_product_analysis`（商品经营阶段、销量趋势、关联买家概况）；跨流程关联：查商品时可关联买家的用户状态，查用户时可关联买过的商品状态 |
| 5b | 流程知识 Resource（完整版） | 在 Step 2 基础上补充：生命周期阶段定义、派生规则说明、跨流程关联逻辑；使 LLM 能进行真正的流程推理 |

**前置依赖**：Step 3（统计 API）、Step 4（预警 API + 派生事件数据）。

**可见结果**（对话示例）：

```
运营者: "最近有什么需要关注的？"
AI: 当前有 3 条预警：
  1. 🔴 SKU #1001 库存告急，预计 2 天售罄
  2. 🟡 SPU #101 周销量环比下降 75%
  3. 🟡 12 位用户本周被标记为流失
  其中 1 和 2 可能相关——库存不足可能拉低了整体销量。

运营者: "流失的 12 位用户有什么共同点？"
AI: 8 位的最后一笔订单都包含 iPhone 配件类商品，
    其中 5 位在流失前曾下单 iPhone 16 Pro 但因库存不足被取消。
    建议：补货到位后对这 5 位进行定向促活。
```

---

### Step 6：行为事件 + 导购流程

> **看全貌** — 补上"用户在买之前做了什么"，覆盖从浏览到成交的全链路。

| # | 交付物 | 说明 |
|---|--------|------|
| 6a | 前端行为事件采集 | `ProductViewed`（浏览商品详情页）、`SearchPerformed`（搜索）、`CartItemAdded`（加购）；前端 SDK → Kafka → Activity |
| 6b | 新增派生事件 | `ProductConversionDropped`（浏览→下单转化率周环比下降 > 40%）、`UserRescued`（流失用户重新完成 OrderCompleted） |
| 6c | 导购流程 Level 1 分析 | 搜索→浏览→加购→下单转化漏斗；各商品/品类转化率排行；Admin 仪表盘新增"转化漏斗"卡片 |
| 6d | Smart Interaction 同步增强 | AI 能回答"用户在购买前看了哪些商品"、"为什么加购后没下单"、"这个商品浏览量高但转化率低的原因" |

**前置依赖**：Step 5（跨流程分析能力就绪后，导购流量数据才有关联价值）。

**可见结果**：
- 仪表盘新增"转化漏斗"：搜索→浏览→加购→下单各步转化率
- AI 能回答「用户 42 在买这个商品之前浏览了什么？」「这个 SPU 浏览量高但购买率低，可能原因是？」

---

### Step 7：策略引擎 + Level 4 自主智能

> **自己动** — 系统在安全边界内自动处理跨流程问题，并留下可审计的行动日志。

| # | 交付物 | 说明 |
|---|--------|------|
| 7a | 策略引擎 | 策略 = 触发条件（派生事件组合）+ 行动 + 安全边界；策略注册表声明式配置 |
| 7b | 首批跨流程自主策略 | 见下表 |
| 7c | 行动日志与审计 | 每个自主行动记录为 `StrategyExecuted` 事件，可回溯；运营者可查看、暂停、回滚策略 |

首批策略（每个都跨越至少两个流程）：

| 策略 | 触发条件 | 行动 | 安全边界 | 涉及流程 |
|------|---------|------|---------|---------|
| 库存联动促活 | ProductStockCritical 解除（补货到位）+ 存在因缺货流失的用户 | 向相关用户推送到货通知 | 推送人数 ≤ 50/次 | 商品运营 × 用户发展 |
| 滞销商品降权 | ProductSalesDeclined 持续 2 周 + ProductConversionDropped | 降低推荐权重 | 权重调整 ≤ 50% | 商品运营 × 导购 |
| 新品冷启动 | ProductListed 后 7 天无 FirstSaleCompleted | 提升推荐曝光 + 通知运营关注 | 仅建议，不自动执行 | 商品运营 × 导购 |
| 高价值用户流失拦截 | BecameLoyal 的用户活跃度连续 2 周下降 | 生成个性化优惠方案 + 通知运营审批 | 需人工审批 | 用户发展 × 交易 |

**前置依赖**：Step 6（导购流程数据，策略才能跨越全部四个流程）。

**可见结果**：
- 策略日志展示"iPhone 16 Pro 已补货，已向 5 位潜在用户发送到货通知"
- 运营者可在策略管理页查看所有自动执行的决策、触发原因、执行效果

---

### 演进路线图

```
Step 1       Step 2      Step 3          Step 4          Step 5         Step 6         Step 7
多维基座      SI 接入     生命周期+L1      派生引擎+L2      跨流程 L3      行为+导购      自主智能 L4
─────────────────────────────────────────────────────────────────────────────────────────→

              ↑ 从 Step 2 起，每步完成后 AI 能力同步增强

流程覆盖:
  交易    ██████████████████████████████████████████████████████████████████████████
  用户       ░░░░░ ▓▓▓▓▓▓▓▓▓▓▓▓▓ ████████████████ ████████████ █████████████████
  商品       ░░░░░ ▓▓▓▓▓▓▓▓▓▓▓▓▓ ████████████████ ████████████ █████████████████
  导购                                                            ▓▓▓▓▓▓▓ ████████

智能层级:
  AI查询        ▓▓▓▓▓▓ █████████████ ████████████████ ████████████ █████████████████
  L1 分析               █████████████ ████████████████ ████████████ █████████████████
  L2 监控                              ████████████████ ████████████ █████████████████
  L3 洞察                                               ████████████ █████████████████
  L4 自主                                                                      ████████

░░ = 基础可见   ▓▓ = 初步能力   ██ = 完整能力

每步的可见结果：
  Step 1: 看得到 — 三个维度查事件（admin 仪表盘）
  Step 2: 问得到 — 自然语言查事件，AI 始终是可见出口
  Step 3: 看得懂 — 生命周期阶段、关键指标；AI 解读首购/首销
  Step 4: 看得快 — 预警页主动提示；AI 解读预警并溯源
  Step 5: 问深了 — AI 跨流程关联分析，给出「为什么」和建议
  Step 6: 看全貌 — 浏览到成交全链路，转化漏斗
  Step 7: 自己动 — 安全边界内自主行动，可审计
```

---

## 八、维护规则

| 时机 | 动作 |
|------|------|
| 新增一级业务流程 | 在第三章增加流程定义；更新第四章事件映射；评估智能化场景 |
| 新增事件类型 | 更新 EventMetadataRegistry（含 processRoles）；更新第四章事件映射表和 payload 评估表 |
| 新增派生事件 | 定义产生规则和溯源结构；更新对应流程的里程碑事件表 |
| 完成某 Step | 更新第七章对应 Step 的状态标记（🔲→✅） |
| 增强事件 payload | 更新第四章 payload 评估表；确认兼容性（第六章 6.3） |
| 智能化能力新增 | 更新第五章对应 Level 的实现说明 |
