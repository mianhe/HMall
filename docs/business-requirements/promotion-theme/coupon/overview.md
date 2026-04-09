# 优惠券（Coupon）

## 一、需求概述与场景

### 业务背景与目标

HMall 当前的定价模型是「Catalog 定义 SKU 基准价 → Order/Cart 直接使用」，没有任何优惠机制。促销体系（[Promotion Theme](../theme.md)）的第一个业务需求是引入**优惠券能力**：运营可以创建券模板并发券给用户，用户可以主动领券；结算时选择可用优惠券，系统计算优惠金额；支付成功后核销券，超时/取消时释放券。

本业务需求同时建立 **Promotion BC 骨架**和**简易价格计算引擎**，为后续业务需求（促销活动、用户定向）奠定基础。

### 需求类型

**全新能力**。新建 Promotion BC，同时改动 Order、Cart、Payment（轻度）及前端。

### 影响面

| 影响范围 | 影响程度 | 说明 |
|---------|---------|------|
| **Promotion BC（新建）** | 🔴 重大 | 全新 BC：券模板、券实例、价格计算引擎 |
| **Order** | 🟡 中等 | PlaceOrder 接入价格计算与券锁定/核销/释放；OrderLineItem 新增通用优惠明细 |
| **Cart** | 🟢 轻微 | 结算预览暂不调用价格引擎（券选择在结账页完成，不在购物车）；Phase 2 再接入 |
| **Payment** | 🟢 轻微 | 无模型变更，支付金额改为优惠后实付金额（已由 Order 计算后传入） |
| **Catalog** | ⚪ 无变更 | SKU 基准价不变 |
| **User** | ⚪ 无变更 | — |
| **前端 (web)** | 🟡 中等 | 结账页增加选券交互；新增领券中心页面；我的优惠券页面 |
| **前端 (admin)** | 🟡 中等 | 新增券模板管理页面 |

### 券类型

本业务需求支持两种优惠券类型：

| 券类型 | 使用门槛 | 优惠方式 | 示例 |
|--------|---------|---------|------|
| **满减券** | 订单金额 ≥ 门槛值 | 减固定金额 | 满 100 元减 20 元 |
| **折扣券** | 订单金额 ≥ 门槛值 | 打折（可设封顶） | 满 200 元打 8 折，最多减 50 元 |

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | 用券下单 | 主流程 | L3 重分析 | 用户选券 → 下单（算价+锁券）→ 支付 → 核销；超时/取消 → 释放 |
| F2 | 券模板管理 | 支撑流程 | L1 轻分析 | 运营在后台创建/编辑/停用券模板 |
| F3 | 发券与领券 | 支撑流程 | L2 中分析 | 系统发券给用户 / 用户从领券中心主动领取 |
| F4 | 结账页选券与优惠预览 | 支撑流程 | L2 中分析 | 结账页展示可用券列表，选券后实时计算并展示优惠金额 |
| F5 | 我的优惠券 | 支撑流程 | L1 轻分析 | 用户查看已有券（可用/已用/已过期） |
| F6 | 券过期自动失效 | 异常流程 | L1 轻分析 | 到期未使用的券自动标记为过期 |

**business-flows.md 检查清单回答**：

1. **影响哪段？** N2O（券在下单/支付阶段生效）。O2F 不受影响。
2. **影响选品与决策阶段吗？** 是——结账页新增选券交互和优惠展示（F4）。商品详情页和购物车页暂不受影响（"券后价"展示留给业务需求 2）。
3. **影响后台管理流程吗？** 是——新增券模板管理（F2）。
4. **影响哪些事件？** OrderCreated/Cancelled/Completed 的 payload 增加优惠明细；Promotion BC 内部新增券生命周期事件（CouponIssued、CouponRedeemed 等）供 Activity 消费。
5. **新增路径还是影响现有路径？** 影响现有 N2O 路径（增加券参与），不新增独立路径。
6. **是否打破 N2O ⊥ O2F？** 否。券在 N2O 阶段闭环（锁定→核销/释放），O2F 不感知券。
7. **测试覆盖是否仍完整？** 需新增 Business E2E 覆盖用券下单场景。

---

## 二、场景分析（事件流）

### F1：用券下单（L3）

以 `business-flows.md` 现有 N2O 事件流为基线，标注优惠券引入的变化点。

