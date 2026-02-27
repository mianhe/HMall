# 界面规格 — 消费者端（frontend/web）

消费者端面向终端用户，提供注册、登录、商城浏览、下单交易与 AI 对话式购物体验。后端需求见各 BC 的 `requirements.md`。

---

## 一、定位与范围

| 端 | 职责 |
|----|------|
| **消费者端（frontend/web）** | 注册、登录、浏览商城、下单交易、AI 智能助手 |
| **后端** | 用户管理、认证、商品（Catalog）、购物车（Cart）、订单（Order）、AI（Smart Interaction） |

---

## 二、页面规格

### 2.1 页面总览

| 路由 | 页面 | 功能 | 后端 API |
|------|------|------|----------|
| `/` | HomePage | 首页：类目导航 + 商品展示（两层类目，参考 VMALL） | `GET /api/categories`、`GET /api/products` |
| `/products/:id` | ProductDetailPage | 商品详情（图廊、规格、价格、详情/参数）；「立即购买」与「加入购物车」 | `GET /api/products/{id}`、`/dimensions`、`/skus`；`POST /api/cart/items` |
| `/cart` | CartPage | 购物车：列表、改数量、删除、全选、去结算 | `GET /api/cart`、`POST/PUT/DELETE /api/cart/items`、`POST /api/cart/checkout-preview` |
| `/checkout` | CheckoutPage | 结账：单件或购物车多件；选地址、提交订单 | `POST /api/orders`、`GET /api/users/{userId}/addresses`、Cart API |
| `/addresses` | AddressPage | 收货地址管理：列表、新增、编辑、删除 | `GET/POST/PUT/DELETE /api/users/{userId}/addresses/{id}` |
| `/orders` | OrderListPage | 订单列表：按状态筛选（待付款/待收货/待评价） | `GET /api/orders?userId=xxx` |
| `/orders/:id` | OrderDetailPage | 订单详情：订单信息、取消、模拟支付 | `GET /api/orders/{id}`、`POST /api/orders/{id}/cancel` |
| `/my` | MyPage | 我的：用户信息块、收货地址入口、我的订单块 | — |
| `/login` | LoginPage | 登录表单，成功后存 token 跳回首页 | `POST /api/login` |
| `/register` | RegisterPage | 注册表单，成功后自动登录跳回首页 | `POST /api/users` → `POST /api/login` |

### 2.2 首页

- **结构参考**：华为商城 VMALL 两层类目——顶部一级类目，悬停展开浮层：左侧二级类目列表，右侧该二级下的商品网格。
- **类目导航**：一级类目为根类目列表（`GET /api/categories` 不传 parentId）；悬停某一级时请求其子类目，浮层左侧展示；点击/悬停二级类目时右侧展示该类目下商品。
- **商品导航**：右侧网格展示商品卡片（名称、`coverImageUrl` 主图，无图时占位），点击进入商品详情页。
- **契约**：`docs/bounded-contexts/catalog/api.yaml`。

### 2.3 商品详情页

- **结构参考**：华为商城 VMALL 商详页（不含促销、返点、关联推荐）。
- **面包屑**：首页 > 商品名称。
- **左侧**：产品图廊（主图 + 缩略图切换）；`defaultDisplayImages` 作为默认图廊；选中某规格后优先展示该选项的图。
- **右侧**：商品名称、价格（随规格选择变化）、规格维度按钮选择、已选规格摘要。
- **可选服务**：实体商品（PHYSICAL）详情页在规格选择区下方展示「可选服务」模块，通过 `GET /api/products/{spuId}/available-services` 获取；每项显示服务名称、服务类别、有效期、价格（来自 SKU）。
- **下方 Tab**：详情（商品描述）、参数（暂无则占位）。
- **操作**：「立即购买」跳结账页；「加入购物车」成功后提示并更新顶栏购物车件数。

### 2.4 购物车页

