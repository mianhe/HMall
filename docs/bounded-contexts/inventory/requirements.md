# Inventory 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。契约：`docs/bounded-contexts/inventory/api.yaml`。Feature 目录：`services/inventory-service/src/test/resources/features/inventory/`。集成与事件契约见 [event-flow.md](./event-flow.md)，领域模型见 [domain-model.md](./domain-model.md)。

### 状态图例

- ✅ 已实现
- 🔄 部分完成
- 🔲 待实现

---

## 1. 占用库存

`inventory-occupy.feature`

- ✅ 1.1 Order 调用占用接口且各 skuId 库存充足时应占用成功并返回成功
- ✅ 1.2 Order 调用占用接口且任一 skuId 库存不足时应占用失败并返回错误（如库存不足）
- ✅ 1.3 同一 orderId 重复调用占用接口时应幂等处理（不重复占用）
- ✅ 1.4 入参缺失 orderId 或 items 时应返回 400 等错误
- ✅ 1.5 占用成功时应发布 StockReserved（库存已占用）领域事件

---

## 2. 释放库存

`inventory-release.feature`

- ✅ 2.1 Order 调用释放接口时应释放该订单的所有占用
- ✅ 2.2 该订单无占用记录时调用释放接口应幂等（空操作，返回成功）
- ✅ 2.3 入参缺失 orderId 时应返回 400 等错误
- ✅ 2.4 释放成功时应发布 StockReleased（库存已释放）领域事件

---

## 3. 库存管理（管理后台）

`inventory-stock.feature`

- ✅ 3.1 按 skuId 初始化/更新库存
- ✅ 3.2 按 skuId 查询库存（available、reserved）
- ✅ 3.3 查询全部库存列表，返回所有已初始化的 SkuStock（skuId、available、reserved）
- ✅ 3.4 查询不存在的 skuId 时应返回 404
- ✅ 3.5 设置 available 为负数时应返回 400
- ✅ 3.6 按 skuId 列表批量查询库存（仅返回已有记录的 SKU，不存在的忽略）

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 状态 | Scenario 数 |
|------|----------------|------|-------------|
| 1. 占用库存 | inventory-occupy.feature | ✅ 已实现 | 5 |
| 2. 释放库存 | inventory-release.feature | ✅ 已实现 | 4 |
| 3. 库存管理 | inventory-stock.feature | ✅ 已实现 | 5 |
