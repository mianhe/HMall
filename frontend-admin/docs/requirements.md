# 前端功能需求

当前管理端以**展示与图片上传**为主：类目、商品、规格、SKU 一页全览；**商品详情页**支持查看商品信息并上传产品级/选项级展示图。其余增删改（类目、商品、维度、选项、SKU）由 MCP 完成。后端需求见 `docs/bounded-contexts/catalog/requirements.md`。

---

## 一、定位与范围

| 项目 | 说明 |
|------|------|
| 定位 | 管理后台「查看 + 商品图片上传」入口，与 MCP 配合使用 |
| 数据操作 | 读（GET）；**图片**：可在商品详情页上传/删除产品级展示图、选项级展示图；其余新建/修改/删除由 MCP 或后端 API 直接操作 |
| 技术栈 | Vue 3 + Vite + Vue Router；样式 Tailwind；请求 axios，开发时代理到后端 `http://localhost:8080` |

---

## 二、页面与路由

路由定义：`frontend-admin/src/router/index.js`。

| 路由 | 页面 | 功能 |
|------|------|------|
| `/` | HomePage | 入口；文案说明；跳转「查看 Catalog」 |
| `/catalog` | CatalogPage | 类目 + 商品 + 规格 + SKU 树形全览；**点击商品**进入该商品详情页；刷新、空态与错误提示 |
| `/products/:id` | ProductDetailPage | 商品详情：基础信息、维度与选项、产品级展示图、各选项展示图；**上传**产品级或选项级图片；删除展示图；返回 Catalog |

---

## 三、Catalog 页

### 3.1 功能

- 递归加载：根类目 → 每类目下子类目与商品 → 每商品下维度（含选项及展示图）、SKU。
- 树形展示（`CatalogTree` / `CatalogTreeNode`），只读。
- 刷新按钮：重新请求并渲染整棵树。
- 错误与空态：请求失败显示错误信息（含 5xx 重试与友好提示）；无数据时提示「暂无数据，请通过 MCP 添加类目与商品」。

### 3.2 使用的后端 API（Catalog 页：均为 GET）

| 用途 | 方法 | 说明 |
|------|------|------|
| 根类目 / 子类目 | GET /api/categories | 不传 `parentId` 查根类目，传则查该父类目下子类目 |
| 类目下商品 | GET /api/products | 必传 `categoryId` |
| 商品维度与选项 | GET /api/products/{spuId}/dimensions | 含各维度下选项及选项展示图 |
| 商品 SKU | GET /api/products/{spuId}/skus | 规格组合、价格、展示名等 |

封装位置：`frontend-admin/src/shared/api/catalog.js`。与 `docs/bounded-contexts/catalog/api.yaml` 一致。

### 3.3 与后端模型对齐

- 维度：无「影响外观」字段；任意维度下选项均可有展示图。
- 展示图：产品级与选项级均由后端返回，前端展示；在商品详情页可上传/删除。

---

## 四、商品详情页（ProductDetailPage）

### 4.1 进入方式

- 在 Catalog 页树形列表中**点击某个商品名称**，跳转到 `/products/:id`（该商品的 SPU id）。

### 4.2 页面展示

- **商品基础信息**：名称、描述、类目等（来自 GET /api/products/:id）。
- **规格维度与选项**：维度名称、是否必填、各选项值；每个选项下展示其**展示图列表**（来自 GET /api/products/:id/dimensions，options[].images）。
- **产品级展示图**：不关联选项的展示图列表（GET /api/products/:id/images），支持删除。
- 信息尽量丰富、结构清晰（如分区：基础信息 / 维度与选项 / 产品级图片 / 各选项图片）。

### 4.3 功能要求：图片上传

- **产品级图片**：在本页提供「上传产品级展示图」入口；用户选择文件后，先调用 **POST /api/files/upload** 上传文件得到 URL，再调用 **POST /api/products/:id/images** 传入 `imageUrl`（及可选 `sortOrder`）；上传成功后刷新产品级展示图列表。
- **选项级图片**：在每个选项旁提供「为该选项添加展示图」入口；用户选择文件后，先 **POST /api/files/upload** 得到 URL，再 **POST /api/products/:id/dimensions/:dimensionId/options/:optionId/images** 传入 `imageUrl`（及可选 `sortOrder`）；上传成功后刷新该选项的展示图列表或整页数据。
- **删除**：产品级展示图、选项级展示图均提供删除操作，调用对应 DELETE 接口，成功后刷新列表。
- 错误时展示后端返回的 `message`，不在前端写死文案。

---

## 五、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/catalog/requirements.md` | 后端需求与 .feature |
| `docs/bounded-contexts/catalog/api.yaml` | 后端 REST 契约 |
| `frontend-admin/docs/design-input.md` | 前端设计输入（FSD + Atomic）；含类目/商品 CRUD 等**未实现**的扩展设想，后续若做表单页可参考 |
