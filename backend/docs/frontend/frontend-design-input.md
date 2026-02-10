# 前端设计输入（FSD + Atomic）

> 用于按 FSD 与 Atomic Design 实现前端的输入文档。当前范围：商品限定上下文（类别 + 商品）。  
> **你无需事先填这份文档**——我会根据后端与需求先写好一版；需要你做选择时，我会用简单问题引导你，并帮你把答案写进这里。

---

## 一、我会怎么引导你

1. **当前这一版**：我已根据你后端的「类别 + 商品」能力和需求，直接写出下面的页面、实体、功能、组件清单。你**不用改**也能开始做前端；若你有偏好（例如「类别想用树形展示」），你说一句，我帮你改文档并实现。
2. **以后加新页面/新功能时**：我会用**简单选择题**问你，例如：
   - 「新建类别你希望是：A 单独一页 / B 当前页弹窗」
   - 「类别列表希望：A 树形折叠 / B 平铺列表」
   你选 A 或 B（或说「随便」），我帮你更新本文档并实现。
3. **你只需**：用自然语言说你的偏好或「按你的来」；不需要懂 FSD/Atomic 的术语。

---

## 二、FSD 输入（当前范围）

以下由后端能力与需求推导得出，可直接用于 FSD 分层。

### 2.1 实体（entities）

与后端一致，仅两个：

| 实体 | 说明 |
|------|------|
| Category | 类别，有 id、parentId、name、description；根类别 parentId 为 null |
| Product | 商品，有 id、categoryId、name、description |

### 2.2 页面/路由（pages）

| 路由 | 页面说明 |
|------|----------|
| `/` | 首页：入口，可跳转到「类别管理」或「商品列表」 |
| `/categories` | 类别列表：展示根类别或当前父类别下的子类别，可进入子类别、可创建根/子类别 |
| `/categories/new` | 新建类别：表单（名称、描述、可选父类别），提交后回到类别列表 |
| `/categories?parentId=:id` | 与 `/categories` 同一页，通过查询参数表示「当前在看某类别下的子类别」 |
| `/products` | 商品列表：需带 `categoryId`（某类别下的商品），可创建商品 |
| `/products/new?categoryId=:id` | 新建商品：表单（名称、描述，类别固定为当前类别），提交后回到该类别商品列表 |
| `/products/:id` | 商品详情：只读展示一个商品 |

### 2.3 功能（features）

| 功能 | 说明 | 对应页面/操作 |
|------|------|----------------|
| 查看类别树/列表 | 根类别列表、或某类别下子类别列表 | `/categories` |
| 创建类别 | 根级或选父类别后创建子类别 | `/categories/new` 或列表页「新建」 |
| 查看商品列表 | 按类别查看商品 | `/products?categoryId=...` |
| 创建商品 | 在叶子类别下创建商品 | `/products/new?categoryId=...` |
| 查看商品详情 | 按 ID 查看单个商品 | `/products/:id` |

### 2.4 共享（shared）

- 请求后端：`/api/categories`、`/api/products` 的封装（axios 或 fetch）
- 通用 UI：按钮、输入框、标签、链接、加载中、错误提示（见下 Atomic）

---

## 三、Atomic Design 输入（当前范围）

按「原子 → 分子 → 有机体」列出，实现时按需增删。

### 3.1 Atoms（原子）

| 组件 | 说明 |
|------|------|
| Button | 按钮，支持主要/次要样式 |
| Input | 文本输入框 |
| Label | 表单标签 |
| Link | 链接（站内路由跳转） |
| Badge | 小徽标/标签（可选，如「叶子」） |
| Spinner | 加载中 |

### 3.2 Molecules（分子）

| 组件 | 说明 |
|------|------|
| FormField | Label + Input 组合，可带错误提示 |
| SearchBar | 输入框 + 搜索按钮（若当前不做搜索可后加） |
| Card | 卡片：标题 + 内容区域，用于类别卡片或商品卡片 |
| DataRow | 表格行或列表行：若干列（用于商品列表、类别列表） |

### 3.3 Organisms（有机体）

| 组件 | 说明 |
|------|------|
| AppHeader | 顶栏：Logo/标题 + 导航链接（首页、类别、商品） |
| CategoryList | 类别列表：展示当前层级类别，支持「进入子类别」「新建类别」 |
| CategoryForm | 创建/编辑类别表单：name、description、parentId（可选，下拉或选择） |
| ProductList | 商品列表：表格或卡片列表，支持「新建商品」 |
| ProductForm | 创建商品表单：name、description，categoryId 由路由带过来 |
| ProductDetail | 商品详情展示块 |

### 3.4 页面结构（每页由哪些 organism 组成）

| 页面 | 结构 |
|------|------|
| 首页 | AppHeader + 简短说明 + 链接（去类别列表、去商品列表需选类别，可先链到类别） |
| 类别列表 | AppHeader + CategoryList（含「新建类别」入口） |
| 新建类别 | AppHeader + CategoryForm |
| 商品列表 | AppHeader + 当前类别信息 + ProductList（含「新建商品」入口） |
| 新建商品 | AppHeader + ProductForm |
| 商品详情 | AppHeader + ProductDetail |

---

## 四、技术栈与约定（实现时采用）

- **框架**：Vue 3 + Vite + Vue Router
- **样式**：Tailwind CSS
- **请求**：axios，baseURL 开发时用 Vite 代理到 `http://localhost:8080`
- **与后端对齐**：请求/响应结构与 `backend/docs/catalog/catalog-api.yaml` 一致（Category、CategoryCreate、Product、ProductCreate、Error）

---

## 五、你需要做选择时我会怎么问

以后加功能或改交互时，我会用类似下面的方式问你，你只需回复选项或一句话：

- 「类别列表你更喜欢：**A** 树形（可折叠展开） **B** 平铺列表（一层级一页）」  
- 「新建类别：**A** 单独一页 **B** 在列表页用弹窗/侧栏」  
- 「商品列表：**A** 表格 **B** 卡片网格」  
- 「没有数据时：**A** 显示空状态文案 **B** 不显示列表区域」

你把答案告诉我，我会更新本文档并按你的选择实现；若你说「你定」，我就按上面当前文档的默认来。
