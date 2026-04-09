# 促销活动与价格引擎（Promotion Activity & Pricing Engine）

## 一、需求概述与场景

### 业务背景与目标

优惠券能力（业务需求 1）已经建立了 Promotion BC 的基础能力，但当前系统仍缺少**商品维度的促销活动**，无法支持常见的“单品直降”“满减活动”“活动互斥”等运营策略。  
本需求作为 [促销体系](../theme.md) 的业务需求 2，目标是把 Promotion BC 从“券引擎”演进为“统一价格引擎”：

- 运营可配置促销活动（单品活动、订单满减）
- 同组活动互斥，不同组可叠加
- 与优惠券默认可叠加（遵循 Theme 决策 D3）
- Order/Cart/Web 统一调用 Promotion 计算最终价格与优惠明细
- 前端展示“活动价 / 预计到手价”，提升转化

### 需求类型

**扩展已有能力**（基于业务需求 1）。  
核心变化是：价格计算从“仅券抵扣”扩展为“活动 + 券统一计算”，并把活动规则纳入 Promotion BC。

### 与已有能力的核心区别

| 维度 | 业务需求 1（优惠券） | 本需求（促销活动与价格引擎） |
|------|----------------------|------------------------------|
| 优惠来源 | 用户维度权益（Coupon） | 商品维度 + 订单维度活动（Activity） |
| 计算输入 | items + couponId | items + userId + couponId + 活动规则集 |
| 计算输出 | COUPON 折扣明细 | COUPON + ACTIVITY 折扣明细 |
| 前端展示 | 结账页展示券抵扣 | 列表/详情/购物车/结账统一展示活动价与到手价 |
| 运营配置 | 券模板管理 | 活动管理 + 互斥组管理 + 生效窗口 |

### 影响面

| 影响范围 | 影响程度 | 说明 |
|---------|---------|------|
| **Promotion BC** | 🔴 重大 | 新增 Activity/MutexGroup 模型；价格引擎扩展为活动+券统一编排 |
| **Order** | 🟡 中等 | 复用 calculatePrice 接口，`DiscountDetail` 新增 `ACTIVITY` 类型 |
| **Cart** | 🟡 中等 | 结算预览接入 Promotion 实时算价，展示活动优惠 |
| **BFF** | 🟢 轻微 | 新增活动管理与活动价查询的代理路由 |
| **Activity** | 🟢 轻微 | 若需促销运营分析，补充消费活动相关事件（可后置） |
| **前端（web）** | 🔴 重大 | 商品列表/详情/购物车/结账页新增活动价与预计到手价展示 |
| **前端（admin）** | 🟡 中等 | 新增促销活动管理页面（活动 CRUD、上下线、互斥组） |

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | 活动价下单（含券叠加） | 主流程 | L3 重分析 | PlaceOrder 统一算价，按互斥组选活动并与券叠加，支付后闭环 |
| F2 | 活动配置与互斥组管理 | 支撑流程 | L2 中分析 | 运营创建活动、配置生效时间与互斥组，控制叠加规则 |
| F3 | 商品列表/详情活动价展示 | 支撑流程 | L2 中分析 | 用户选品阶段看到“活动价/预计到手价” |
| F4 | 购物车与结账页实时优惠预览 | 支撑流程 | L2 中分析 | 购物车和结账统一展示活动优惠，结账可继续选券 |
| F5 | 活动失效与并发冲突处理 | 异常流程 | L2 中分析 | 活动到期、并发上下线、价格不一致时以后端重算为准 |

**business-flows.md 检查清单回答**：

1. **影响哪段？** 主要影响 N2O（选品、结账、下单算价）；O2F 不变。  
2. **影响选品与决策阶段吗？** 是。商品列表/详情/购物车/结账都新增价格展示与查询。  
3. **影响后台管理流程吗？** 是。新增“活动管理 + 互斥组管理”。  
4. **影响哪些事件？** Order 现有事件无需改名，但价格相关 payload 将包含 ACTIVITY 折扣明细。Promotion 可新增活动生命周期事件（可选）。  
5. **新增路径还是影响现有路径？** 影响现有 N2O 路径，不新增独立业务路径。  
6. **是否打破 N2O ⊥ O2F？** 否。活动规则在 N2O 算价完成后即固化为订单快照。  
7. **测试覆盖是否仍完整？** 需新增 Business E2E 覆盖“活动价展示 + 活动+券叠加下单”。  