#### 主成功路径

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|-----|---------|
| 1 | — | ⌘ PlaceOrder(items, address, **couponId?**) | — | Order | 🔄 新增可选参数 couponId |
| 2 | — | → 同步 **CalculatePrice**(items, userId, couponId) | ⟳ 有 couponId 时：校验券属于该用户、状态 AVAILABLE、未过期、订单基准总额 ≥ 门槛；计算折扣金额并按行金额比例分摊 | Promotion | 🆕 新增同步调用 |
| 3 | 🟧 StockReserved | → 同步 OccupyStock(orderId, physicalItems) | ⟳ 仅 PHYSICAL items 占库存 | Inventory | ✅ 已有 |
| 4 | — | → 同步 **LockCoupon**(couponId, orderId) | ⟳ 有 couponId 时：券状态 AVAILABLE → LOCKED，记录 orderId | Promotion | 🆕 新增同步调用 |
| 5 | 🟧 OrderCreated | → CreatePayment(**payableAmountCents**) → 保存 → 发布 | ⟳ 订单保存时 OrderLineItem 含 discounts；支付金额为优惠后实付 | Order | 🔄 payload 增加 discounts |
| 6 | 🟧 PaymentCompleted | 网关回调 | ⟳ Order 置 PAID | Payment | ✅ 已有 |
| 7 | — | → 同步 **RedeemCoupon**(couponId) | ⟳ 有 couponId 时：LOCKED → USED，记录 usedAt | Promotion | 🆕 新增步骤 |
| 8 | — | → 同步 CreateFulfillment | — | Fulfillment | ✅ 已有 |
| 9 | 🟧 ... → OrderCompleted | 后续履约流程 | — | Order | ✅ 不变 |

> **决策 CPN1**：PlaceOrder 内的执行顺序为 CalculatePrice → OccupyStock → LockCoupon → CreatePayment。若 LockCoupon 失败（券已被他人使用），需补偿释放已占用的库存后返回错误。此顺序与现有"先占库存再创建支付"一致，券锁定插在两者之间。

> **决策 CPN2**：Order 传入 items（含 base prices）给 Promotion.calculatePrice，Promotion 不直接查 Catalog。Phase 1 只有券抵扣，Promotion 只需知道订单总额即可校验门槛和计算折扣。Phase 2 引入活动价时，Promotion 可能需要 skuId/spuId 来匹配活动规则，届时扩展入参即可。

> **决策 CPN3**：couponId 作为 Order 聚合的一级字段存储（而非仅存在于 discounts 列表中），因为取消/超时补偿需要快速定位要释放的券。

#### 补偿路径

支付超时或用户取消时，需同步释放券。

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 与现有差异 |
|---|----------|-----------|-----------------|-----|----------|
| 1 | 🟧 PaymentExpired | 超时检测 | ⟳ Order: CancelOrder | Payment | ✅ 已有 |
| 2 | — | ⌘ CancelOrder | ⟳ 同步 ReleaseStock + **ReleaseCoupon** + Refund（如已支付） + CancelFulfillment（如已创建） | Order | 🔄 新增 ReleaseCoupon |
| 3 | 🟧 StockReleased | → 同步 ReleaseStock | — | Inventory | ✅ 已有 |
| 4 | — | → 同步 **ReleaseCoupon**(couponId) | ⟳ 有 couponId 时：LOCKED → AVAILABLE | Promotion | 🆕 新增 |
| 5 | 🟧 OrderCancelled | 发布事件 | — | Order | ✅ 已有 |

用户主动取消（PENDING_PAYMENT / PAID / FULFILLING）走同样的补偿逻辑。已核销的券（USED 状态）取消时回退为 AVAILABLE。

#### 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 2 | 校验券可用性 | 券的 userId、status、expiresAt、门槛、类型、折扣参数 | Promotion BC 内部查询 | ❌ 需新建 CouponTemplate + Coupon |
| 2 | 计算折扣并分摊 | 各行 unitPriceCents × quantity | PlaceOrder 入参 | ✅ 已有 |
| 4 | 券状态流转 | Coupon.status | Promotion BC | ❌ 需新建 |
| 5 | OrderLineItem 记录折扣 | discounts: List\<DiscountDetail\> | 步骤 2 计算结果 | ❌ OrderLineItem 需新增 discounts |
| 5 | 支付金额为实付 | payableAmountCents | Order 内部计算：总额 − 总折扣 | 🔄 Order.totalAmountCents 语义变为实付金额 |
| 7 | 核销/释放券 | Order 持有的 couponId | Order 聚合 | ❌ Order 需新增 couponId 字段 |

---

### F2：券模板管理（L1）

运营在 admin 后台管理券模板，为发券提供配置基础。

**操作步骤**：
1. 运营进入券模板管理页面，查看现有模板列表
2. 创建新模板：填写名称、类型（满减/折扣）、门槛金额、优惠参数（减免金额或折扣率+封顶）、有效天数、发行总量、每人限领数
3. 可编辑模板信息（发行总量只能调大不能调小）
4. 可停用模板（ACTIVE → INACTIVE），停用后不可再发放/领取新券，已发放的券不受影响

**对主流程的影响边界**：券模板是 F3（发券/领券）的前置条件，不直接影响 F1（用券下单）。

---

### F3：发券与领券（L2）

券实例从模板创建，有两种发放方式。

#### 系统发券（运营触发）

