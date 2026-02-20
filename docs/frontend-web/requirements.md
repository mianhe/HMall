# 前端功能需求 — 消费者端（frontend-web）

消费者端面向终端用户，提供注册、登录和商城浏览功能。后端需求见 `docs/bounded-contexts/user/requirements.md`。

---

## 一、定位

| 端 | 职责 |
|----|------|
| **消费者端（frontend-web）** | 注册、登录、浏览商城、下单交易 |
| **后端** | 用户管理、认证、商品数据（Catalog）、订单（Order） |

---

## 二、页面结构

| 路由 | 页面 | 功能 | 后端 API |
|------|------|------|----------|
| `/` | HomePage | 首页：类目导航 + 商品展示（两层类目，参考 VMALL） | `GET /api/categories`、`GET /api/products` |
| `/my` | MyPage | 我的：用户信息块、收货地址入口、我的订单块（分块展示，Atomic Design） | — |
| `/cart` | CartPage | 购物车：列表、改数量、删除、全选、去结算（需登录） | `GET /api/cart`、`POST /api/cart/items`、`PUT /api/cart/items/{id}`、`DELETE /api/cart/items`、`POST /api/cart/checkout-preview` |
| `/products/:id` | ProductDetailPage | 商品详情（图廊、规格、价格、详情/参数）；「立即购买」与「加入购物车」 | `GET /api/products/{id}`、`/images`、`/dimensions`、`/skus`；`POST /api/cart/items` |
| `/checkout` | CheckoutPage | 结账页：支持立即购买单件或购物车勾选多件；选地址、提交订单；成功后清理已下单项 | `POST /api/orders`、`GET /api/users/{userId}/addresses`；购物车入口时 `POST /api/cart/checkout-preview`、`DELETE /api/cart/items` |
| `/addresses` | AddressPage | 收货地址管理：列表、新增、编辑、删除 | `GET /api/users/{userId}/addresses`、`POST / PUT / DELETE /api/users/{userId}/addresses/{id}` |
| `/orders` | OrderListPage | 订单列表：当前用户订单，支持 query 按状态筛选（待付款/待收货/待评价） | `GET /api/orders?userId=xxx` |
| `/orders/:id` | OrderDetailPage | 订单详情：订单信息、取消、模拟支付 | `GET /api/orders/{id}`、`POST /api/orders/{id}/cancel` |
| `/login` | LoginPage | 用户登录表单，成功后存 token 跳回首页 | `POST /api/login` |
| `/register` | RegisterPage | 用户注册表单，成功后自动登录跳回首页 | `POST /api/users` → `POST /api/login` |

### 2.1 首页：类目与商品导航

- **结构参考**：华为商城 VMALL 两层类目——顶部一级类目，悬停展开浮层：左侧二级类目列表，右侧该二级下的商品网格。
- **类目导航**：一级类目为根类目列表（`GET /api/categories` 不传 parentId）；悬停某一级时请求其子类目（`GET /api/categories?parentId=xxx`），在浮层左侧展示；点击/悬停二级类目时，右侧展示该类目下商品。
- **商品导航**：右侧网格展示当前选中二级类目下的商品（`GET /api/products?categoryId=xxx`），卡片含商品名称、主图（若有）或占位图，点击进入商品详情页。
- **契约**：`docs/bounded-contexts/catalog/api.yaml`（Category、Product）。

### 2.2 购物车页

- **入口**：顶栏「购物车」链接（已登录显示件数）；商品详情页「加入购物车」。
- **内容**：购物车项列表（SKU 名称、价格、数量、小计）；不可用 SKU 标灰并提示已下架；每项可改数量、删除；全选、已选件数与合计、「去结算」。
- **去结算**：跳转 `/checkout` 并携带勾选的 `cartItemIds`。
- **API**：`GET /api/cart`、`POST /api/cart/items`、`PUT /api/cart/items/{cartItemId}`、`DELETE /api/cart/items`（单项或 body 批量）；契约 `docs/bounded-contexts/cart/api.yaml`。请求头 `X-User-Id` 由前端从 JWT 解析注入。

### 2.3 商品详情页

- **结构参考**：华为商城 VMALL 商详页（不含促销、返点、关联推荐）。
- **面包屑**：首页 > 商品名称。
- **左侧**：产品图廊（主图 + 缩略图切换）；无图时占位。
- **右侧**：商品名称、价格（随规格选择变化）、规格维度（颜色、版本等）按钮选择、已选规格摘要。
- **下方 Tab**：详情（商品描述）、参数（暂无则占位）。
- **操作**：「立即购买」跳结账页；「加入购物车」调用 `POST /api/cart/items`，成功后提示并更新顶栏购物车件数。
- **API**：`GET /api/products/{id}`、`GET /api/products/{spuId}/images`、`GET /api/products/{spuId}/dimensions`、`GET /api/products/{spuId}/skus`。