---

## 二、场景分析（事件流）

### F1：活动价下单（含券叠加）（L3）

以 `business-flows.md` 的 N2O 主链路为基线，变化聚焦在 PlaceOrder 前后的算价与门禁。

#### 主成功路径

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|----|---------|
| 1 | — | ⌘ PlaceOrder(items, address, couponId?) | 前端提交时带 skuId、quantity、unitPrice 快照与可选 couponId | Order | ✅ 入口不变 |
| 2 | — | → 同步 CalculatePrice(items, userId, couponId?) | Promotion 计算顺序：基准价 → 活动选择（按互斥组）→ 券抵扣；返回行级 discounts | Promotion | 🔄 价格引擎扩展 |
| 3 | 🟧 StockReserved | → 同步 OccupyStock(orderId, physicalItems) | SERVICE 行跳过库存占用 | Inventory | ✅ 既有 |
| 4 | — | → 同步 LockCoupon(couponId, orderId) | 有券时锁券，无券跳过 | Promotion | ✅ 既有 |
| 5 | 🟧 OrderCreated | → CreatePayment(payableAmountCents) → 保存/发布 | `OrderLineItem.discounts` 含 COUPON/ACTIVITY；`totalAmountCents` 为实付 | Order | 🔄 折扣类型扩展 |
| 6 | 🟧 PaymentCompleted | 网关回调 | Order 置 PAID | Payment | ✅ 既有 |
| 7 | — | → 同步 RedeemCoupon(couponId) | 有券时核销 | Promotion | ✅ 既有 |
| 8 | — | → 同步 CreateFulfillment | 履约流程不感知活动，仅使用订单实付与快照 | Fulfillment | ✅ 不变 |
| 9 | 🟧 ... → OrderCompleted | 后续履约流程 | — | Order | ✅ 不变 |

> **决策 ACT1**：活动计算只在 Promotion BC 内完成，Order/Cart/Web 不复制业务规则，只传“订单上下文”并接收计算结果。

> **决策 ACT2**：互斥规则采用 Theme 决策 D2（互斥组）。同组最多命中一个最优活动，不同组可叠加，最终再叠加 coupon。

> **决策 ACT3**：活动折扣也落入 `List<DiscountDetail>`，`type=ACTIVITY`，保持 Theme 决策 D6 的“结构稳定、类型扩展”策略。

#### 补偿与异常路径

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 与现有差异 |
|---|----------|-----------|-----------------|----|------------|
| 1 | 🟧 PaymentExpired | 超时检测 | 触发 CancelOrder | Payment | ✅ 既有 |
| 2 | — | ⌘ CancelOrder | 释放库存 + 释放券 + 退款（如已支付） | Order | ✅ 券补偿既有 |
| 3 | — | CalculatePrice 再校验失败 | 活动过期/停用导致不可用时，返回业务错误，前端需刷新价格 | Promotion | 🆕 新增失败原因 |
| 4 | — | LockCoupon 失败 | 释放已占库存并返回“价格或权益已变化” | Order | ✅ 既有补偿，错误语义扩展 |

#### 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 2 | 活动筛选 | skuId/spuId、数量、基准价、当前时间、活动状态、生效窗口、互斥组 | Order 入参 + Promotion | ❌ 需新增 PromotionActivity/MutexGroup |
| 2 | 最优活动选择 | 同组候选活动的可减金额 | Promotion 价格引擎 | ❌ 需新增组内择优策略 |
| 2 | 叠加与封顶 | 活动折扣总额、券门槛校验基数 | Promotion | 🔄 需扩展 calculatePrice 输出 |
| 5 | 行级折扣快照 | DiscountDetail(type=ACTIVITY/COUPON) | Promotion 返回 | 🔄 Order 事件/DTO 需支持多类型 |
| 3/4 | 并发一致性 | 活动版本或更新时间戳 | Promotion | ❌ 需增加版本校验字段（或乐观锁语义） |

---

### F2：活动配置与互斥组管理（L2）

1. 运营在 admin 创建活动：名称、类型（单品直降/订单满减）、生效时间、优先级、互斥组。  
2. 若为单品活动，配置命中范围（skuIds/spuIds）；若为订单满减，配置门槛与减免。  
3. 活动上/下线：`DRAFT -> ACTIVE -> INACTIVE`，仅 ACTIVE 且在时间窗口内可参与计算。  
4. 互斥组可维护“组编码 + 说明”，活动关联组编码；同组活动在算价时只取最优。  

