# 商品限定上下文 API 接口说明书

> 本文档由 `catalog-api.yaml`（OpenAPI 3.0）生成，描述类别、商品（SPU）、规格维度与选项、SKU 的 REST 接口。实现与验收测试均按此规范。

**版本**：0.1.0

---

## 一、接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/categories` | 创建类别 |
| GET | `/api/categories` | 查询类别列表（按父类别） |
| POST | `/api/products` | 创建商品 |
| GET | `/api/products` | 按类别查商品列表 |
| GET | `/api/products/{id}` | 按 ID 查商品详情 |
| DELETE | `/api/products/{id}` | 删除商品 |
| POST | `/api/products/{spuId}/dimensions` | 为 SPU 添加规格维度 |
| GET | `/api/products/{spuId}/dimensions` | 查询某 SPU 下所有维度及选项 |
| POST | `/api/products/{spuId}/dimensions/{dimensionId}/options` | 为某维度添加可选项 |
| POST | `/api/products/{spuId}/skus` | 在 SPU 下创建 SKU |
| GET | `/api/products/{spuId}/skus` | 按 SPU 查该 SPU 下所有 SKU |
| GET | `/api/products/{spuId}/skus/{skuId}` | 按 SPU 与 SKU ID 查 SKU 详情 |

---

## 二、类目（Category）

### 2.1 创建类别

**POST** `/api/categories`

创建根级类别（不传 parentId）或在已有类别下创建子类别（传 parentId）。

**请求体**（application/json）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 类别名称 |
| description | string | 否 | 描述 |
| parentId | int64 | 否 | 父类别 ID，不传或 null 表示根级类别 |

**响应**

- **201** 创建成功，返回 `Category`（id, parentId, name, description）
- **404** 父类别不存在，返回 `Error`（message）

---

### 2.2 查询类别列表（按父类别）

**GET** `/api/categories`

不传 parentId 或为空表示查根目录下所有类别；传 parentId 表示查该类别下所有子类别。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| parentId | int64 | 否 | 父类别 ID，不传或空为根目录 |

**响应**

- **200** 类别列表，Body 为 `Category[]`

---

## 三、商品（Product / SPU）

### 3.1 创建商品

**POST** `/api/products`

仅在叶子类别下可创建；类别非叶子时返回 400/422。

**请求体**（application/json）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | int64 | 是 | 所属类别 ID |
| name | string | 是 | 商品名称 |
| description | string | 否 | 描述 |

**响应**

- **201** 创建成功，返回 `Product`（id, categoryId, name, description）
- **400** / **422** 业务校验失败（如仅叶子类别可挂商品），返回 `Error`
- **404** 类别不存在，返回 `Error`

---

### 3.2 按类别查商品列表

**GET** `/api/products?categoryId={categoryId}`

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | int64 | 是 | 类别 ID |

**响应**

- **200** 商品列表，Body 为 `Product[]`

---

### 3.3 按 ID 查商品详情

**GET** `/api/products/{id}`

**路径参数**：`id`（int64）商品 ID

**响应**

- **200** 商品详情，Body 为 `Product`
- **404** 商品不存在，返回 `Error`

---

### 3.4 删除商品

**DELETE** `/api/products/{id}`

**路径参数**：`id`（int64）商品 ID

**响应**

- **204** 删除成功，无响应体
- **404** 商品不存在，返回 `Error`

---

## 四、规格维度与选项

### 4.1 为 SPU 添加规格维度

**POST** `/api/products/{spuId}/dimensions`

**路径参数**：`spuId`（int64）SPU ID

**请求体**（application/json）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 维度名称，同 SPU 内唯一 |
| required | boolean | 是 | 创建 SKU 时是否必选 |
| sortOrder | int | 否 | 排序 |
| affectsAppearance | boolean | 否 | 该维度的 Option 才允许填图片；同 SPU 下至多一个为 true，默认 false |

**响应**

- **201** 创建成功，返回 `SpecDimension`（id, spuId, name, required, sortOrder, affectsAppearance）
- **404** SPU 不存在，返回 `Error`
- **400** 业务校验失败（如同 SPU 内维度名称重复），返回 `Error`

---

### 4.2 查询某 SPU 下所有维度及选项

**GET** `/api/products/{spuId}/dimensions`

**路径参数**：`spuId`（int64）SPU ID

**响应**