1. 运营在 admin 后台选择一个 ACTIVE 模板 + 指定一批 userId
2. Promotion BC 为每个 userId 创建一张券实例：状态 AVAILABLE，expiresAt = now + template.validDays
3. 规则校验：
   - `template.issuedQuantity < template.totalQuantity`（否则返回"库存不足"）
   - 该用户对此模板的已领取数 < `template.perUserLimit`（否则跳过该用户）
   - `template.status = ACTIVE`

#### 用户领券（用户触发）

1. 用户在领券中心页面看到可领取的券模板列表（ACTIVE + 未领满 + 用户未达限额）
2. 用户点击「领取」→ 调用 Promotion API 创建券实例
3. 校验规则同上；并发领券场景需保证 issuedQuantity 的原子递增

**关键状态变化**：
- CouponTemplate.issuedQuantity += 1
- 新建 Coupon：status=AVAILABLE，userId，templateId，expiresAt

**数据依赖**：
- 可领券列表：需查询模板（ACTIVE + totalQuantity > issuedQuantity）+ 用户已领数 → 均在 Promotion BC 内

---

### F4：结账页选券与优惠预览（L2，前端为主）

用户在结账页选择优惠券并预览优惠效果。

**页面数据流**：

```
CheckoutPage 加载
  → 计算当前订单基准总额（items × unitPriceCents）
  → GET /api/coupons/available?userId=xxx&orderAmountCents=yyy
  → 展示可用券列表（名称、类型、优惠描述、门槛）

用户选中一张券
  → POST /api/promotion/calculate-price { items, userId, couponId }
  → 展示：原价 | −优惠金额 | 实付金额

用户取消选券
  → 恢复原价展示

用户提交订单
  → POST /api/orders { items, address, couponId }
```

**关键组件边界**：
- CheckoutPage 新增「优惠券」选择区域（在商品摘要与地址之间）
- 券列表组件：展示可用券，选中态高亮，最多选一张
- 价格摘要组件：展示原价、优惠金额（红色）、实付金额

**与后端 API 的对接点**：
- `GET /api/coupons/available`：Promotion BC 新增 API，返回当前用户对指定金额可用的券
- `POST /api/promotion/calculate-price`：Promotion BC 新增 API，返回折扣金额和实付

---

### F5：我的优惠券（L1）

用户在"我的"页面查看所有优惠券。

**操作步骤**：
1. 用户进入"我的优惠券"页面
2. 分 tab 展示：可用（AVAILABLE）| 已使用（USED）| 已过期（EXPIRED）
3. 每张券展示：券名称、类型标签（满减/折扣）、面额或折扣率、使用门槛、有效期

**对主流程的影响边界**：纯查询，不影响其他流程。

---

### F6：券过期自动失效（L1）

**机制**：Promotion BC 定时扫描 status=AVAILABLE 且 expiresAt < now 的券，批量更新为 EXPIRED。类似 Payment BC 的支付超时检测。

**对主流程的影响边界**：过期券在 F4（可用券列表）和 F1（价格计算校验）中自然被排除，无需额外处理。

---

### 查询影响

| 查询场景 | 变化 |
|---------|------|
| Order 详情查询 | 返回需包含 OrderLineItem.discounts 和 Order.couponId |
| Order 列表查询 | 可展示"已优惠 ¥XX"标签（从 totalAmountCents 与 sum(line.totalPriceCents) 的差额推算，或直接返回 totalDiscountCents） |
| OrderCreated/Cancelled/Completed 事件 | payload 增加 discounts 字段（供 Activity 消费和分析） |

### 流程间耦合

- **F2 → F3**：券模板是发券的前置数据，F3 实时引用模板的 totalQuantity/issuedQuantity/perUserLimit/status。模板停用不影响已发放的券。
- **F3 → F1**：用户已领取的券是下单的可选输入。券一旦发放，其属性（门槛、折扣）快照自模板，后续模板修改不影响已发放券。

> **决策 CPN4**：券的门槛和折扣参数在发放时从模板快照到券实例，而非实时引用模板。这样模板后续修改不会意外改变用户手中券的行为。

- **F4 → F1**：前端选券结果（couponId）传入 PlaceOrder。前端预览的折扣金额仅供展示，后端 PlaceOrder 会重新计算（以后端为准）。
- **F6**：不触发跨流程补偿。LOCKED 状态的券不会被过期扫描影响（只扫描 AVAILABLE）。

---

## 三、变更分析

### Promotion BC（🔴 重大，🔲 全新）

#### 领域模型

**CouponTemplate — 聚合根**

