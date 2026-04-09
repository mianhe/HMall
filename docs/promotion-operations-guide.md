# Promotion 交付整理与操作指南

面向本轮“用户分群与圈选 + 用户定向与满件折扣”交付后的落地执行文档，目标是两件事：

1. 把当前代码与配置能力整理成可持续维护状态。
2. 给运营/开发一套统一的活动配置与用户配置操作路径。

---

## 一、交付整理清单

### 1) 需要重构的（结构性优化）

- **规则结构复用**：将 `frontend/admin` 中 `CouponTemplatePage`、`PromotionActivityPage`、`UserSegmentationPage` 的定向规则解析与构造逻辑抽到共享 composable（如 `useTargetingRuleForm`），避免三处重复。
- **User 规则引擎分层**：将 `services/user-service` 的规则匹配与原因统计从 `UserApplicationService` 下沉到独立领域服务（`SegmentRuleEvaluator`），应用层只做编排。
- **DTO 映射统一**：为 `user-service` 新增 `api/dto` 映射器，替代控制器内手写 `toDto()/toDomain()`，减少控制器体积。
- **E2E 数据准备抽象**：把 `BIZ-UM` 用例中的“创建用户/创建活动/激活活动”搬到 shared helper，减少重复与 flaky 风险。

### 2) 需要清理的（技术债清扫）

- **历史用例不稳定项**：`frontend/admin` 的镭雕业务用例 `BIZ-LE-002/003` 存在历史不稳定，建议单独清理，不与本需求发布门禁耦合。
- **环境初始化脚本化**：把 `user_account` 字段补齐、`user_segment_rule` 表准备下沉到迁移脚本（Flyway/Liquibase），避免依赖手工 SQL。
- **日志与错误语义统一**：User 与 Promotion 的降级/门禁错误 message 统一命名（如 `LEVEL_NOT_MATCH`、`EXCLUDED_TAG_HIT`）并形成错误码表。
- **文档状态同步**：每次迭代结束后同步 `theme.md`、`overview.md`、`project-status.md`，避免“代码已交付/文档还在开发中”。

### 3) 需要简化的（降低操作复杂度）

- **运营入口收敛**：用户标签/等级/圈选统一在 `/user-segmentation`，活动页面仅配置活动规则，不再承载“补录用户画像”。
- **字段心智模型简化**：统一“逗号分隔输入 -> Set 去重 -> 空集合即未配置”的处理规则，减少运营误配。
- **验收流程简化**：需求相关 E2E 单独跑 `BIZ-UM`，系统回归跑 `Smoke P0`；先验证需求，再做全量回归。
- **排障路径简化**：固定顺序“服务状态 -> 关键 API -> E2E”，禁止一上来盲跑全量测试。

---

## 二、活动配置操作指南

### A. 券模板配置（`/coupon-templates`）

适用场景：运营需要发放“满减券/折扣券”，并可按用户等级/标签定向。

#### UI 操作步骤

1. 进入 `frontend/admin` 路由 `/coupon-templates`。
2. 点击“新增模板”，填写基础字段：
   - `name`
   - `type`: `AMOUNT_OFF` 或 `PERCENTAGE_OFF`
   - `thresholdCents`
   - `discountCents` 或 `discountRate + maxDiscountCents`
   - `totalQuantity`、`perUserLimit`、`validDays`
3. （可选）配置定向规则 `targetingRule`：
   - `levelsIn`
   - `tagsAny`
   - `tagsAll`
   - `excludeTags`
4. 保存后可在列表停用（`deactivate`）或对指定用户发券（`issue`）。

#### API 对应

- `POST /api/promotion/coupon-templates`
- `POST /api/promotion/coupon-templates/{id}/deactivate`
- `POST /api/promotion/coupon-templates/{id}/issue`

---

### B. 活动配置（`/promotion-activities`）

适用场景：运营配置单品直降、订单满减、满件折扣，并支持定向。

#### UI 操作步骤

1. 进入路由 `/promotion-activities`。
2. 创建活动并填写：
   - 基础：`name`、`startAt`、`endAt`、`priority`、`mutexGroupCode`
   - 类型：
     - `SKU_AMOUNT_OFF`：配置 `targetSkuIds + discountCents`
     - `ORDER_AMOUNT_OFF`：配置 `thresholdCents + discountCents`
