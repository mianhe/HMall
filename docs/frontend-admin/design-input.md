# 前端设计输入 — 管理后台（frontend-admin）

**本文档作用**：描述该前端的**页面结构、组件分层与设计风格**，与 requirements（功能与 API）互补。用于新页面或大改结构时保持实现一致；详细约定见 `docs/design-principles.md` 第三节与「Design Input 使用」。

> 管理后台以**展示为主**，增删改由 MCP 完成。单页分层展示类目 → 商品 → SKU。

---

## 一、FSD 输入（当前范围）

### 1.1 实体（entities）

| 实体 | 说明 |
|------|------|
| Category | 类目：id、parentId、name、description |
| Product | 商品：id、categoryId、name、description |
| SpecDimension | 规格维度：名称、选项 |
| Sku | SKU：规格组合、价格、展示名 |

### 1.2 页面/路由（pages）

| 路由 | 页面说明 |
|------|----------|
| `/` | 首页：入口，导航到 Catalog 浏览 |
| `/catalog` | Catalog 一页全览：类目 → 商品（含 dimension）→ SKU 分层展示 |

### 1.3 功能（features）

| 功能 | 说明 | 对应页面/操作 |
|------|------|---------------|
| 浏览 Catalog | 一页看全：类目、商品、SKU 分层展示 | `/catalog` |
| 展开/折叠 | 各级可展开或折叠 | 树形控件 |
| 刷新 | 重新拉取数据 | 刷新按钮 |

### 1.4 共享（shared）

- 请求后端：`/api/categories`、`/api/products` 及 product 的 dimensions、skus
- 通用 UI：加载中、错误提示、树形行、缩进

---

## 二、Atomic Design 输入（当前范围）

### 2.1 Atoms（原子）

| 组件 | 说明 |
|------|------|
| Spinner | 加载中 |
| Badge | 小徽标（可选，如价格） |

### 2.2 Molecules（分子）

| 组件 | 说明 |
|------|------|
| TreeNode | 树形行：可展开/折叠，缩进表示层级 |
| TreeNodeRow | 单行内容：类目名、商品名+维度、SKU 规格与价格 |

### 2.3 Organisms（有机体）

| 组件 | 说明 |
|------|------|
| AppHeader | 顶栏：Logo/标题 + 导航链接 |
| CatalogTree | 完整 Catalog 树：类目 → 商品（含 dimension）→ SKU，支持展开/折叠、刷新 |

### 2.4 页面结构

| 页面 | 结构 |
|------|------|
| 首页 | AppHeader + 简短说明 + 链接（去 Catalog） |
| Catalog | AppHeader + CatalogTree（树形分层展示 + 刷新） |

---

## 三、Catalog 树展示细节

| 层级 | 展示内容 |
|------|----------|
| 类目 | 名称、描述；可展开看子类目或商品 |
| 商品 | 名称、描述、**规格维度**（如「颜色、容量」） |
| SKU | 规格组合、价格、展示名 |

类目递归展示；商品仅挂在叶子类目下；商品行内注明其 dimension 名称。

---

## 四、技术栈与约定

- **框架**：Vue 3 + Vite + Vue Router
- **样式**：Tailwind CSS
- **请求**：axios，baseURL 开发时用 Vite 代理到 `http://localhost:8080`
- **与后端对齐**：请求/响应结构与 `docs/bounded-contexts/catalog/api.yaml` 一致

---

## 五、设计风格（参考华为商城 VMALL）

参考 [华为商城 VMALL](https://www.vmall.com/)：简洁、干净、现代，以白色与浅灰为主，红色作强调。

| 元素 | 规范 |
|------|------|
| **主色** | 华为红 `#C7000B`，用于顶栏、按钮、链接、强调 |
| **主色 hover** | `#A00009` 深红 |
| **背景** | 主区域白色 `#FFFFFF`；页面底 `#F5F5F5` |
| **文字** | 标题深灰 `#333333`；正文 `#666666`；辅助 `#999999` |
| **边框** | `#E0E0E0` |
| **布局** | 居中、max-width 限制；留白充足；卡片圆角 `rounded-lg` |
| **顶栏** | 红底白字；Logo/标题左；导航右 |
| **按钮** | 主按钮红底白字；次要为白底灰框 |

Tailwind 已配置 `vmall-red`、`vmall-red-hover`、`vmall-gray-bg` 等，实现时优先使用。