**关键数据流**：
- admin 页面调用 Promotion API 完成 CRUD 和状态切换。
- 活动配置变更实时生效到 calculatePrice（无需部署）。

---

### F3：商品列表/详情活动价展示（L2，前端为主）

**页面数据流**：

1. 列表/详情查询商品时，附带调用 Promotion 的“活动价预估接口”。  
2. Promotion 根据当前时间与 SKU 计算 `activityPrice`、`activityLabel`、`estimatedDiscount`。  
3. 前端展示“原价划线 + 活动价 + 活动标签”；无活动时回退原价。  

**关键边界**：
- 商品详情页属于“决策前展示”，不锁定任何权益。  
- 最终价格以后端 PlaceOrder 时重算为准，前端展示仅做引导。  

---

### F4：购物车与结账页实时优惠预览（L2，前端为主）

1. 购物车结算预览阶段，调用 Promotion 统一算价（无 coupon 或默认 couponId=null）。  
2. 结账页继续允许选券；选券后再次调用 calculatePrice，返回“活动优惠 + 券优惠 + 实付”。  
3. 提交订单时将 `couponId` 与 items 一并传给 Order；Order 再次调用 Promotion 保证一致性。  

**流程耦合**：
- F3/F4 只做展示与预估，F1 才是价格落单的权威路径。  
- 购物车与立即购买都复用同一价格引擎，避免规则分叉。  

---

### F5：活动失效与并发冲突处理（L2）

1. 用户停留在结账页期间活动到期：提交订单时重算失败，提示“价格已变化，请刷新”。  
2. 运营下线活动与用户下单并发：以 PlaceOrder 时 Promotion 实时快照为准。  
3. 活动配置更新后，未下单页面可能展示旧价；提交时后端重算兜底，前端接错误后刷新。  

---

### 查询影响

| 查询场景 | 变化 |
|---------|------|
| 商品列表/详情查询 | 返回或补充活动价、活动标签、预计到手价 |
| 购物车结算预览 | 从“基准价汇总”升级为“活动优惠后汇总” |
| 结账页价格摘要 | 展示活动优惠、券优惠、实付金额三段 |
| 订单详情 | 展示 ACTIVITY 折扣明细，与券明细并列 |

### 流程间耦合

- **F2 → F3/F4/F1**：活动配置是算价输入，实时引用，不做长期缓存快照。  
- **F3/F4 → F1**：展示价与预览价只做引导，订单提交必须后端重算。  
- **F5**：异常由“提交时重算”统一兜底，不新增跨 BC 补偿链。  

---

## 三、变更分析

### Promotion（🔴 重大，🔄 需调整：从券引擎升级为统一价格引擎）

#### 领域模型变更

- 新增 `PromotionActivity` 聚合根  
  - 核心属性：`id`, `name`, `activityType`, `status`, `startAt`, `endAt`, `priority`, `mutexGroupCode?`, `targetScope`, `rule`, `version`, `createdAt`, `updatedAt`
- 新增 `MutexGroup`（可作为聚合根或配置实体）  
  - 核心属性：`groupCode`, `name`, `description`, `status`
- 扩展 `PricingEngine`  
  - 输入：`items`, `userId`, `couponId?`, `now`
  - 输出：`lineDiscounts[]`, `totalActivityDiscountCents`, `totalCouponDiscountCents`, `totalDiscountCents`, `payableAmountCents`
- 扩展 `DiscountDetail.type` 枚举：`COUPON | ACTIVITY`

**不变式**：
- 仅 `ACTIVE` 且 `startAt <= now < endAt` 的活动可参与计算  
- 同一互斥组同一订单最多命中 1 个活动（取优惠金额最大）  
- `payableAmountCents >= 0`  

#### 事件流变更

- 新增管理 API（admin）  
  - `POST /api/promotion/activities`
  - `GET /api/promotion/activities`
  - `PUT /api/promotion/activities/{id}`
  - `POST /api/promotion/activities/{id}/activate`
  - `POST /api/promotion/activities/{id}/deactivate`
  - `GET /api/promotion/mutex-groups`
- 扩展价格 API  
  - `POST /api/promotion/calculate-price`：响应新增活动优惠维度  
  - `POST /api/promotion/preview-sku-price`（可选）：列表/详情页活动价预估

#### 需求场景变更