- **入口**：顶栏「购物车」链接（已登录显示件数）；商品详情页「加入购物车」。
- **内容**：购物车项列表（SKU 名称、价格、数量、小计）；不可用 SKU 标灰并提示已下架；每项可改数量、删除；全选、已选件数与合计、「去结算」。
- **去结算**：跳转 `/checkout` 并携带勾选的 `cartItemIds`。
- **API**：`GET /api/cart`、`POST/PUT/DELETE /api/cart/items`。请求头 `X-User-Id` 由前端从 JWT 解析注入。
- **契约**：`docs/bounded-contexts/cart/api.yaml`。

### 2.5 结账页

- **入口**：商品详情页「立即购买」（单件）；或购物车「去结算」（勾选 `cartItemIds`）。
- **内容**：商品摘要与合计；收货地址选择或新增；链接「管理收货地址」跳转 `/addresses`。
- **购物车入口**：进入时调用 `POST /api/cart/checkout-preview` 获取摘要与总价；提交成功后清理已下单项。
- **提交**：`POST /api/orders`，成功跳转订单详情；失败展示后端 message。
- **契约**：`docs/bounded-contexts/order/api.yaml`、`docs/bounded-contexts/cart/api.yaml`。

### 2.6 订单列表

- **入口**：顶栏「我的订单」；需登录。
- **内容**：分页展示（orderId、status、totalAmountCents、items 摘要、收货地址）。
- **API**：`GET /api/orders?userId=xxx&page=0&size=20`。

### 2.7 订单详情

- **内容**：订单完整信息（明细、收货地址、状态文案映射）。
- **操作**：待支付时可「取消订单」；可「模拟支付」（纯前端 Mock，toast 提示后跳转订单列表）。

### 2.8 我的页

- **用户信息块**：头像、用户名；收货地址入口 `>`；退出登录。
- **我的订单块**：标题「我的订单」+ 右侧「全部订单 >」；快捷入口：待付款、待收货、待评价。
- 块与块之间留白；块内元素对齐、间距统一。

### 2.9 登录与注册

- **注册**：用户名、密码、确认密码；前端校验确认密码一致；`POST /api/users` → 自动 `POST /api/login` → 存 token → 跳首页。
- **登录**：用户名、密码；`POST /api/login` → 存 JWT token → 跳首页。
- 互相提供「去登录」/「去注册」链接。

### 2.10 AI 智能助手

消费者端接入 Smart Interaction，提供对话式购物体验。

| 功能 | 说明 | API |
|------|------|-----|
| 呼出/关闭 | 右下角浮动按钮，或 Ctrl+K / Cmd+K | — |
| 流式对话 | SSE 流式接收 LLM 回复 | `POST /api/ai/chat` (SSE) |
| Tool Call 展示 | 折叠卡片展示工具名、参数、状态、结果 | — |
| Markdown 渲染 | marked + DOMPurify | — |
| 自动匹配模式 | 消费者端默认使用自动匹配，不显示 Skill 选择器 | — |

后端需求见 `docs/bounded-contexts/smart-interaction/requirements.md`。

---

## 三、组件结构

### 3.1 原子（Atoms）

| 组件 | 说明 |
|------|------|
| BlockTitle | 块标题：粗体、深色 |
| NavArrow | 右侧箭头 `>`，表示可点击 |
| IconPlaceholder | 占位图标：统一尺寸圆/方容器 |

### 3.2 分子（Molecules）

| 组件 | 说明 |
|------|------|
| IconLabel | 图标 + 文案（如待收货） |
| NavRow | 单行导航：左侧文案 + 右侧箭头 |
| MetricRow | 数值 + 单位/标签，可选右侧箭头 |

### 3.3 有机体（Organisms）

| 组件 | 说明 |
|------|------|
| AppHeader | 顶栏：Logo + 一级类目导航 + 购物车（件数）+ 我的/登录 |
| BlockCard | 白色卡片容器：圆角、边框、内边距 |
| UserProfileCard | 用户块：头像 + 用户名 + 收货地址入口 + 退出登录 |
| MyOrdersCard | 订单块：标题 + 全部订单 > + 快捷入口（待付款/待收货/待评价） |
| CategoryFlyout | 类目浮层：左侧二级类目列表 + 右侧商品网格 |
| ProductGallery | 商品图廊：主图 + 缩略图切换 |
| SpecSelector | 规格维度选择器：按钮组选择 + 已选摘要 |

