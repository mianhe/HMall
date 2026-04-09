# 用户定向与满件折扣（User Targeting & Piece-Based Discount）

## 一、需求概述与场景

### 业务背景与目标

促销体系前两期已经具备“活动 + 券统一算价”能力，但当前规则对“谁在买”感知不足，且订单维度仅支持满减（按金额门槛），无法支持常见的“指定人群专享活动”“买 N 件打折/减免”等运营策略。  
本需求作为 [促销体系](../theme.md) 的业务需求 3，目标是补齐两类能力：

- **用户定向**：基于用户标签/等级决定活动与券是否可命中（如新客、会员等级、渠道人群）
- **满件折扣**：在金额门槛之外，支持按件数触发优惠（如“满 3 件减 30”“第 2 件半价”）

业务目标：

1. 让运营可配置“人群 + 规则”组合，提高活动投放精准度
2. 保持“统一价格引擎”单一出口，不把促销规则分散到 Order/Cart/Web
3. 在不改变主交易链路的前提下，扩展 Promotion 的规则表达能力

### 需求类型

**扩展已有能力**（基于业务需求 2）。  
核心变化不是新增交易路径，而是扩展 Promotion 价格引擎的命中条件与规则类型。

### 与已有能力的核心区别

| 维度 | 业务需求 2（活动与价格引擎） | 本需求（用户定向与满件折扣） |
|------|-----------------------------|-----------------------------|
| 活动命中条件 | 商品/订单金额 + 时间 + 互斥组 | 叠加“用户标签/等级”定向条件 |
| 规则类型 | 单品直降、订单满减（金额） | 新增满件规则（件数门槛） |
| 用户建模 | userId 仅用于券归属 | userId 需可映射到标签/等级用于定向判断 |
| 运营配置 | 活动基本配置 + 互斥组 | 增加定向条件配置与可视化反馈 |

### 影响面

| 影响范围 | 影响程度 | 说明 |
|---------|---------|------|
| **Promotion** | 🔴 重大 | 新增定向规则表达、满件折扣规则、命中判定扩展 |
| **User** | 🟡 中等 | 需要提供用户标签/等级查询能力（供 Promotion 判定） |
| **Order** | 🟢 轻微 | 调用方式保持不变；接收新增规则计算出的折扣明细 |
| **Cart** | 🟢 轻微 | 调用方式保持不变；展示结果来自统一算价输出 |
| **BFF** | 🟢 轻微 | 增加后台配置与前台查询所需路由代理（若 API 新增） |
| **前端（admin）** | 🟡 中等 | 活动/券配置页增加“定向条件”“满件规则”配置 |
| **前端（web）** | 🟡 中等 | 价格展示文案与可用券/活动说明需体现“人群定向与件数门槛” |

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | 定向活动 + 满件折扣下单 | 主流程 | L3 重分析 | 用户下单时由 Promotion 先做人群命中判定，再计算满件折扣并输出实付 |
| F2 | 定向规则配置（活动/券） | 支撑流程 | L2 中分析 | 运营在后台配置“可命中人群”，控制规则生效范围 |
| F3 | 满件折扣规则配置与生效 | 支撑流程 | L2 中分析 | 运营配置件数门槛与优惠形式，统一进入价格引擎 |
| F4 | 前台价格与可用权益展示 | 支撑流程 | L2 中分析 | 列表/详情/购物车/结账展示“预计到手价 + 命中条件说明” |
| F5 | 定向不命中/规则冲突处理 | 异常流程 | L2 中分析 | 用户不在人群内、件数未达门槛、并发配置变更时以后端重算为准 |

### `business-flows.md` 检查清单（第七章）初判

