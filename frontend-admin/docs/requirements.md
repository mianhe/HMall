# 前端功能需求

前端有哪些页面、各做什么、对应哪些后端 API。后端需求见 `docs/bounded-contexts/catalog/requirements.md`。

---

## 一、页面一览

### 入口

| 路由 | 页面 | 功能 | 后端 API |
|------|------|------|----------|
| `/` | HomePage | 入口；导航到类别管理、商品列表 | — |

### 类目管理

| 路由 | 页面 | 功能 | 后端 API |
|------|------|------|----------|
| `/categories` | CategoryListPage | 当前层级类目列表；点类目进子层级，入口「新建类别」 | GET /api/categories |
| `/categories/new` | CategoryFormPage | 新建类目表单（名称、描述、父类目可选）；提交后回列表 | POST /api/categories |

### 商品管理

| 路由 | 页面 | 功能 | 后端 API |
|------|------|------|----------|
| `/products` | ProductListPage | 按类目展示商品列表（需 `categoryId`）；入口「新建商品」，点商品进详情 | GET /api/products |
| `/products/new` | ProductFormPage | 新建商品表单（名称、描述，类目由 `categoryId` 定）；提交后回列表 | POST /api/products |
| `/products/:id` | ProductDetailPage | 商品详情（名称、描述等）；**规格配置**（维度列表、新建维度、为维度添加选项）；**SKU 列表**（列表、新建 SKU）；可返回列表 | GET /api/products/:id、GET/POST dimensions、GET/POST skus |

路由定义：`frontend-admin/src/router/index.js`。

---

## 二、SPU 配置与 SKU 管理（已实现于商品详情页）

以下能力已做在 **商品详情页** `/products/:id` 内两个区块：**规格配置** 和 **SKU 列表**。

### 规格配置（区块）

| 功能 | 后端 API |
|------|----------|
| 展示该 SPU 下维度列表（含各维度下的选项及图片） | GET /api/products/{spuId}/dimensions |
| 新建规格维度（名称、是否必填、是否影响外观） | POST /api/products/{spuId}/dimensions |
| 为某维度添加可选项（选项值、排序、影响外观时可填图片链接） | POST /api/products/{spuId}/dimensions/{dimensionId}/options |

### SKU 管理（区块）

| 功能 | 后端 API |
|------|----------|
| 展示该 SPU 下所有 SKU（规格组合、价格、展示名） | GET /api/products/{spuId}/skus |
| 新建 SKU：各维度选一选项、填价格、可选展示名 | POST /api/products/{spuId}/skus |

后端契约：`spec-dimension.feature`、`sku.feature`，及 `catalog-api.yaml` 对应 path。

---

## 三、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/catalog/requirements.md` | 后端需求与 .feature |
| `frontend-admin/docs/design-input.md` | 前端设计输入（FSD + Atomic、组件结构） |