| 属性 | 类型 | 说明 |
|------|------|------|
| templateId | Long | 唯一标识 |
| name | String | 券名称，如"新人专享满100减20" |
| type | CouponType | AMOUNT_OFF（满减）/ PERCENTAGE_OFF（折扣） |
| thresholdCents | Long | 使用门槛（分），订单基准总额 ≥ 此值才可用 |
| discountCents | Long? | 满减金额（分），仅 AMOUNT_OFF |
| discountRate | Integer? | 折扣值（如 80 表示打 8 折），仅 PERCENTAGE_OFF |
| maxDiscountCents | Long? | 折扣封顶金额（分），仅 PERCENTAGE_OFF，可选 |
| validDays | Integer | 领取后有效天数 |
| totalQuantity | Integer | 发行总量 |
| issuedQuantity | Integer | 已发放数量，初始 0 |
| perUserLimit | Integer | 每人限领数 |
| status | TemplateStatus | ACTIVE / INACTIVE |
| createdAt | Instant | — |
| updatedAt | Instant | — |

**不变式**：
- thresholdCents ≥ 0
- AMOUNT_OFF：discountCents > 0 且 discountCents ≤ thresholdCents
- PERCENTAGE_OFF：1 ≤ discountRate ≤ 99；maxDiscountCents > 0（若设）
- totalQuantity > 0，perUserLimit > 0，perUserLimit ≤ totalQuantity
- issuedQuantity ≤ totalQuantity
- 仅 ACTIVE 可发券/领券

**Coupon — 聚合根**

| 属性 | 类型 | 说明 |
|------|------|------|
| couponId | Long | 唯一标识 |
| templateId | Long | 来源模板 |
| userId | Long | 持有用户 |
| name | String | 快照自模板 |
| type | CouponType | 快照自模板 |
| thresholdCents | Long | 快照自模板 |
| discountCents | Long? | 快照自模板 |
| discountRate | Integer? | 快照自模板 |
| maxDiscountCents | Long? | 快照自模板 |
| status | CouponStatus | AVAILABLE / LOCKED / USED / EXPIRED |
| orderId | Long? | LOCKED/USED 时关联的订单 |
| issuedAt | Instant | 发放时间 |
| expiresAt | Instant | 过期时间 = issuedAt + validDays |
| usedAt | Instant? | 核销时间 |

**不变式**：
- userId、templateId 必填
- LOCKED/USED 时 orderId 必填
- USED 时 usedAt 必填

**状态机**：

```
AVAILABLE → LOCKED    (lockCoupon: 下单锁定)
LOCKED    → USED      (redeemCoupon: 支付完成核销)
LOCKED    → AVAILABLE (releaseCoupon: 超时/取消释放)
USED      → AVAILABLE (releaseCoupon: 已支付订单取消退券，若未过期)
AVAILABLE → EXPIRED   (定时扫描: expiresAt < now)
```

**领域行为**：
- `lock(orderId)`：仅 AVAILABLE → LOCKED，记录 orderId
- `redeem()`：仅 LOCKED → USED，记录 usedAt
- `release()`：LOCKED → AVAILABLE 或 USED → AVAILABLE（清除 orderId、usedAt）
- `expire()`：仅 AVAILABLE → EXPIRED

**DiscountDetail — 值对象**（供 Order BC 使用，Promotion 在 calculatePrice 中返回）

| 属性 | 类型 | 说明 |
|------|------|------|
| type | String | 优惠类型：`COUPON`（Phase 2 扩展 `ACTIVITY`） |
| sourceId | Long | 来源 ID（couponId） |
| amountCents | Long | 本行分摊的优惠金额（分） |
| description | String | 优惠描述，如"满100减20" |

**折扣计算逻辑**（应用服务 / 领域服务）：

1. 加载 Coupon，校验 status=AVAILABLE、userId 匹配、expiresAt > now
2. 计算订单基准总额 = Σ(unitPriceCents × quantity)
3. 校验基准总额 ≥ thresholdCents
4. 计算折扣金额：
   - AMOUNT_OFF：discountCents
   - PERCENTAGE_OFF：基准总额 × (100 − discountRate) / 100，若超过 maxDiscountCents 则取 maxDiscountCents
5. 按各行金额占比分摊折扣到行（尾差分配到最后一行）
6. 返回 lineDiscounts + totalDiscountCents + payableAmountCents

#### 事件流

**API（REST）**：

| 方法 | 路径 | 说明 | 调用方 |
|------|------|------|--------|
| POST | `/api/coupon-templates` | 创建券模板 | admin |
| GET | `/api/coupon-templates` | 查询券模板列表 | admin |
| GET | `/api/coupon-templates/{id}` | 查询单个模板 | admin |
| PUT | `/api/coupon-templates/{id}` | 编辑券模板 | admin |
| PUT | `/api/coupon-templates/{id}/deactivate` | 停用券模板 | admin |
| POST | `/api/coupons/issue` | 系统发券 `{ templateId, userIds }` | admin |
| GET | `/api/coupon-templates/claimable` | 可领取的模板列表 | web 领券中心 |
| POST | `/api/coupons/claim` | 用户领券 `{ templateId }` | web 领券中心 |
| GET | `/api/coupons/my` | 我的优惠券 `?status=AVAILABLE\|USED\|EXPIRED` | web |
| GET | `/api/coupons/available` | 当前订单可用券 `?orderAmountCents=xxx` | web 结账页 |
| POST | `/api/promotion/calculate-price` | 价格计算 `{ items, userId, couponId }` | web 结账页预览 / Order 同步调用 |
| POST | `/api/coupons/{id}/lock` | 锁券 `{ orderId }` | Order 同步调用 |
| POST | `/api/coupons/{id}/redeem` | 核销券 | Order 同步调用 |
| POST | `/api/coupons/{id}/release` | 释放券 | Order 同步调用 |