1. **影响哪段？** 主要影响 N2O（选品与决策 + 下单算价）；O2F 不变。  
2. **影响选品与决策阶段吗？** 是。详情/购物车/结账的展示与说明文案需要体现“是否命中定向”和“件数门槛”。  
3. **影响后台管理流程吗？** 是。活动与券配置需新增定向与满件规则字段。  
4. **影响哪些事件？** 订单事件名称不变，但折扣明细来源会新增“定向活动/满件活动”语义。  
5. **新增路径还是影响现有路径？** 影响现有 N2O 路径，不新增独立主路径。  
6. **是否打破 N2O ⊥ O2F？** 否。定向判定和满件计算在 N2O 结束前完成，O2F 继续消费订单快照。  
7. **测试覆盖是否仍完整？** 需要新增 Business E2E 覆盖“命中定向”“不命中回退”“满件门槛边界”三类场景。

---

## 二、场景分析（事件流）

### F1：定向活动 + 满件折扣下单（L3）

以 `business-flows.md` 现有 N2O 主链路为基线，变化集中在 PlaceOrder 前的统一算价阶段。

#### 主成功路径

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|----|---------|
| 1 | — | ⌘ PlaceOrder(items, address, couponId?) | 前端提交商品项、数量、可选 couponId | Order | ✅ 入口不变 |
| 2 | — | → 同步 CalculatePrice(items, userId, couponId?) | Promotion 先做用户定向命中判定，再按互斥组挑选活动，最后叠加券抵扣 | Promotion | 🔄 算价规则扩展 |
| 3 | — | (2.1) FetchUserSegments(userId) | 查询用户标签/等级（如 NEW_USER、VIP、L2）作为命中条件 | User | 🆕 新增上游数据依赖 |
| 4 | — | (2.2) EvaluateActivityTargeting | 仅命中目标人群的活动进入候选集 | Promotion | 🆕 新增定向门禁 |
| 5 | — | (2.3) EvaluatePieceBasedRule | 对候选活动执行“满 N 件”判定并计算折扣 | Promotion | 🆕 新增规则类型 |
| 6 | — | (2.4) ApplyCouponOnPostActivityAmount | 券门槛仍基于“活动后金额”判断 | Promotion | ✅ 沿用既有 |
| 7 | 🟧 StockReserved | → 同步 OccupyStock(orderId, physicalItems) | 仅 PHYSICAL 占库存 | Inventory | ✅ 既有 |
| 8 | — | → 同步 LockCoupon(couponId, orderId) | 有券才锁定 | Promotion | ✅ 既有 |
| 9 | 🟧 OrderCreated | → CreatePayment(payableAmountCents) → 保存/发布 | `OrderLineItem.discounts` 增加定向活动/满件来源明细 | Order | 🔄 折扣来源扩展 |
| 10 | 🟧 PaymentCompleted | 网关回调 | Order 置 PAID | Payment | ✅ 既有 |
| 11 | — | → 同步 RedeemCoupon(couponId) | 有券才核销 | Promotion | ✅ 既有 |
| 12 | — | → 同步 CreateFulfillment | 履约不感知定向条件，只消费订单快照 | Fulfillment | ✅ 不变 |
| 13 | 🟧 ... → OrderCompleted | 后续履约流程 | — | Order | ✅ 不变 |

> **决策 UT1**：定向判定只在 Promotion BC 内执行，Order/Cart/Web 不复制“用户标签匹配”逻辑，保持“一个规则引擎、一个真相源”。

> **决策 UT2**：用户定向数据采用同步查询（Promotion -> User），不在 Promotion 侧持久化用户标签快照。这样标签变更可实时生效；订单最终仍以后端提交时重算为准。

> **决策 UT3**：满件折扣作为活动规则的一种（而不是新建平行优惠体系），继续复用“互斥组择优 + 跨组叠加 + 券后置”框架，避免规则分叉。