- **200** 维度列表（含各维度下选项），Body 为 `SpecDimensionWithOptions[]`（即 SpecDimension + `options: SpecOption[]`）
- **404** SPU 不存在，返回 `Error`

---

### 4.3 为某维度添加可选项

**POST** `/api/products/{spuId}/dimensions/{dimensionId}/options`

**路径参数**：`spuId`（int64）、`dimensionId`（int64）

**请求体**（application/json）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| optionValue | string | 是 | 选项值，同维度内唯一 |
| sortOrder | int | 否 | 排序 |
| image | string | 否 | 图片 URL，仅当所属维度 affectsAppearance 时可有值 |

**响应**

- **201** 创建成功，返回 `SpecOption`（id, specDimensionId, optionValue, sortOrder, image）
- **404** SPU 或维度不存在，返回 `Error`
- **400** 业务校验失败（如同维度内选项值重复），返回 `Error`

---

## 五、SKU

### 5.1 在 SPU 下创建 SKU

**POST** `/api/products/{spuId}/skus`

选齐必填维度 Option 并设置价格。每个必填维度恰好选一个 Option ID。

**路径参数**：`spuId`（int64）SPU ID

**请求体**（application/json）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| specOptionIds | int64[] | 是 | 每个必填维度选一 Option 的 ID，顺序不限 |
| priceCents | int64 | 是 | 价格（分），≥0 |
| displayName | string | 否 | 规格展示名 |

**响应**

- **201** 创建成功，返回 `Sku`（id, spuId, priceCents, displayName, specValues）
- **404** SPU 不存在，返回 `Error`
- **400** 业务校验失败（未选齐必填维度、Option 不属于该 SPU、价格不能为负等），返回 `Error`

---

### 5.2 按 SPU 查该 SPU 下所有 SKU

**GET** `/api/products/{spuId}/skus`

**路径参数**：`spuId`（int64）SPU ID

**响应**

- **200** SKU 列表，Body 为 `Sku[]`
- **404** SPU 不存在，返回 `Error`

---

### 5.3 按 SPU 与 SKU ID 查 SKU 详情

**GET** `/api/products/{spuId}/skus/{skuId}`

**路径参数**：`spuId`（int64）、`skuId`（int64）

**响应**

- **200** SKU 详情，Body 为 `Sku`
- **404** SKU 不存在或不属于该 SPU，返回 `Error`

---

## 六、数据模型（Schema）

### Category

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int64 | 唯一标识 |
| parentId | int64 | 根类别为 null |
| name | string | 名称 |
| description | string | 描述，可选 |

### Product

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int64 | 唯一标识 |
| categoryId | int64 | 所属类别 |
| name | string | 名称 |
| description | string | 描述，可选 |

### SpecDimension

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int64 | 唯一标识 |
| spuId | int64 | 所属 SPU |
| name | string | 维度名称 |
| required | boolean | 创建 SKU 时是否必选 |
| sortOrder | int | 排序，可选 |
| affectsAppearance | boolean | 该维度的 Option 才允许填图片；同 SPU 下至多一个为 true |

### SpecOption

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int64 | 唯一标识 |
| specDimensionId | int64 | 所属维度 |
| optionValue | string | 选项值 |
| sortOrder | int | 排序，可选 |
| image | string | 图片 URL，可选（仅影响外观维度） |

### SpecDimensionWithOptions

在 SpecDimension 基础上增加 `options: SpecOption[]`，用于「查询某 SPU 下所有维度及选项」的响应。

### Sku

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int64 | 唯一标识 |
| spuId | int64 | 所属 SPU |
| priceCents | int64 | 价格（分），≥0 |
| displayName | string | 规格展示名，可选 |
| specValues | array | 所选 Option 展示信息，如 [{ dimensionName, optionValue }] |

### Error

| 字段 | 类型 | 说明 |
|------|------|------|
| message | string | 错误说明 |

---

## 七、错误码汇总

| 状态码 | 含义 |
|--------|------|
| 200 | 成功（查询/列表） |
| 201 | 创建成功 |
| 204 | 删除成功，无响应体 |
| 400 | 业务校验失败（如唯一性、叶子类目、必填维度、价格非负等） |
| 404 | 资源不存在（父类别、SPU、维度、SKU 等） |
| 422 | 业务校验失败（与 400 同义，如仅叶子可挂商品） |

---

*契约源文件：`catalog-api.yaml`。文档与契约不一致时以 YAML 为准。*