用户身份：`/api/coupons/my`、`/api/coupons/available`、`/api/coupons/claim` 通过 JWT → userId 传递。

**Kafka 事件（供 Activity 消费，Phase 1 可选）**：

| Topic | 事件 | 关键 Payload |
|-------|------|-------------|
| `promotion.coupon.issued` | CouponIssued | couponId, templateId, userId, name, type, occurredAt |
| `promotion.coupon.redeemed` | CouponRedeemed | couponId, orderId, userId, discountCents, occurredAt |

Phase 1 可不发布 Kafka 事件（Order 事件 payload 已携带 discounts，Activity 可从中获取优惠信息）。Phase 2 按需补充。

#### 需求场景

- 🔲 1.1 创建满减券模板（名称、门槛、减免金额、有效天数、总量、限领数）→ 成功返回 templateId
- 🔲 1.2 创建折扣券模板（名称、门槛、折扣率、封顶、有效天数、总量、限领数）→ 成功
- 🔲 1.3 门槛/金额不合法时创建失败
- 🔲 1.4 编辑券模板 → 成功
- 🔲 1.5 停用券模板 → INACTIVE，不可再发放
- 🔲 2.1 系统发券（指定模板+用户列表）→ 为每用户创建一张券
- 🔲 2.2 发券超出模板总量时应失败
- 🔲 2.3 用户已达限领数时应跳过
- 🔲 3.1 用户领券 → 创建券实例，status=AVAILABLE
- 🔲 3.2 模板已领完（issuedQuantity ≥ totalQuantity）时领券失败
- 🔲 3.3 用户已达限领数时领券失败
- 🔲 4.1 查询可用券列表 → 返回该用户 AVAILABLE 且未过期且满足门槛的券
- 🔲 4.2 计算满减券折扣 → 返回正确的折扣金额和实付
- 🔲 4.3 计算折扣券折扣 → 返回正确的折扣金额（含封顶）和实付
- 🔲 4.4 订单金额未达门槛时计算应返回错误
- 🔲 5.1 锁券 → AVAILABLE → LOCKED，记录 orderId
- 🔲 5.2 锁定非 AVAILABLE 状态的券应失败
- 🔲 5.3 核销券 → LOCKED → USED
- 🔲 5.4 释放券 → LOCKED → AVAILABLE
- 🔲 5.5 释放已核销的券（取消退券）→ USED → AVAILABLE
- 🔲 6.1 查询我的优惠券 → 按 status 筛选返回
- 🔲 7.1 过期扫描 → AVAILABLE 且 expiresAt < now 的券置为 EXPIRED

---

### Order（🟡 中等，🔄 需调整：PlaceOrder、CancelOrder、事件消费）

#### 领域模型变更

- Order 新增 `couponId: Long?`（可选，记录使用的优惠券 ID）
- Order.totalAmountCents 语义变为**实付金额**（优惠后），Phase 1 无券时行为不变（实付 = 基准总额）
- OrderLineItem 新增 `discounts: List<DiscountDetail>`（🔲 全新，来自 Theme 决策 D6）
  - DiscountDetail: `{ type, sourceId, amountCents, description }` 值对象
  - Phase 1 只有 `type=COUPON`，Phase 2 扩展 `type=ACTIVITY`
- OrderLineItem.totalPriceCents 保持为基准小计（unitPriceCents × quantity），不含折扣

**不变式变更**：
- couponId 可选；非空时表示本单使用了优惠券
- totalAmountCents = Σ(line.totalPriceCents) − Σ(所有 line.discounts.amountCents)，≥ 0

#### 事件流变更

**同步调用新增（Order → Promotion）**：

| 调用 | 时机 | 说明 |
|------|------|------|
| calculatePrice(items, userId, couponId) | PlaceOrder 开始时 | 有 couponId 时调用，获取折扣明细 |
| lockCoupon(couponId, orderId) | PlaceOrder 占库存后 | 有 couponId 时调用，锁定券 |
| redeemCoupon(couponId) | onPaymentCompleted | 有 couponId 时调用，核销券 |
| releaseCoupon(couponId) | CancelOrder | 有 couponId 时调用，释放券 |

**出站端口新增**：
- `PromotionPricePort`：calculatePrice
- `CouponLifecyclePort`：lock / redeem / release