- 🔲 新增：创建单品直降活动并生效  
- 🔲 新增：创建订单满减活动并生效  
- 🔲 新增：同组多活动命中时取最优  
- 🔲 新增：不同组活动可叠加  
- 🔄 修改：calculatePrice 支持活动 + 券联合计算  
- 🔲 新增：活动过期后自动失效，不再参与算价  

---

### Order（🟡 中等，✅ 可复用主流程，🔄 扩展折扣语义）

#### 领域模型变更

- `OrderLineItem.discounts: List<DiscountDetail>` 结构复用，不新增字段  
- `DiscountDetail.type` 使用扩展枚举，新增 `ACTIVITY` 值  
- `Order.totalAmountCents` 继续表示实付金额（含活动+券后）

#### 事件流变更

- `PromotionPricePort.calculatePrice(...)` 接口签名保持不变（响应增强）  
- PlaceOrder、PaymentCompleted、CancelOrder 的调用顺序保持既有  
- `OrderCreated/Cancelled/Completed` payload 中的折扣明细支持 ACTIVITY

#### 需求场景变更

- 🔄 修改：下单含活动优惠时应写入 ACTIVITY 明细  
- 🔄 修改：活动+券叠加下单时支付金额正确  
- ✅ 兼容：无活动场景行为不变  

---

### Cart（🟡 中等，🔄 需接入统一算价）

#### 领域/应用变更

- 结算预览从“基准价汇总”升级为“调用 Promotion 统一算价”  
- 返回结构增加活动优惠与预计到手价字段（或前端侧二次调用 Promotion，二选一，推荐前者以统一口径）

#### 需求场景变更

- 🔄 修改：购物车结算预览展示活动优惠金额  
- 🔲 新增：有活动命中时分组小计与总价正确  

---

### BFF（🟢 轻微）

#### 事件流变更

- 新增路由代理：
  - `/api/promotion/activities`
  - `/api/promotion/mutex-groups`
  - `/api/promotion/preview-sku-price`（若采用）

---

### Activity（🟢 轻微，可后置）

#### 事件流变更（可选）

- 可新增事件：`promotion.activity.created`, `promotion.activity.activated`, `promotion.activity.deactivated`  
- 若本迭代不做运营分析，可先不落地，依赖订单事件中的 discounts 即可满足交易分析。

---

### 前端（frontend/web）（🔴 重大）

#### 新增/修改页面与组件

- 修改 `ProductListPage`：展示活动价标签与划线价  
- 修改 `ProductDetailPage`：展示活动价、预计到手价  
- 修改 `CartPage`：展示活动优惠汇总  
- 修改 `CheckoutPage`：价格摘要拆分“活动优惠 / 券优惠 / 实付”

#### 数据流与状态

- 页面展示场景调用活动价预估接口（或聚合 API）  
- 结账页选券后复用 calculatePrice，统一展示活动+券叠加结果  
- 订单提交后若后端返回价格变化错误，触发价格刷新并提示

#### 手工验收 checklist

- [ ] 商品列表可见活动标签与活动价  
- [ ] 商品详情活动价与预计到手价正确  
- [ ] 购物车结算预览显示活动优惠  
- [ ] 结账页可同时展示活动优惠和券优惠  
- [ ] 活动过期后提交订单提示价格变化并可刷新重试  

---

### 前端（frontend/admin）（🟡 中等）

#### 新增页面

- 新增 `PromotionActivityPage`（建议路由 `/promotion-activities`）  
  - 列表：活动类型、时间窗口、互斥组、状态  
  - 表单：创建/编辑活动  
  - 操作：上/下线

#### 手工验收 checklist

- [ ] 可创建单品活动与订单满减活动  
- [ ] 可配置互斥组并生效  
- [ ] 活动上下线后前台价格展示可感知变化  

---

### BC 间数据流

- `admin -> Promotion`: 活动/互斥组配置  
- `web list/detail/cart/checkout -> Promotion`: 活动价预估/统一算价  
- `Order -> Promotion`: PlaceOrder 统一算价（活动+券）  
- `Promotion -> Order`: 行级 `DiscountDetail[]`（含 `ACTIVITY`/`COUPON`）  
- `Order -> Payment`: `payableAmountCents`（实付）  
- `Order events -> Activity`: 折扣快照用于运营分析

---

## 四、迭代计划

### 迭代 0：活动模型与管理端配置 ✅ 已完成

