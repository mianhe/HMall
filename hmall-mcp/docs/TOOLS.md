# Catalog MCP Tools 说明

本文档描述 HMall MCP Server 提供的 **Catalog 相关 tools**，供用户或其它 Agent 查询与操作：类目、商品（SPU）、规格维度/选项、SKU、展示图。

- **面向**：使用 MCP Client（如 Cursor、Claude Desktop）的用户与 AI Agent。
- **后端**：调用 HMall `catalog-service` 的 REST API（默认 `http://localhost:8080/api`）。

---

## 工具总览（7 个，均已实现）

| 工具名 | 用途 |
|--------|------|
| `catalog_categories` | 类目：列表 / 树 / 详情 / 创建 / 修改 / 删除 |
| `catalog_products` | 商品(SPU)：列表或搜索 / 详情(基础或完整) / 创建 / 修改 / 删除 |
| `catalog_dimensions` | 规格维度与选项：列表 / 添加维度 / 添加选项 / 删除选项 |
| `catalog_skus` | SKU：列表 / 创建 / 修改 / 删除 |
| `catalog_upload_image` | 上传本地图片，返回可访问 URL |
| `catalog_product_images` | 产品级展示图：列表 / 添加(支持 URL 或本地路径) / 删除 |
| `catalog_option_images` | 选项级展示图：列表 / 添加(支持 URL 或本地路径) / 删除 |

共 **7 个 tool**，没有未实现的项。有时说「8 个」是指把「上传」单独算一个能力点（上传已包含在 `catalog_upload_image` 里）。

---

## 1. catalog_categories

类目（分类）的查询与维护。支持树形结构：根类目无 `parentId`，子类目传 `parentId`。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `tree` \| `get` \| `create` \| `update` \| `delete` |
| `parentId` | number | 否 | **list** 时：不传查根类目，传则查该父类目下子类目 |
| `categoryId` | number | action 为 get/update/delete 时必填 | 类目 ID |
| `name` | string | create/update 时必填 | 类目名称 |
| `description` | string | 否 | 类目描述 |

**示例**

- 查根类目：`action=list`
- 查某类目下子类目：`action=list`, `parentId=2`
- 查完整类目树：`action=tree`
- 查类目详情：`action=get`, `categoryId=2`
- 创建根类目：`action=create`, `name=数码`
- 创建子类目：`action=create`, `name=手机`, `parentId=1`
- 修改：`action=update`, `categoryId=2`, `name=新名称`
- 删除：`action=delete`, `categoryId=2`

---

## 2. catalog_products

商品（SPU）的查询与维护。商品挂在叶子类目下。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `get` \| `create` \| `update` \| `delete` |
| `categoryId` | number | 否 | **list** 时：按类目过滤；不传则与 keyword 二选一或返回全部（依实现） |
| `keyword` | string | 否 | **list** 时：按关键词搜索商品名称（跨类目模糊匹配） |
| `productId` | number | get/update/delete 时必填 | 商品 ID |
| `detail` | string | 否 | **get** 时：`basic` 仅基础信息，`full` 含规格维度、选项及 SKU；默认可为 basic |
| `name` | string | create/update 时必填 | 商品名称 |
| `description` | string | 否 | 商品描述 |

**示例**

- 按类目查列表：`action=list`, `categoryId=3`
- 按关键词搜索：`action=list`, `keyword=手机`
- 查商品详情（含规格与 SKU）：`action=get`, `productId=1`, `detail=full`
- 创建：`action=create`, `categoryId=3`, `name=iPhone 16`
- 修改：`action=update`, `productId=1`, `name=新名称`
- 删除：`action=delete`, `productId=1`

---

## 3. catalog_dimensions

某 SPU 的规格维度及选项（如「颜色」「容量」）。创建 SKU 时需指定各维度的选项 ID。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `add_dimension` \| `add_option` \| `delete_option` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `dimensionId` | number | add_option/delete_option 时必填 | 维度 ID |
| `optionId` | number | delete_option 时必填 | 选项 ID |
| `name` | string | add_dimension 时必填 | 维度名称，如「颜色」「容量」 |
| `required` | boolean | add_dimension 时必填 | 创建 SKU 时是否必选该维度 |
| `optionValue` | string | add_option 时必填 | 选项值，如「128G」「黑色」 |
| `sortOrder` | number | 否 | add_option 时排序（越小越靠前） |

**示例**