**PlaceOrder 流程变更**（有 couponId 时）：
1. 校验用户、SKU（✅ 已有）
2. 🆕 调用 Promotion.calculatePrice → 获取 lineDiscounts
3. 构建 OrderLineItem 时写入 discounts
4. 计算 totalAmountCents = 基准总额 − 总折扣
5. 占库存（✅ 已有）
6. 🆕 调用 Promotion.lockCoupon
7. 创建支付单（金额为 totalAmountCents，即实付）（✅ 已有，金额来源变化）
8. 保存订单、发布 OrderCreated（✅ 已有）
9. 🆕 补偿：步骤 6 失败时释放步骤 5 的库存

**onPaymentCompleted 变更**：
- ✅ 置 PAID（已有）
- 🆕 有 couponId 时同步调用 Promotion.redeemCoupon
- ✅ 创建履约单（已有）

**CancelOrder 变更**：
- ✅ 释放库存、退款（已有）
- 🆕 有 couponId 时同步调用 Promotion.releaseCoupon

**OrderCreated / OrderCancelled / OrderCompleted 事件 payload 变更**：
- 🆕 增加 `discounts: [{ type, sourceId, amountCents, description }]`（订单级汇总，供 Activity 消费）
- 🆕 增加 `couponId`（可选）

#### 需求场景变更

- 🔲 1.10 提交含 couponId 的订单 → 调用价格计算 → 锁券 → 订单含折扣明细 → 支付金额为实付
- 🔲 1.11 couponId 对应的券不可用（已用/已过期/不属于该用户/未达门槛）时下单失败
- 🔲 1.12 锁券失败时（券已被他人锁定）应释放已占库存并返回错误
- 🔲 1.13 不传 couponId 时行为与现有完全一致（兼容）
- 🔄 2.x 取消订单时，有 couponId 应同步释放/退还券
- 🔄 4.1 PaymentCompleted 后，有 couponId 应同步核销券

---

### Cart（🟢 轻微，Phase 1 无变更）

Phase 1 不修改 Cart BC。券的选择发生在结账页（由前端与 Promotion BC 直接交互），不在购物车。Phase 2 引入促销活动后，Cart 结算预览将接入 Promotion 价格引擎展示活动价。

---

### Payment（🟢 轻微，无模型变更）

Payment 无感知券的存在。Order 传入的 `amountCents` 已经是优惠后的实付金额。现有 `createPayment(orderId, amountCents)` 接口无需变更。

---

### BFF（🟢 轻微）

新增路由代理 Promotion BC 的 API：

| 路径前缀 | 代理目标 |
|---------|---------|
| `/api/coupon-templates` | Promotion BC |
| `/api/coupons` | Promotion BC |
| `/api/promotion` | Promotion BC |

---

### Activity（⚪ 无变更）

Phase 1 不修改 Activity BC。Order 事件 payload 增加的 discounts 字段会被 Activity 作为 BusinessActivity 的 payload 存储，无需额外处理。Phase 2 若需"优惠券使用分析"维度，再扩展 Activity。

---

### 前端（frontend/web）（🟡 中等）

#### 新增/修改页面与组件

| 页面/组件 | 路径 | 说明 |
|----------|------|------|
| CheckoutPage（修改） | `/checkout` | 新增优惠券选择区域 |
| CouponCenterPage（新增） | `/coupons` | 领券中心：展示可领取的券模板，点击领取 |
| MyCouponsPage（新增） | `/my/coupons` | 我的优惠券：分 tab（可用/已用/已过期） |
| CouponSelector（新增组件） | — | 结账页内嵌的券选择器：展示可用券列表，选中/取消 |
| CouponCard（新增组件） | — | 单张券的展示卡片：名称、类型标签、面额、门槛、有效期 |

#### 数据流与状态

- **结账页**：加载时调用 `GET /api/coupons/available?orderAmountCents=xxx` 获取可用券；选中券后调用 `POST /api/promotion/calculate-price` 预览折扣；提交时传 couponId 给 `POST /api/orders`
- **领券中心**：调用 `GET /api/coupon-templates/claimable` 获取可领列表；点击领取调用 `POST /api/coupons/claim`
- **我的优惠券**：调用 `GET /api/coupons/my?status=xxx`

#### 界面规格（粗粒度）

**结账页优惠券区域**（在商品摘要与地址之间）：
```
┌─────────────────────────────────┐
│ 🏷 优惠券                    ▸  │  ← 点击展开券列表
│   已选：满100减20  -¥20.00      │  ← 选中后显示
└─────────────────────────────────┘
展开后：
┌─────────────────────────────────┐
│ ○ 满100减20    满100元可用  3天后过期 │
│ ● 全场8折     满200元可用  7天后过期 │  ← radio 单选
│ ○ 不使用优惠券                      │
└─────────────────────────────────┘
```

**价格摘要区域**（底部提交前）：
```
商品总价         ¥188.00
优惠券           -¥20.00   ← 红色
─────────────────────────
实付金额         ¥168.00   ← 加粗
```