#### 补偿与逆向路径

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 与现有差异 |
|---|----------|-----------|-----------------|----|-----------|
| 1 | 🟧 PaymentExpired | 超时检测 | 触发 CancelOrder | Payment | ✅ 既有 |
| 2 | — | ⌘ CancelOrder | 释放库存 + 释放券 + 退款（如已支付） | Order | ✅ 既有 |
| 3 | — | PlaceOrder 前重算失败 | 若用户标签变化导致不再命中，返回“优惠条件已变化” | Promotion | 🆕 新增失败语义 |
| 4 | — | LockCoupon 失败 | 释放已占库存并返回“权益已变化” | Order | ✅ 既有补偿语义扩展 |

#### 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 3 | 用户是否命中定向 | userId 对应标签集合、会员等级 | User BC | ❌ 需新增“用户标签/等级查询能力” |
| 4 | 活动定向过滤 | 活动目标人群条件表达式 | Promotion 活动配置 | ❌ 需扩展 PromotionActivity 模型 |
| 5 | 满件是否达标 | 按规则分组后的购买件数 | Order items 入参 | ❌ 需新增 PieceBasedRule 规则模型 |
| 5 | 同组择优 | 同互斥组候选活动的可减金额 | Promotion 价格引擎 | 🔄 需在择优策略纳入“件数规则” |
| 9 | 订单折扣可追踪 | 折扣来源类型/规则 ID/说明 | Promotion 算价响应 | 🔄 需扩展 DiscountDetail 元数据 |

---

### F2：定向规则配置（活动/券）（L2）

1. 运营在 admin 配置活动或券时，可选“适用人群”：用户等级、标签集合、黑白名单。  
2. Promotion 保存为统一的 `TargetingRule` 表达（支持 AND/OR 组合，首版限制为“同维度 OR、跨维度 AND”）。  
3. 活动/券仅在用户满足规则时可命中：
   - 不满足时在算价中直接排除  
   - 前端可显示“当前账号不满足使用条件”

**关键数据流**：
- admin -> Promotion：写入定向配置
- Promotion -> User：算价时读取用户标签/等级

> **决策 UT4**：定向规则首版不做通用脚本引擎，使用结构化字段（levelIn/tagsAny/tagsAll/excludeTags）表达，避免运维难度和解释成本。

---

### F3：满件折扣规则配置与生效（L2）

1. 运营创建活动时可选择“满件折扣”类型，配置：
   - 作用范围（指定 SKU / 指定 SPU / 全订单）
   - 件数门槛（`minQuantity`）
   - 优惠方式（固定减免 / 按比例折扣）
   - 可选上限（`maxDiscountCents`）
2. Promotion 在统一算价时按 scope 统计件数并判定是否达门槛。  
3. 命中后折扣按行分摊，继续参与互斥组与跨组叠加流程。

**关键边界**：
- 满件按“有效商品行”计数（不含 SERVICE 行）  
- 同一个活动只能配置一种主规则（避免一个活动内混合多种件数规则）

---

### F4：前台价格与可用权益展示（L2，前端为主）

**页面数据流**：

1. 列表/详情调用活动价预估接口时，带 `userId` 上下文，返回“是否命中定向 + 命中后价格 + 条件提示”。  
2. 购物车/结账调用统一算价，展示“活动优惠（含满件）+ 券优惠 + 实付”。  
3. 结账提交时由 Order 再次调用 Promotion 重算，确保最终一致。

**关键组件边界**：
- 商品卡片：活动标签支持“会员专享”“新客专享”“满 3 件减 30”  
- 结账页权益区：展示不可用原因（未达件数、等级不足、标签不匹配）

---

### F5：定向不命中/规则冲突处理（L2）

1. 用户在页面停留期间等级或标签变化：提交订单时后端重算为准。  
2. 运营并发修改活动配置：以 PlaceOrder 时刻读取到的活动快照为准。  
3. 满件门槛边界（N-1/N/N+1）必须稳定，尾差分摊不影响总额守恒。  
4. 当“定向活动不命中”但“非定向活动命中”时，应正常回退到后者，不应返回错误。

---

### 查询影响