### 2.4 结账页

- **入口**：商品详情页「立即购买」（单件）；或购物车「去结算」（勾选 `cartItemIds`）。
- **内容**：商品摘要（单件或购物车结算预览多件）、合计；收货地址：可从已保存地址中选择，或新增填写；链接「管理收货地址」跳转 `/addresses`。
- **购物车入口**：进入时调用 `POST /api/cart/checkout-preview` 获取选中项摘要与总价；提交订单成功后调用 `DELETE /api/cart/items` 清理已下单项。
- **提交**：调用 `POST /api/orders`，body 为 `OrderCreate`（userId 从 JWT 解析，items、shippingAddress）。
- **成功**：跳转订单详情 `/orders/{orderId}`；失败：展示后端返回的 message。
- **契约**：`docs/bounded-contexts/order/api.yaml`（OrderCreate、ShippingAddress）；`docs/bounded-contexts/cart/api.yaml`（checkout-preview、delete items）。

### 2.5 订单列表

- **入口**：顶栏「我的订单」链接；需登录。
- **内容**：分页展示当前用户订单（orderId、status、totalAmountCents、items 摘要、收货地址）。
- **API**：`GET /api/orders?userId=xxx&page=0&size=20`，userId 从 JWT 解析。
- **点击**：进入订单详情 `/orders/{id}`。

### 2.6 订单详情

- **内容**：订单完整信息（明细、收货地址、status）；状态文案映射（如 PENDING_PAYMENT → 待支付）。
- **操作**：待支付时可「取消订单」`POST /api/orders/{id}/cancel`；可「模拟支付」—— 纯前端 Mock，点击后 toast「支付成功（模拟）」，跳转订单列表。
- **API**：`GET /api/orders/{id}`。

---

## 三、已实现功能

### 3.1 注册

- 用户输入用户名、密码、确认密码
- 前端校验：确认密码与密码一致（唯一的前端校验）
- 调用 `POST /api/users` 创建用户
- 成功后自动调用 `POST /api/login` 登录，获取 token 存入 `localStorage`，跳转首页
- 失败：展示后端返回的错误信息（如「用户名已存在」）
- 提供「已有账号？去登录」链接

### 3.2 登录

- 用户输入用户名、密码，调用 `POST /api/login`
- 成功：获取 JWT token，存入 `localStorage`，跳转首页
- 失败：展示后端返回的错误信息
- 提供「没有账号？去注册」链接

### 3.3 首页

- 顶部主导航下为**一级类目**（根类目），悬停某一级展开浮层。
- 浮层**左侧**为二级类目列表，**右侧**为当前选中二级类目下的商品网格；点击商品进入详情页。
- 欢迎文案与登录/注册入口保留在首屏或顶栏。

### 3.4 商品详情页

- 面包屑、图廊（主图 + 缩略图）、规格维度选择（选齐后显示对应 SKU 价格与已选摘要）、详情/参数 Tab；由首页或列表进入。不包含促销、返点、关联推荐。
- 「立即购买」与「加入购物车」按钮：选中规格后可用；加入购物车成功后提示并触发顶栏件数更新。

### 3.5 购物车页

- 购物车列表（名称、价格、数量、小计）、不可用项标灰；改数量、删除、全选、去结算；空车时提示并跳首页。

### 3.6 结账页

- 支持单件（立即购买）或多件（购物车勾选）；商品摘要、收货地址表单、提交订单；成功后跳转订单详情，购物车入口时清理已下单项。未登录跳转登录页并保存 checkout 数据至 sessionStorage。

### 3.7 订单列表

- 按 userId 分页展示订单；顶栏入口「我的订单」。未登录跳转登录页。

### 3.8 订单详情

- 订单信息、取消、模拟支付（前端 Mock）。待支付时可取消；模拟支付后 toast 提示并跳转订单列表。

---

## 四、技术栈

- **框架**：Vue 3 + Vite + Vue Router
- **样式**：Tailwind CSS（VMALL 风格）
- **请求**：axios，baseURL 开发时用 Vite 代理；`/api/cart` 代理到 Cart 服务 `http://localhost:8087`，其余 `/api` 到 BFF `http://localhost:8085`
- **端口**：开发环境 `5174`

---

## 五、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/user/requirements.md` | 后端 User BC 需求与 .feature |
| `docs/bounded-contexts/user/api.yaml` | User BC API 契约 |
| `docs/bounded-contexts/catalog/api.yaml` | Catalog BC API 契约（类目、商品） |
| `docs/bounded-contexts/order/api.yaml` | Order BC API 契约（订单创建、查询、取消） |
| `docs/bounded-contexts/cart/api.yaml` | Cart BC API 契约（购物车、结算预览） |
| `docs/frontend-admin/requirements.md` | 管理后台需求 |
