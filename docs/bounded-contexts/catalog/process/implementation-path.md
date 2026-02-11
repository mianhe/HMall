# Catalog 验收实现路径（按 Feature 分步、用例一部分一部分绿）

当前：**category**、**product**、**spec-dimension**、**sku** 已全绿（Step 1.1～2.3 已完成）。

依赖关系：**SKU 依赖规格维度与选项**（创建 SKU 需要先有 dimension/option），因此必须先实现 **spec-dimension**，再实现 **sku**。

---

## 一、先让哪些用例通过（整体顺序）

| 顺序 | 目标 | 说明 |
|------|------|------|
| 1 | **spec-dimension 的 9 个场景** 逐个变绿 | 先做维度与选项的接口，SKU 才能用 |
| 2 | **sku 的 8 个场景** 逐个变绿 | 在 spec-dimension 全绿之后做 SKU 接口 |

**category / product** 已绿，无需再动。

---

## 二、第一个 Feature：spec-dimension（9 个场景）

建议按下面 5 小步做，每步只实现最少接口，让对应用例先绿，再进入下一步。

### Step 1.1：添加维度成功（1 个场景绿）

- **要通过的场景**：`为 SPU 添加规格维度时应成功并返回维度名称`
- **实现**：
  - `POST /api/products/{spuId}/dimensions` → 201，body 含 `id, spuId, name, required, sortOrder, affectsAppearance`
  - SPU 存在性校验（不存在可先 404，或本步只做存在时成功）
- **验收**：只跑该场景或只跑 `spec-dimension.feature`，确认 1 个通过。

### Step 1.2：添加选项成功（1 个场景绿）

- **要通过的场景**：`为维度添加可选项时应成功并返回选项值`
- **实现**：
  - `POST /api/products/{spuId}/dimensions/{dimensionId}/options` → 201，body 含 `id, optionValue, sortOrder, image` 等
  - 维度归属 SPU 校验（可选本步或后面统一）
- **验收**：spec-dimension 中至少 2 个场景绿。

### Step 1.3：带图片的选项（1 个场景绿）

- **要通过的场景**：`为影响外观的维度添加带图片的选项时应成功并返回选项及图片`
- **实现**：创建维度时支持 `affectsAppearance`；创建选项时支持 `image`，响应中返回 `image`。
- **验收**：3 个场景绿。

### Step 1.4：查询维度及选项列表（2 个场景绿）

- **要通过的场景**：
  - `请求某 SPU 下维度及选项时应返回维度列表及选项`
  - `请求维度及选项且选项有图片时应返回选项图片`
- **实现**：`GET /api/products/{spuId}/dimensions` → 200，返回维度列表，每个维度含 `options`（含 `image`）。
- **验收**：5 个场景绿。

### Step 1.5：错误与边界（4 个场景绿）

- **要通过的场景**：
  - `SPU 不存在时添加维度应失败并返回 404`
  - `同 SPU 内维度名称重复时添加维度应失败并返回错误提示`
  - `同维度内选项值重复时添加选项应失败并返回错误提示`
  - `维度不存在时添加选项应失败并返回 404`
- **实现**：
  - POST dimension：spuId 不存在 → 404；同 SPU 下 name 重复 → 400 + message。
  - POST option：dimensionId 不存在或不属于该 SPU → 404；同维度下 optionValue 重复 → 400 + message。
- **验收**：spec-dimension 全部 9 个场景绿。

---

## 三、第二个 Feature：sku（8 个场景）

在 **spec-dimension 全绿** 之后再做。

### Step 2.1：创建 SKU 成功（1 个场景绿）

- **要通过的场景**：`在 SPU 下选齐必填 Option 及价格创建 SKU 时应成功并返回 SKU 信息`
- **实现**：
  - `POST /api/products/{spuId}/skus`，body：`specOptionIds`（必填维度各选一）、`priceCents`、可选 `displayName`
  - 返回 201，body 含 `id, spuId, priceCents, displayName, specValues`（如 `[{ dimensionName, optionValue }]`）
- **验收**：只跑 sku.feature 时 1 个场景绿。

### Step 2.2：SKU 列表与详情（2 个场景绿）

- **要通过的场景**：
  - `按 SPU 请求该 SPU 下 SKU 时应返回 SKU 列表`
  - `按 SPU 与 SKU ID 请求详情时应返回 SKU 信息`
- **实现**：
  - `GET /api/products/{spuId}/skus` → 200，SKU 列表
  - `GET /api/products/{spuId}/skus/{skuId}` → 200，单 SKU 详情（含 specValues）
- **验收**：3 个场景绿。

### Step 2.3：SKU 错误与边界（5 个场景绿）

- **要通过的场景**：
  - `SPU 不存在时创建 SKU 应失败并返回 404`
  - `未选齐必填维度时创建 SKU 应失败并返回错误提示`
  - `传入不属于该 SPU 的 Option 时创建 SKU 应失败并返回错误提示`
  - `价格为负数时创建 SKU 应失败并返回错误提示`
  - `请求不存在的 SKU 详情时应返回 404`
- **实现**：
  - POST sku：spuId 不存在 → 404；specOptionIds 未覆盖某必填维度 → 400；option 不属于该 SPU → 400；priceCents < 0 → 400。
  - GET sku 详情：skuId 不存在或不属于该 spuId → 404。
- **验收**：sku 全部 8 个场景绿。

---

## 四、如何「只跑一个 Feature」做验收

- **按路径只跑一个 feature**（推荐）：  
  在 `RunCucumberTest` 或 `junit-platform.properties` 里用 `cucumber.features` 限定，例如只跑  
  `src/test/resources/features/catalog/spec-dimension.feature`，  
  这样先实现 spec-dimension 时不会受 sku 失败干扰。

- **按 tag 只跑部分场景**：  
  在 feature 里给场景加 tag（如 `@spec-dimension-happy`），运行时用 `cucumber.filter.tags` 只跑这些场景，适合 Step 1.1～1.4 这种「先让少数场景绿」的节奏。

（若你项目里已有 tag 或 features 配置，可沿用现有方式，只要保证「一次只关注一个 feature 或一小批场景」即可。）

---

## 五、小结表

| 阶段 | Feature | 本阶段要绿的场景 | 本阶段主要实现 |
|------|---------|------------------|----------------|
| 1.1 | spec-dimension | 添加维度成功 | POST dimensions |
| 1.2 | spec-dimension | 添加选项成功 | POST dimension options |
| 1.3 | spec-dimension | 带图片选项成功 | affectsAppearance + option.image |
| 1.4 | spec-dimension | 查询维度及选项（含图片） | GET dimensions |
| 1.5 | spec-dimension | 404/400 四个边界场景 | 各种校验与错误响应 |
| 2.1 | sku | 创建 SKU 成功 | POST skus |
| 2.2 | sku | 列表 + 详情 | GET skus, GET sku 详情 |
| 2.3 | sku | 五个错误/边界场景 | 404/400 校验 |

按上表从 **1.1** 做到 **2.3**，用例会一部分一部分通过，最后 **category + product + spec-dimension + sku** 全绿。