| 查询场景 | 变化 |
|---------|------|
| 列表/详情活动价预估 | 返回 `eligible` 与 `ineligibleReason`（可选） |
| 可用券列表 | 新增定向过滤（不满足人群时不返回） |
| 购物车/结账价格摘要 | 活动优惠中可包含“满件折扣”来源说明 |
| 订单详情 | 折扣明细包含规则来源（如 ACTIVITY_PIECE） |

### 流程间耦合

- **F2/F3 -> F1/F4**：运营配置实时影响下单与展示，前端不缓存长期规则快照。  
- **F4 -> F1**：展示仅为预估，最终以后端提交时重算。  
- **F5**：通过“提交时重算 + 错误语义可解释”兜底，不引入新的跨 BC 补偿链。

---

## 三、变更分析

### Promotion（🔴 重大，🔄 需调整：定向判定 + 满件规则）

#### 影响程度

- ✅ 可复用：活动状态机、互斥组框架、券后置抵扣、统一算价入口  
- 🔄 需调整：活动规则表达、算价候选筛选、折扣来源语义  
- 🔲 全新：TargetingRule、PieceBasedRule

#### 领域模型变更

- `PromotionActivity` 扩展字段：
  - `targetingRule: TargetingRule?`
  - `pieceRule: PieceBasedRule?`
  - `ruleType: SKU_AMOUNT_OFF | ORDER_AMOUNT_OFF | PIECE_DISCOUNT`
- 新增值对象 `TargetingRule`
  - `levelsIn: Set<String>?`
  - `tagsAny: Set<String>?`
  - `tagsAll: Set<String>?`
  - `excludeTags: Set<String>?`
- 新增值对象 `PieceBasedRule`
  - `scopeType: SKU | SPU | ORDER`
  - `scopeIds: Set<Long>?`
  - `minQuantity: Integer`
  - `discountType: AMOUNT_OFF | PERCENTAGE_OFF`
  - `discountValue: Long/Decimal`
  - `maxDiscountCents: Long?`
- `CouponTemplate` 可选扩展：
  - `targetingRule: TargetingRule?`（支持定向发券/定向使用）

#### 事件流变更

- 扩展 API：
  - `POST /api/promotion/activities`：请求体支持 `targetingRule`、`pieceRule`
  - `PUT /api/promotion/activities/{id}`：支持更新上述配置
  - `POST /api/promotion/calculate-price`：响应增加 `ineligibleReasons` 与增强折扣来源
  - `POST /api/promotion/preview-sku-prices`：支持返回“是否命中定向”
- 新增内部集成：
  - `Promotion -> User`: `GET /api/users/{userId}/segments`（建议）

#### 需求场景变更

- 🔲 新增：配置定向活动（标签/等级）并生效
- 🔲 新增：配置满件活动并命中
- 🔲 新增：定向不命中时活动被排除
- 🔄 修改：统一算价返回可解释的“不命中原因”
- 🔄 修改：折扣明细语义支持满件来源

---

### User（🟡 中等，🔄 需调整：对外提供分群信息）

#### 影响程度

- ✅ 可复用：用户身份体系（userId）  
- 🔄 需调整：新增“标签/等级”查询能力

#### 领域模型变更

- `User` 聚合新增：
  - `level: String`（如 L1/L2/L3）
  - `tags: Set<String>`（如 NEW_USER、VIP、CHANNEL_xxx）

#### 事件流变更

- 新增查询 API（建议）：
  - `GET /api/users/{userId}/segments`
  - 响应：`{ userId, level, tags[] }`

#### 需求场景变更

- 🔲 新增：查询用户分群信息（供 Promotion 算价同步调用）
- 🔲 新增：无标签用户返回空集合而非错误

---

### Order（🟢 轻微，✅ 可复用为主）

#### 影响程度

- ✅ 可复用：PlaceOrder/CancelOrder/PaymentCompleted 主流程，调用顺序不变  
- 🔄 需调整：折扣明细来源扩展（可解释性增强）