- 查维度与选项：`action=list`, `spuId=1`
- 添加维度：`action=add_dimension`, `spuId=1`, `name=容量`, `required=true`
- 为维度添加选项：`action=add_option`, `spuId=1`, `dimensionId=1`, `optionValue=128G`
- 删除选项：`action=delete_option`, `spuId=1`, `dimensionId=1`, `optionId=2`

---

## 4. catalog_skus

某 SPU 下 SKU 的查询与维护。SKU 由各规格维度的选项组合 + 价格（及可选展示名）构成。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `create` \| `update` \| `delete` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `skuId` | number | update/delete 时必填 | SKU ID |
| `specOptionIds` | number[] | create 时必填 | 各必填维度所选选项 ID 列表 |
| `priceCents` | number | create 时必填；update 时可选 | 价格，单位：分 |
| `displayName` | string | 否 | 展示名 |

**示例**

- 查某 SPU 下所有 SKU：`action=list`, `spuId=1`
- 创建 SKU：`action=create`, `spuId=1`, `specOptionIds=[1,3]`, `priceCents=599900`
- 改价格：`action=update`, `spuId=1`, `skuId=5`, `priceCents=549900`
- 删除：`action=delete`, `spuId=1`, `skuId=5`

---

## 5. catalog_upload_image

上传本地图片文件到服务器，返回可访问的 URL。该 URL 可用于「添加展示图」等接口的 `imageUrl` 参数。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `localPath` | string | 是 | 本地图片文件的绝对路径 |

**示例**

- `localPath=/Users/me/photo.png` → 返回 `{ url: "https://..." }` 或文本中包含 URL。

---

## 6. catalog_product_images

产品级展示图（不绑定到具体规格选项）。添加时支持直接传 URL 或本地路径（传本地路径时内部先上传再关联）。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `add` \| `delete` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `imageUrl` | string | add 时与 localPath 二选一 | 图片 URL（可来自 catalog_upload_image） |
| `localPath` | string | add 时与 imageUrl 二选一 | 本地图片路径（内部先上传再添加） |
| `sortOrder` | number | 否 | 排序，越小越靠前 |
| `imageId` | number | delete 时必填 | 展示图 ID |

**示例**

- 查产品级图：`action=list`, `spuId=1`
- 用 URL 添加：`action=add`, `spuId=1`, `imageUrl=https://...`
- 用本地文件添加：`action=add`, `spuId=1`, `localPath=/path/to/img.jpg`
- 删除：`action=delete`, `spuId=1`, `imageId=2`

---

## 7. catalog_option_images

为某规格维度的某选项绑定展示图（如「颜色：黑色」对应一张图）。添加时支持 URL 或本地路径。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `add` \| `delete` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `dimensionId` | number | 是 | 维度 ID |
| `optionId` | number | 是 | 选项 ID |
| `imageUrl` | string | add 时与 localPath 二选一 | 图片 URL |
| `localPath` | string | add 时与 imageUrl 二选一 | 本地图片路径 |
| `sortOrder` | number | 否 | 排序 |
| `imageId` | number | delete 时必填 | 展示图 ID |

**示例**

- 查某选项的图：`action=list`, `spuId=1`, `dimensionId=1`, `optionId=2`
- 添加：`action=add`, `spuId=1`, `dimensionId=1`, `optionId=2`, `localPath=/path/to/black.jpg`
- 删除：`action=delete`, `spuId=1`, `dimensionId=1`, `optionId=2`, `imageId=1`

---

## 典型流程简述

1. **类目**：`catalog_categories` 建树（根 → 子 → 叶子）。
2. **商品**：在叶子类目下 `catalog_products` create；需要规格时用 `catalog_dimensions` 添加维度和选项。
3. **SKU**：用 `catalog_dimensions` list 拿到选项 ID，再用 `catalog_skus` create 组合选项与价格。
4. **展示图**：用 `catalog_upload_image` 得到 URL，或用 `catalog_product_images` / `catalog_option_images` 的 add + `localPath` 一步完成；list/delete 按需调用。

---

## 环境与依赖

- 需 **catalog-service** 已启动，默认 API 基地址：`http://localhost:8080/api`，可通过环境变量 `HMALL_API_BASE` 覆盖。
- **上传图像**（`catalog_upload_image` 及展示图 add 的 localPath）：依赖后端 **MinIO**。须在 catalog-service 中配置 `minio.enabled=true`（默认已为 true），并确保 MinIO 服务（如 `http://localhost:9000`）已启动，否则 `/api/files/upload` 不会注册或会报错。

---

*当前实现已与本文档一致：上述 7 个 tool 均已实现。*