**涉及 BC**：Promotion、BFF、frontend/admin  
**前置依赖**：业务需求 1（优惠券）完成

**后端**：
- 新增 `PromotionActivity`、`MutexGroup` 模型与仓储
- 活动管理 API（CRUD + 上下线）
- BFF 代理活动管理路由

**前端**：
- admin 新增活动管理页（列表、创建/编辑、上下线、互斥组选择）

**验收标准**：
- 运营可维护活动并控制生效状态
- 活动数据可通过 API 查询

---

### 迭代 1：价格引擎扩展（活动 + 券） ✅ 已完成

**涉及 BC**：Promotion、Order  
**前置依赖**：迭代 0

**后端**：
- `calculatePrice` 增强为活动+券统一计算
- 互斥组择优与跨组叠加策略落地
- `DiscountDetail.type=ACTIVITY` 全链路落地到 Order 与事件 payload

**前端**：
- 无新增页面，先不改 UI（后续迭代统一接）

**验收标准**：
- 活动命中、互斥、叠加规则符合预期
- 活动+券叠加金额正确，订单实付正确

---

### 迭代 2：前台价格展示与结算闭环 ✅ 已完成

**涉及 BC**：frontend/web、Cart、BFF、Promotion  
**前置依赖**：迭代 1

**后端**：
- 提供活动价预估接口（或统一聚合查询）
- Cart 结算预览接入统一算价

**前端**：
- 商品列表/详情展示活动价
- 购物车/结账展示活动优惠与实付
- 价格变化错误提示与刷新机制

**验收标准**：
- 选品阶段可见活动价
- 结账页活动+券金额展示一致
- 提交订单与后端重算一致

**E2E 验收**：

| 用例 | 场景概述 |
|------|---------|
| BIZ-PRM-001 | 单品活动命中：列表展示活动价 → 下单实付正确 |
| BIZ-PRM-002 | 同组活动互斥：仅最优活动生效 |
| BIZ-PRM-003 | 活动 + 券叠加：结账展示与下单结果一致 |

---

## 一致性检查

| 维度 | 检查项 | 结果 |
|------|--------|------|
| 场景完整 | 主流程 L3，支撑与异常覆盖选品/结账/后台配置 | ✅ |
| 事件完整 | 以 N2O 基线扩展，补偿链不新增复杂分支 | ✅ |
| 数据可达 | 活动筛选、互斥择优、折扣快照的数据缺口已映射到模型变更 | ✅ |
| 场景↔变更 | F1-F5 在 Promotion/Order/Cart/前端均有对应落点 | ✅ |
| 前端完整 | admin + web 均有页面改动与验收清单 | ✅ |
| 扩展一致性 | 与 Theme 决策 D2/D3/D6 保持一致，无模型回退 | ✅ |

---

## 交付跟踪

### 迭代 0：活动模型与管理端配置 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Promotion：活动模型（PromotionActivity/状态机）与持久化 | evolve-feature | — | ✅ 完成 |
| 2 | Promotion：活动管理 API（创建、列表、上下线） | evolve-feature | #1 | ✅ 完成 |
| 3 | Admin：促销活动管理页 + API 对接 | frontend-development | #2 | ✅ 完成 |

**交付日期**：2026-03-15  
**下一迭代**：迭代 1（价格引擎扩展）

---

### 迭代 1：价格引擎扩展（活动 + 券） ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Promotion：活动互斥组择优 + 跨组叠加 + 券后置抵扣 | evolve-feature | 迭代 0 | ✅ 完成 |
| 2 | Promotion：算价响应补充活动/券分项优惠明细 | evolve-feature | #1 | ✅ 完成 |
| 3 | Order：下单统一调用算价（含无券场景）并落单折扣明细 | integration | #1, #2 | ✅ 完成 |

**交付日期**：2026-03-15  
**下一迭代**：迭代 2（前台展示与结算闭环）

---

### 迭代 2：前台价格展示与结算闭环 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Web：商品列表/详情接入活动价预估展示 | frontend-development | 迭代 1 | ✅ 完成 |
| 2 | Web：购物车/结账展示活动优惠与实付金额 | frontend-development | #1 | ✅ 完成 |
| 3 | 回归验证：后端测试 + Web Smoke P0 + Business E2E | deliver-requirement | #1, #2 | ✅ 完成 |

**交付日期**：2026-03-15  
**下一迭代**：本需求已全部交付