#### 手工验收 checklist

- [ ] 结账页加载后，优惠券区域可见
- [ ] 有可用券时，展开显示券列表
- [ ] 选中一张券后，价格摘要实时更新（显示优惠金额和实付）
- [ ] 取消选券后，价格恢复原价
- [ ] 提交订单成功（含券），跳转订单详情
- [ ] 订单详情页展示优惠信息
- [ ] 领券中心页面可见可领券模板，点击领取成功
- [ ] 我的优惠券页面分 tab 展示

---

### 前端（frontend/admin）（🟡 中等）

#### 新增页面

| 页面 | 路径 | 说明 |
|------|------|------|
| CouponTemplatePage（新增） | `/coupon-templates` | 券模板管理：列表 + 创建/编辑/停用 |

#### 界面规格（粗粒度）

**券模板列表**：
```
┌──────────────────────────────────────────────────────────────────┐
│ 券模板管理                                      [+ 创建券模板]   │
├────────┬────┬──────┬──────┬─────┬──────┬──────┬──────┬─────────┤
│ 名称   │ 类型│ 门槛  │ 优惠  │有效期│ 已发/总量│每人限│ 状态  │ 操作    │
│ 新人券 │满减 │¥100  │-¥20  │7天  │ 50/100  │ 1   │ACTIVE│编辑 停用│
└────────┴────┴──────┴──────┴─────┴──────┴──────┴──────┴─────────┘
```

**创建/编辑弹窗**：表单含名称、类型（下拉）、门槛金额、优惠参数（按类型切换）、有效天数、总量、每人限领。

#### 手工验收 checklist

- [ ] 券模板列表页加载正常
- [ ] 创建满减券模板 → 成功出现在列表
- [ ] 创建折扣券模板 → 成功
- [ ] 编辑模板 → 更新生效
- [ ] 停用模板 → 状态变为 INACTIVE

---

### BC 间数据流

```
Promotion: CouponTemplate → 发券/领券 → Coupon
Promotion: Coupon + items → calculatePrice → DiscountDetail[]
Order: PlaceOrder(couponId) → Promotion.calculatePrice → Promotion.lockCoupon
Order: onPaymentCompleted → Promotion.redeemCoupon
Order: CancelOrder → Promotion.releaseCoupon
Order: OrderCreated/Cancelled/Completed 事件 → Activity（payload 含 discounts）
前端 web: CheckoutPage → Promotion API（available + calculatePrice）→ Order API（couponId）
前端 admin: CouponTemplatePage → Promotion API（template CRUD + issue）
BFF: 代理 /api/coupon-templates、/api/coupons、/api/promotion → Promotion BC
```

---

## 四、迭代计划

### 迭代 0：Promotion BC 骨架 + 券模板管理 ✅ 已完成

**涉及 BC**：Promotion（新建）、BFF
**前置依赖**：无

**后端**：
- 通过 `add-bounded-context` 创建 Promotion BC 四层骨架（promotion-service）
- CouponTemplate 聚合根 + CRUD API（创建、查询列表、查询单个、编辑、停用）
- BFF 新增 `/api/coupon-templates` 代理路由

**前端**：
- `frontend/admin` 新增 CouponTemplatePage（`/coupon-templates`）：模板列表 + 创建/编辑弹窗 + 停用

**验收**：
- Admin 可创建满减/折扣两种券模板
- `GET /api/coupon-templates` 返回模板列表
- 停用后 status 变为 INACTIVE

---

### 迭代 1：券实例（发券、领券、查询、过期）

**涉及 BC**：Promotion
**前置依赖**：迭代 0

**后端**：
- Coupon 聚合根 + 状态机（AVAILABLE → LOCKED → USED / EXPIRED）
- 发券 API：`POST /api/coupons/issue`（系统发券）
- 领券 API：`POST /api/coupons/claim` + `GET /api/coupon-templates/claimable`
- 查询 API：`GET /api/coupons/my`（我的优惠券）+ `GET /api/coupons/available`（可用券）
- 过期扫描：定时任务，AVAILABLE 且 expiresAt < now → EXPIRED
- BFF 新增 `/api/coupons` 代理路由

**前端**：
- `frontend/web` 新增 CouponCenterPage（`/coupons`）：可领券列表 + 领取
- `frontend/web` 新增 MyCouponsPage（`/my/coupons`）：分 tab 展示
- `frontend/admin` CouponTemplatePage 增加「发券」操作（选模板 + 输入 userId 列表）

**验收**：
- Admin 可对指定用户发券；超出总量或限领数时正确拒绝
- 用户可在领券中心领券；领完/达上限时提示
- 我的优惠券页面分 tab 正确展示
- 可用券查询按门槛正确过滤
- 到期券自动变为 EXPIRED

---

### 迭代 2：价格计算 + 用券下单（核心集成）

**涉及 BC**：Promotion、Order、BFF
**前置依赖**：迭代 1

