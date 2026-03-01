# Cart 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。契约：`docs/bounded-contexts/cart/api.yaml`。Feature 目录：`services/cart-service/src/test/resources/features/cart/`。领域模型见 [domain-model.md](./domain-model.md)。

### 状态图例

- ✅ 已实现
- 🔄 部分完成
- 🔲 待实现

---

## 1. 添加商品到购物车
`cart-add-item.feature`

- ✅ 1.1 添加一个有效 SKU 到购物车时应成功并返回购物车项（含 skuId、quantity）
- ✅ 1.2 重复添加同一 SKU 时应累加数量而非新增一行
- ✅ 1.3 SKU 不存在时添加应失败并返回 404
- ✅ 1.4 数量 ≤ 0 时添加应失败并返回错误
- ✅ 1.5 未登录（无 userId）时添加应失败并返回 401

---

## 2. 查看购物车
`cart-query.feature`

- ✅ 2.1 查询当前用户购物车时应返回所有购物车项（含 SKU 展示信息：名称、价格、图片）
- ✅ 2.2 购物车为空时查询应返回空列表
- ✅ 2.3 SKU 已被下架或删除时，购物车项仍返回但标记为不可用

---

## 3. 修改购物车项
`cart-update-item.feature`

- ✅ 3.1 修改购物车项数量时应成功并返回更新后的数量
- ✅ 3.2 数量设为 0 时应删除该购物车项
- ✅ 3.3 数量为负数时应失败并返回错误
- ✅ 3.4 购物车项不存在时修改应返回 404

---

## 4. 删除购物车项
`cart-delete-item.feature`

- ✅ 4.1 删除已有购物车项时应成功
- ✅ 4.2 批量删除多个购物车项时应成功（结算后清理选中项）
- ✅ 4.3 购物车项不存在时删除应返回 404

---

## 5. 购物车结算预览
`cart-checkout.feature`

- ✅ 5.1 勾选若干购物车项进行结算预览时应返回选中项的摘要（商品信息、数量、小计、总价）
- ✅ 5.2 勾选项中包含不可用 SKU 时应失败并返回错误

---

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/cart/items` | 添加商品到购物车 |
| GET | `/api/cart` | 查询当前用户购物车 |
| PUT | `/api/cart/items/{cartItemId}` | 修改购物车项数量 |
| DELETE | `/api/cart/items/{cartItemId}` | 删除单个购物车项 |
| DELETE | `/api/cart/items` | 批量删除购物车项（body 传 ids） |

用户标识通过 JWT token → userId 传递，不在 URL 中暴露。

---

## 设计决策

| # | 决策 | 原因 |
|---|------|------|
| 1 | Cart 不快照 SKU 价格，展示时实时拉取 Catalog | 购物车商品价格应随 Catalog 变动实时更新；下单时才由 Order 快照 |
| 2 | 同一 (skuId, relatedSkuId) 去重，重复添加累加数量 | 电商通用体验；服务商品关联不同实体商品视为不同购物车项 |
| 3 | Cart 与用户 1:1，首次操作自动创建 | 无需显式"创建购物车"动作，简化 API |
| 4 | Cart → Order 结算由前端编排 | 前端从 Cart 取选中项 → 调用 Order API 创建订单 → 成功后清理已下单项；与现有「立即购买」流程复用 CheckoutPage |
| 5 | Cart 不发布跨 BC 领域事件 | 当前无其他 BC 需要订阅购物车变更；未来如需审计/统计可补充 |

---

## 6. 购物车展示关联服务
`cart-service.feature`

> 以下变更来自业务需求 [虚拟商品](../../business-requirements/virtual-product/overview.md)

- ✅ 6.1 查看购物车时，实体商品旁应展示可选的关联服务列表（从 Catalog ServiceBinding 查询）；已加入购物车的服务项价格按绑定价展示
- ✅ 6.2 添加服务 SKU 到购物车时，应记录 relatedSkuId（关联的实体 SKU）
- ✅ 6.3 结算预览时，实体商品与其关联服务应分组展示，小计与总价包含服务费用；服务费用按 ServiceBinding 绑定价计算（不使用服务 SKU 标准价）
- ✅ 6.4 同一服务 SKU 关联不同实体商品分别加入购物车后，查询应正常返回所有购物车项
- ✅ 6.5 删除实体商品时，应级联删除其关联服务项，避免出现不可见的孤儿服务项
- ✅ 6.6 修改实体商品数量时，应同步更新其关联服务项数量

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 状态 | Scenario 数 |
|------|----------------|------|-------------|
| 1. 添加商品到购物车 | cart-add-item.feature | ✅ 已实现 | 5 |
| 2. 查看购物车 | cart-query.feature | ✅ 已实现 | 3 |
| 3. 修改购物车项 | cart-update-item.feature | ✅ 已实现 | 4 |
| 4. 删除购物车项 | cart-delete-item.feature | ✅ 已实现 | 3 |
| 5. 购物车结算预览 | cart-checkout.feature | ✅ 已实现 | 2 |
| 6. 购物车展示关联服务 | cart-service.feature | ✅ 已实现 | 3 |