#### 领域模型变更

- `DiscountDetail` 建议扩展：
  - `subType: ACTIVITY_DIRECT | ACTIVITY_PIECE | COUPON`
  - `ruleId: Long?`
  - `description: String`（继续保留）

#### 事件流变更

- `PromotionPricePort` 接口签名不变，响应字段增强  
- Order 事件 payload 继续携带 discounts，支持新子类型

#### 需求场景变更

- 🔄 修改：满件命中时，订单明细可见对应折扣来源
- ✅ 兼容：无定向/无满件配置时行为与现有一致

---

### Cart（🟢 轻微）

#### 影响程度

- ✅ 可复用：统一算价调用方式  
- 🔄 需调整：展示层可解释文案

#### 事件流变更

- 无新接口强制要求，复用 calculate-price 返回增强字段即可

#### 需求场景变更

- 🔄 修改：结算预览展示“未命中原因/件数未达门槛”提示（可选）

---

### BFF（🟢 轻微）

#### 事件流变更

- 若 User 新增分群查询 API，BFF 增加对应代理路由  
- Promotion 新字段透传无需额外编排

---

### 前端（frontend/admin）（🟡 中等）

#### 新增/修改页面与组件

- 修改 `CouponTemplatePage`：增加“适用人群”配置区  
- 修改 `PromotionActivityPage`：增加“定向规则”“满件规则”配置表单  
- 新增表单组件（建议）：
  - `TargetingRuleForm`
  - `PieceRuleForm`

#### 数据流与状态

- 表单状态包含：
  - `targeting.levelsIn/tagsAny/tagsAll/excludeTags`
  - `pieceRule.scopeType/scopeIds/minQuantity/discountType/discountValue/maxDiscountCents`
- 表单提交后可预览规则文案（如“L2+ 会员满 3 件减 30”）

#### 界面规格（粗粒度）

```
活动配置弹窗
┌───────────────────────────────────────────┐
│ 活动基础信息                              │
│ [类型] [时间] [互斥组] [优先级]            │
│                                           │
│ 定向规则                                  │
│ [等级多选] [标签任一] [标签全部] [排除标签] │
│                                           │
│ 满件规则                                  │
│ [作用范围] [件数门槛] [优惠方式] [优惠值]   │
│                                           │
│                 [取消] [保存]             │
└───────────────────────────────────────────┘
```

#### 手工验收 checklist

- [ ] 可配置并保存定向规则
- [ ] 可配置并保存满件规则
- [ ] 编辑后再次打开表单可正确回显
- [ ] 非法配置（门槛<=0、空范围等）被前后端拦截

---

### 前端（frontend/web）（🟡 中等）

#### 新增/修改页面与组件

- 修改 `HomePage`/商品卡片：展示“专享”“满件”标签  
- 修改 `ProductDetailPage`：展示条件文案与活动说明  
- 修改 `CartPage`/`CheckoutPage`：权益区展示命中与不命中原因

#### 数据流与状态

- 使用 Promotion 返回的可解释字段直接展示，不在前端自行推导  
- 提交订单失败且错误码为“价格条件变化”时触发重拉算价

#### 手工验收 checklist

- [ ] 命中定向用户可看到专享价格
- [ ] 未命中用户看到原价及原因提示
- [ ] 满件临界值（N-1/N）展示与结账结果一致
- [ ] 价格变化错误后可一键刷新并重试提交

---

### BC 间数据流

- `User(level,tags)` -> `Promotion`：算价时做人群命中判定  
- `Promotion(targeting+piece rules)` -> `Cart/Order/Web`：统一输出折扣明细与可解释信息  
- `Order(discounts)` -> `Activity`：沉淀交易优惠快照用于后续分析

---

## 四、迭代计划

### 迭代 0：User 分群能力与 Promotion 规则建模

**涉及 BC**：User、Promotion  
**前置依赖**：业务需求 2 已完成

