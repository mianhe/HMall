# Promotion BC — 领域模型

> 本文件已覆盖优惠券（业务需求 1）与促销活动+价格引擎（业务需求 2）的当前实现，并包含业务需求 [用户定向与满件折扣](../../business-requirements/promotion-theme/user-targeting/overview.md) 与 [用户分群与圈选](../../business-requirements/user-management-theme/user-segmentation/overview.md) 的增量模型目标。

---

## 聚合

### CouponTemplate（聚合根）

优惠券模板，定义一类优惠券的规则。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 模板名称，如"满 100 减 20" |
| type | CouponType | AMOUNT_OFF（满减）/ PERCENTAGE_OFF（折扣） |
| thresholdCents | Long | 使用门槛（分），0 表示无门槛 |
| discountCents | Long? | 满减金额（分），仅 AMOUNT_OFF |
| discountRate | BigDecimal? | 折扣率（0~1），仅 PERCENTAGE_OFF |
| maxDiscountCents | Long? | 最高优惠上限（分），仅 PERCENTAGE_OFF |
| totalQuantity | Integer | 发放总量 |
| issuedQuantity | Integer | 已发放数量（初始 0） |
| perUserLimit | Integer | 每人限领数量 |
| validDays | Integer | 领取后有效天数 |
| status | TemplateStatus | ACTIVE / INACTIVE |
| createdAt | Instant | 创建时间 |

### 枚举

| 枚举 | 值 |
|------|------|
| CouponType | AMOUNT_OFF, PERCENTAGE_OFF |
| TemplateStatus | ACTIVE, INACTIVE |

---

### PromotionActivity（聚合根）

促销活动，承载商品维度/订单维度的让利规则。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 活动名称 |
| type | PromotionActivityType | SKU_AMOUNT_OFF / ORDER_AMOUNT_OFF |
| targetSkuIds | Set\<Long\> | 单品活动目标 SKU 集合（仅 SKU_AMOUNT_OFF 使用） |
| thresholdCents | Long? | 订单满减门槛（仅 ORDER_AMOUNT_OFF 使用） |
| discountCents | Long | 优惠金额（分） |
| mutexGroupCode | String? | 互斥组编码，同组活动择优 |
| priority | Integer | 优先级（同折扣时用于稳定选择） |
| startAt | Instant | 生效开始时间 |
| endAt | Instant | 生效结束时间 |
| status | PromotionActivityStatus | DRAFT / ACTIVE / INACTIVE |
| createdAt | Instant | 创建时间 |
| updatedAt | Instant | 更新时间 |
| targetingRule | TargetingRule? | 🔄 用户定向规则（标签/等级） |
| pieceRule | PieceRule? | 🔄 满件折扣规则 |

#### 状态机

```
DRAFT --activate()--> ACTIVE --deactivate()--> INACTIVE
```

#### 不变式

- 活动名称不能为空
- `endAt` 必须晚于 `startAt`
- `discountCents > 0`
- `priority >= 0`
- `SKU_AMOUNT_OFF` 必须有 `targetSkuIds`
- `ORDER_AMOUNT_OFF` 必须有 `thresholdCents`
- `targetingRule` 存在时，calculatePrice 需先校验用户分群命中
- `pieceRule` 存在时，必须满足 `minQuantity > 0`

### TargetingRule（值对象，来自用户定向需求）

| 属性 | 类型 | 说明 |
|------|------|------|
| levelsIn | Set\<String\>? | 允许命中的等级集合 |
| tagsAny | Set\<String\>? | 命中任一标签即可 |
| tagsAll | Set\<String\>? | 必须同时命中的标签集合 |
| excludeTags | Set\<String\>? | 命中即排除 |

> 说明：TargetingRule 的字段命名与 User 圈选规则 `SegmentCondition` 对齐，便于 admin 在两侧复用配置组件。

### PieceRule（值对象，来自满件折扣需求）

| 属性 | 类型 | 说明 |
|------|------|------|
| scopeType | String | SKU / SPU / ORDER |
| scopeIds | Set\<Long\>? | 作用范围（scopeType 非 ORDER 时） |
| minQuantity | Integer | 件数门槛 |
| discountType | String | AMOUNT_OFF / PERCENTAGE_OFF |
| discountValue | BigDecimal | 优惠值 |
| maxDiscountCents | Long? | 折扣上限（可选） |

---

### Coupon（聚合根）

优惠券实例，从模板发放给用户，承载完整的券生命周期。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| templateId | Long | 来源模板 ID |
| userId | Long | 持有用户 ID |
| name | String | 快照：模板名称 |
| type | CouponType | 快照：AMOUNT_OFF / PERCENTAGE_OFF |
| thresholdCents | Long | 快照：使用门槛（分） |
| discountCents | Long? | 快照：满减金额（分） |
| discountRate | BigDecimal? | 快照：折扣率 |
| maxDiscountCents | Long? | 快照：最高优惠上限（分） |
| status | CouponStatus | AVAILABLE / LOCKED / USED / EXPIRED |
| orderId | Long? | 关联订单 ID（锁定/核销时设置） |
| issuedAt | Instant | 发放时间 |
| expiresAt | Instant | 过期时间（issuedAt + validDays） |
| usedAt | Instant? | 核销时间 |

#### 状态机

```
AVAILABLE ──lock(orderId)──→ LOCKED ──redeem()──→ USED
    │                          │
    │                     release()
    │                          │
    │                          ↓
    │                      AVAILABLE
    ↓
  expire()
    ↓
  EXPIRED
```

#### 不变式

- 只有 AVAILABLE 状态的券可被锁定或过期
- 只有 LOCKED 状态的券可被核销或释放
- orderId 仅在 LOCKED 和 USED 状态时有值
- expiresAt = issuedAt + template.validDays

### 枚举

| 枚举 | 值 |
|------|------|
| CouponType | AMOUNT_OFF, PERCENTAGE_OFF |
| TemplateStatus | ACTIVE, INACTIVE |
| CouponStatus | AVAILABLE, LOCKED, USED, EXPIRED |
| PromotionActivityType | SKU_AMOUNT_OFF, ORDER_AMOUNT_OFF |
| PromotionActivityStatus | DRAFT, ACTIVE, INACTIVE |

---

## 价格引擎（PricingEngine）

统一算价逻辑（被 Order/Cart/Web 调用）：

1. 基于当前时间筛选 ACTIVE 且在有效期内的活动  
2. 计算活动优惠：
   - 单品活动：对命中 SKU 按行折扣
   - 订单活动：按订单金额与门槛命中后按行分摊
3. 同互斥组活动取最优，不同组可叠加
4. 在“活动后金额”上计算券优惠
5. 输出：
   - `activityDiscountAmountCents`
   - `couponDiscountAmountCents`
   - `discountAmountCents`
   - `payableAmountCents`
   - `lineItems[].discounts[]`（`type=ACTIVITY|COUPON`）

### UserSegmentResolver（领域服务端口）

在活动命中计算前，PricingEngine 通过 `UserSegmentResolver` 获取用户分群快照。

| 输入 | 输出 | 说明 |
|------|------|------|
| userId | level + tags | 由 User BC 同步提供 |

降级约束（来自用户分群与圈选需求）：
- User 查询失败时返回默认分群（如 `L1 + 空 tags`）
- 同时向上游返回可解释提示，避免“静默错误”
