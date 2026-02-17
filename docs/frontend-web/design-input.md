
# 前端设计输入 — 消费者端（frontend-web）

**本文档作用**：描述该前端的**页面结构、组件分层与设计风格**，与 requirements（功能与 API）互补。用于新页面或大改结构时保持实现一致；详细约定见 `docs/design-principles.md` 第三节与「Design Input 使用」。

> 消费者端以**浏览与交易**为主；「我的」页聚合用户相关入口，分块展示，参考华为商城 VMALL。UI 按 **Atomic Design** 分层。

---

## 一、FSD 输入（「我的」页范围）

### 1.1 实体（entities）

| 实体 | 说明 |
|------|------|
| User | 用户：id、username（登录态来自 JWT） |
| Address | 收货地址：addressId、userId、recipientName、phone、省市区、detail |
| Order | 订单：orderId、status、items、totalAmountCents |

### 1.2 页面/路由（pages）

| 路由 | 页面说明 |
|------|----------|
| `/my` | 我的：用户信息块 + 收货地址入口 + 我的订单块（分块展示，VMALL 风格） |
| `/addresses` | 收货地址管理（列表、增删改） |
| `/orders` | 我的订单列表 |
| `/orders/:id` | 订单详情 |

### 1.3 功能（features）—「我的」页

| 功能 | 说明 | 对应块/操作 |
|------|------|--------------|
| 用户信息展示 | 头像、用户名 | UserProfileCard |
| 收货地址入口 | 点击进入地址管理 | UserProfileCard 内链接 |
| 我的订单入口 | 待付款/待收货/全部订单 快捷入口 | MyOrdersCard |
| 退出登录 | 退出并跳首页 | UserProfileCard 或顶栏 |

### 1.4 共享（shared）

- 请求：`/api/users/{userId}/addresses`、`/api/orders?userId=xxx`（列表仅需数量或摘要时可后续加聚合接口）
- 通用 UI：块卡片、带箭头的导航行、图标+文案

---

## 二、Atomic Design 输入（「我的」页）

### 2.1 Atoms（原子）

| 组件 | 说明 |
|------|------|
| BlockTitle | 块标题：粗体、深色 |
| NavArrow | 右侧箭头 `>`，表示可点击 |
| IconPlaceholder | 占位图标（如订单状态图标）：统一尺寸圆/方容器 |

### 2.2 Molecules（分子）

| 组件 | 说明 |
|------|------|
| IconLabel | 图标 + 文案（如 📦 待收货） |
| NavRow | 单行导航：左侧文案 + 右侧箭头，可点击 |
| MetricRow | 数值 + 单位/标签（如 0 积分），可选右侧箭头 |

### 2.3 Organisms（有机体）

| 组件 | 说明 |
|------|------|
| BlockCard | 白色卡片容器：圆角、边框、内边距，可插 slot |
| UserProfileCard | 用户块：头像 + 用户名；下方 NavRow「收货地址」；可选 MetricRow（积分/优惠券等占位）；底部「退出登录」 |
| MyOrdersCard | 订单块：标题「我的订单」+ 右侧「全部订单 >」；一行多个 IconLabel（待付款、待收货、待评价）；点击跳对应列表或详情 |
| ServiceToolsCard | 服务工具块（可选）：标题 + 网格 IconLabel，后续扩展 |

### 2.4 页面结构（「我的」页）

| 层级 | 结构 |
|------|------|
| 我的页 | 面包屑（首页 > 我的） + UserProfileCard + MyOrdersCard [+ ServiceToolsCard] |

---

## 三、分块展示规范（参考 VMALL）

| 块 | 内容 |
|----|------|
| 用户信息块 | 头像、用户名；收货地址 >；退出登录 |
| 我的订单块 | 标题 + 全部订单 >；待付款、待收货、待评价（图标+文案），点击进列表或筛选 |

块与块之间留白；块内元素对齐、间距统一；主操作使用 `vmall-red`。

---

## 四、技术栈与约定

- **框架**：Vue 3 + Vite + Vue Router
- **样式**：Tailwind CSS（vmall-red、vmall-gray-*）
- **请求**：axios，baseURL 经 Vite 代理到 BFF
- **设计风格**：与 frontend-admin/design-input 一致，参考 VMALL 主色与留白

---

## 五、设计风格（VMALL）

| 元素 | 规范 |
|------|------|
| 主色 | `#C7000B`（vmall-red），链接、按钮、强调 |
| 背景 | 页面底 `#F5F5F5`（vmall-gray-bg）；块内白底 |
| 文字 | 标题深灰；正文 vmall-gray-text |
| 块卡片 | 白底、圆角、边框 vmall-gray-border；块间距 4–6 |