3. （可选）配置 `targetingRule`（同券模板）。
4. （可选）启用 `pieceRule`：
   - `scopeType`: `ORDER` / `SKU`
   - `scopeIds`（当 `scopeType=SKU`）
   - `minQuantity`
   - `discountType`: `AMOUNT_OFF` / `PERCENTAGE_OFF`
   - `discountValue`
   - `maxDiscountCents`（可选）
5. 创建后执行上线（activate）/下线（deactivate）。

#### API 对应

- `POST /api/promotion/activities`
- `POST /api/promotion/activities/{id}/activate`
- `POST /api/promotion/activities/{id}/deactivate`

#### 配置建议

- 同一业务目标活动放同一 `mutexGroupCode`，避免叠加过度让利。
- `priority` 只用于同折扣额下稳定排序，不替代业务门槛设计。
- 满件规则优先用 `ORDER` 范围做 MVP，再按 `SKU` 精细化。

---

## 三、用户配置操作指南

统一入口：`/user-segmentation`

### A. 单用户画像维护

1. 输入 `userId` 查询用户。
2. 修改并保存 `level`（建议只用 `L1/L2/L3`）。
3. 修改并保存 `tags`（逗号分隔，系统会去重/去空值）。
4. 通过 `GET /api/users/{id}/segments` 复核结果。

对应 API：

- `PUT /api/users/{id}/level`
- `PUT /api/users/{id}/tags`
- `GET /api/users/{id}/segments`

### B. 圈选规则维护

1. 新建规则：填写 `name + conditions`。
2. 执行预览（preview），关注：
   - `hitCount`
   - `sampleUserIds`
   - `reasonStats`
3. 满足门禁后激活（activate）：
   - 当前门禁：`hitCount > 0` 才允许激活。
4. 在活动页配置定向时复用同一套字段语义，确保一致。

对应 API：

- `POST /api/users/segment-rules`
- `POST /api/users/segment-rules/{id}/preview`
- `POST /api/users/segment-rules/{id}/activate`
- `GET /api/users/segment-rules`

---

## 四、Promotion 总体技能（团队操作准则）

### 技能 1：规则建模技能

- 把活动拆为“门槛条件 + 优惠动作 + 排他关系”三层。
- 先保证规则可解释，再追求复杂组合。
- 统一字段语义：活动、券模板、用户圈选都使用相同的 `TargetingRule/SegmentCondition` 词汇。

### 技能 2：联动验证技能

- 每次改活动规则后，至少做三步验证：
  1. `preview-sku-prices` 看活动预估
  2. `calculate-price` 看最终优惠拆分
  3. 前端页面检查解释信息（命中/未命中）
- 用户画像变更后必须复测定向命中是否即时变化。

### 技能 3：发布与回归技能

- 本需求最小回归组合：
  - `services/user-service`: `mvn test -q`
  - `services/promotion-service`: `mvn test -q`
  - `frontend/web`: `npm run test:smoke:e2e:p0`
  - `frontend/admin`: `npm run test:business:e2e -- --grep BIZ-UM`
  - `frontend/web`: `npm run test:business:e2e -- --grep BIZ-UM`
- 先需求回归，再全量回归；失败时先查环境状态（服务/端口/API）再查代码。

### 技能 4：故障处置技能

- 常见故障与优先排查：
  - 登录 500：先看 `user-service` 与数据库表结构
  - 促销不命中：先看用户 `segments` 返回，再看活动 `targetingRule`
  - E2E 卡慢：先检查依赖服务是否就绪，再重跑
- 标准顺序：`status` -> 关键 API `curl` -> 定向 E2E。

---

## 五、附录：最小联调检查单

- [ ] 用户 `level/tags` 可查询、可更新
- [ ] 圈选规则可创建、可预览、可激活
- [ ] 活动可配置定向与满件规则并上线
- [ ] `calculate-price` 返回活动/优惠券分项与解释信息
- [ ] 页面路由 `/user-segmentation`、`/promotion-activities`、`/coupon-templates` 可用
- [ ] `BIZ-UM-001/002/003` 全部通过