**后端变更**：
- User 增加 `level/tags` 模型与查询 API
- Promotion 增加 `TargetingRule`、`PieceBasedRule` 模型与持久化映射

**前端变更**：
- 无必做（可后置到迭代 1）

**验收标准**：
- Promotion 能同步获取指定用户分群信息
- 活动实体可保存并读取定向/满件配置

---

### 迭代 1：统一算价扩展（定向 + 满件）

**涉及 BC**：Promotion、Order、Cart  
**前置依赖**：迭代 0

**后端变更**：
- calculate-price 支持定向过滤与满件计算
- 互斥组择优逻辑兼容满件规则
- Order/Cart 透传并消费增强后的折扣明细

**前端变更**：
- 无新增页面，先完成 API 对接与基础文案展示

**验收标准**：
- 命中定向 + 满件时，实付金额与明细正确
- 不命中时回退规则正确，不出现错误降级

---

### 迭代 2：管理端配置与消费者端可视化闭环

**涉及 BC**：frontend/admin、frontend/web、BFF  
**前置依赖**：迭代 1

**后端变更**：
- BFF 透传新增字段和新增路由（如 User 分群查询）

**前端变更**：
- admin：活动/券配置表单支持定向 + 满件  
- web：列表/详情/购物车/结账展示命中条件、不命中原因和权益标签

**验收标准**：
- 运营可独立完成定向+满件活动配置并上线
- 消费者端价格展示与下单实付一致

**E2E 验收**：

| 用例 | 场景概述 |
|------|---------|
| BIZ-PRM-UT-001 | 命中定向 + 满件活动，下单实付正确 |
| BIZ-PRM-UT-002 | 不命中定向，活动自动排除并回退到其他优惠 |
| BIZ-PRM-UT-003 | 满件边界值（N-1/N）在结账展示与下单结果一致 |

---

## 一致性检查

| 维度 | 检查项 | 结果 |
|------|--------|------|
| 场景完整 | 主流程 L3、支撑流程 L2、异常流程 L2 是否覆盖 | ✅ 覆盖 F1~F5 |
| 事件完整 | 主成功路径 + 逆向路径是否完整 | ✅ F1 已覆盖 |
| 数据可达 | L3 中所有 ❌ 数据缺口是否在变更分析补齐 | ✅ User 分群、Promotion 规则模型均已落点 |
| 场景↔变更 | 场景影响与 BC 变更是否一一对应 | ✅ Promotion/User/前端均有映射 |
| 变更内部 | 模型、事件、场景描述是否自洽 | ✅ 以统一算价为单一规则出口 |
| 前端 | 有显著前端改动时是否给出粗粒度规格与验收 checklist | ✅ admin/web 均已覆盖 |
| 扩展一致性 | 与 Theme 决策 D2/D3/D6 是否一致 | ✅ 互斥组、活动+券叠加、DiscountDetail 通用结构保持不变 |

---

## 交付跟踪

### 迭代 0：User 分群能力与 Promotion 规则建模

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Phase B 文档落地（Promotion/User/Order/前端/系统文档） | analyze-requirement (Phase B) | — | ✅ 完成 |
| 2 | Order 事件 payload v2（couponId + pricingSnapshot） | evolve-feature | #1 | ✅ 完成 |
| 3 | User 分群查询接口与模型落地 | evolve-feature | #1 | ✅ 完成 |
| 4 | Promotion 定向/满件模型持久化 | evolve-feature | #1, #3 | ✅ 完成（规则计算待迭代1） |
| 5 | Promotion 算价扩展（定向过滤 + 满件折扣） | evolve-feature | #3, #4 | ✅ 完成 |
| 6 | 迭代验收（后端测试 + E2E） | deliver-requirement | #2-#5 | ✅ 完成 |

**启动日期**：2026-03-15  
**下一步**：进入下一业务需求，用户定向与满件折扣需求闭环完成（含 payload 优化与前后端展示）