### 3.4 页面结构

| 页面 | 结构 |
|------|------|
| 首页 | AppHeader（含 CategoryFlyout）+ 欢迎区 + 商品网格 |
| 商品详情 | AppHeader + 面包屑 + ProductGallery（左）+ 商品信息/SpecSelector（右）+ 详情/参数 Tab |
| 购物车 | AppHeader + 购物车列表（卡片行）+ 底部合计栏 + 去结算按钮 |
| 结账 | AppHeader + 商品摘要 + 地址选择/新增 + 提交按钮 |
| 订单列表 | AppHeader + 筛选 Tab + 订单卡片列表 |
| 订单详情 | AppHeader + 状态 + 商品明细 + 地址 + 操作按钮 |
| 我的 | AppHeader + 面包屑 + UserProfileCard + MyOrdersCard |
| 收货地址 | AppHeader + 地址卡片列表 + 新增/编辑表单 |
| 登录/注册 | AppHeader + 居中表单卡片 |

### 3.5 AI Chat 组件

```
src/shared/
├── api/ai.js                           # AI Chat API（SSE 流式）
├── composables/useAiChat.js            # 消费者版 composable（无 Skill 管理）
└── ui/ai-chat/
    ├── AiChatButton.vue                # 浮动按钮
    ├── AiChatPanel.vue                 # 对话面板（简化版）
    ├── AiChatInput.vue                 # 输入框
    ├── AiMessageList.vue               # 消息列表
    ├── AiMessageBubble.vue             # 消息气泡
    ├── AiToolCallGroup.vue             # 工具调用组
    └── AiToolCallCard.vue              # 单个工具调用卡片
```

---

## 四、视觉规范

参考 [华为商城 VMALL](https://www.vmall.com/)，与管理后台共享同一设计语言。

| 元素 | 规范 |
|------|------|
| **主色** | 华为红 `#C7000B`（vmall-red），链接、按钮、强调 |
| **主色 hover** | `#A00009` 深红 |
| **背景** | 页面底 `#F5F5F5`（vmall-gray-bg）；块/卡片内白底 |
| **文字** | 标题深灰 `#333333`；正文 `#666666`；辅助 `#999999` |
| **边框** | `#E0E0E0`（vmall-gray-border） |
| **块卡片** | 白底、圆角 `rounded-lg`、边框 vmall-gray-border；块间距 4–6 |
| **顶栏** | 白底 + 红色强调；Logo 左 + 类目导航 + 右侧用户操作 |
| **按钮** | 主按钮红底白字；次要白底灰框 |

Tailwind 已配置 `vmall-red`、`vmall-red-hover`、`vmall-gray-bg` 等。

---

## 五、技术栈

- **框架**：Vue 3 + Vite + Vue Router
- **样式**：Tailwind CSS
- **请求**：axios，开发时代理到后端
  - `/api/ai` → smart-interaction-service `http://127.0.0.1:8089`
  - `/api` → BFF `http://localhost:8085`
- **与后端对齐**：请求/响应结构与各 BC 的 `api.yaml` 一致
- **端口**：开发环境 `5174`

---

## 六、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/user/requirements.md` | User BC 需求与 .feature |
| `docs/bounded-contexts/user/api.yaml` | User BC API 契约 |
| `docs/bounded-contexts/catalog/api.yaml` | Catalog BC API 契约 |
| `docs/bounded-contexts/order/api.yaml` | Order BC API 契约 |
| `docs/bounded-contexts/cart/api.yaml` | Cart BC API 契约 |
| `docs/bounded-contexts/smart-interaction/requirements.md` | Smart Interaction 需求 |
| `docs/bounded-contexts/smart-interaction/skills-reference.md` | Skill 配置参考 |
| `docs/frontend/admin/ui-spec.md` | 管理后台界面规格 |