**后端**：
- Promotion：calculatePrice API + lock/redeem/release API
- Order：
  - OrderLineItem 新增 `discounts: List<DiscountDetail>`
  - Order 新增 `couponId: Long?`
  - PlaceOrder 流程接入价格计算 + 锁券（含锁券失败补偿释放库存）
  - onPaymentCompleted 增加 redeemCoupon
  - CancelOrder 增加 releaseCoupon
  - OrderCreated/Cancelled/Completed 事件 payload 增加 discounts
  - 订单查询返回 discounts
- BFF 新增 `/api/promotion` 代理路由

**前端**：
- `frontend/web` CheckoutPage 新增优惠券选择区域 + 价格摘要更新
- `frontend/web` OrderDetailPage 展示优惠信息

**验收**：
- 结账页展示可用券列表，选券后实时更新价格
- 提交含券订单 → 支付金额为优惠后实付
- 支付完成后券状态变为 USED
- 取消/超时后券状态恢复为 AVAILABLE
- 不传 couponId 时与现有行为完全一致

**E2E 验收**：

| 用例 | 场景概述 |
|------|---------|
| BIZ-CPN-001 | 直接购买实体商品 + 使用满减券 → 结账页选券 → 价格正确 → 下单 → 支付 → 券核销 |
| BIZ-CPN-002 | 使用折扣券下单 → 支付超时/取消 → 券释放回可用 |

---

## 一致性检查

| 维度 | 检查项 | 结果 |
|------|--------|------|
| 场景完整 | 所有场景已盘点？主流程做了 L3？支撑流程说明了影响边界？ | ✅ F1-F6 全覆盖；F1 做了 L3（主成功 + 补偿 + 数据依赖验证） |
| 事件完整 | L3 覆盖主成功路径 + 补偿路径？状态门禁无遗漏？ | ✅ 主成功 9 步 + 补偿 5 步；Coupon 状态机 5 种流转 |
| 数据可达 | L3 数据依赖中所有 ❌ 在变更分析中已补齐？跨 BC 传递链完整？ | ✅ 6 项数据依赖全部在第三章有对应模型/字段 |
| 场景↔变更 | 场景分析中的每个 BC 影响在变更分析中有对应规格，反之亦然 | ✅ F1→Order+Promotion；F2→Promotion(Template)；F3→Promotion(Coupon)；F4→前端+Promotion API；F5→前端+Promotion API；F6→Promotion(过期扫描) |
| 变更内部 | 需求场景有模型支撑；payload 与模型一致；不变式与状态机一致 | ✅ Coupon 状态机与 lock/redeem/release 场景一一对应；DiscountDetail 定义与 Order 事件 payload 一致 |
| 前端 | 变更分析中有「前端变更」节；界面规格粗粒度描述完整；手工验收 checklist 可独立执行 | ✅ web + admin 均有页面规格和验收 checklist |
| 运营配置 | 需要手动录入的配置项是否已在迭代计划中列为独立工作项 | ✅ 券模板管理在迭代 0（admin CRUD），无需额外配置 |

---

## 交付跟踪

### 迭代 0：Promotion BC 骨架 + 券模板管理 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Promotion BC 四层骨架 | add-bounded-context | — | ✅ 完成 |
| 2 | Promotion: CouponTemplate CRUD 特性 | evolve-feature | #1 | ✅ 完成 |
| 3 | BFF: Promotion 代理路由 | integration | #2 | ✅ 完成 |
| 4 | Admin: CouponTemplatePage 券模板管理页 | frontend-development | #3 | ✅ 完成 |

**交付日期**：2026-03-15
**下一迭代**：迭代 1（券实例：发券、领券、查询、过期）

---

### 迭代 1：券实例（发券、领券、查询、过期） ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Promotion: Coupon 聚合根 + 发券/领券/查询/过期 API | evolve-feature | 迭代 0 | ✅ 完成 |
| 2 | Web: CouponCenterPage 领券中心 + MyCouponsPage 我的优惠券 | frontend-development | #1 | ✅ 完成 |
| 3 | Admin: CouponTemplatePage 增加发券操作 | frontend-development | #1 | ✅ 完成 |
| 4 | 迭代验收 | deliver-requirement | #1–#3 | ✅ 完成 |

**交付日期**：2026-03-15

---

### 迭代 2：价格计算 + 用券下单（核心集成） ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | Promotion: calculatePrice + lock/redeem/release + available API | evolve-feature | 迭代 1 | ✅ 完成 |
| 2 | Order: couponId + discounts + 集成 Promotion Ports | evolve-feature | #1 | ✅ 完成 |
| 3 | Web: CheckoutPage 选券 + OrderDetailPage 优惠展示 | frontend-development | #2 | ✅ 完成 |
| 4 | E2E 验收 (BIZ-CPN-001, BIZ-CPN-002) | deliver-requirement | #1–#3 | ✅ 完成 |

**交付日期**：2026-03-15
