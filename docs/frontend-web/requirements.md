# 前端功能需求 — 消费者端（frontend-web）

消费者端面向终端用户，提供注册、登录和商城浏览功能。后端需求见 `docs/bounded-contexts/user/requirements.md`。

---

## 一、定位

| 端 | 职责 |
|----|------|
| **消费者端（frontend-web）** | 注册、登录、浏览商城 |
| **后端** | 用户管理（`POST /api/users`）、认证（`POST /api/login`）、商品数据 |

---

## 二、页面结构

| 路由 | 页面 | 功能 | 后端 API |
|------|------|------|----------|
| `/` | HomePage | 首页入口，导航到登录/注册 | — |
| `/login` | LoginPage | 用户登录表单，成功后存 token 跳回首页 | `POST /api/login` |
| `/register` | RegisterPage | 用户注册表单，成功后自动登录跳回首页 | `POST /api/users` → `POST /api/login` |

> 后续规划：商品浏览等。

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

- 展示欢迎文案
- 提供「登录」和「注册」入口

---

## 四、技术栈

- **框架**：Vue 3 + Vite + Vue Router
- **样式**：Tailwind CSS（VMALL 风格）
- **请求**：axios，baseURL 开发时用 Vite 代理到 `http://localhost:8080`
- **端口**：开发环境 `5174`

---

## 五、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/user/requirements.md` | 后端 User BC 需求与 .feature |
| `docs/bounded-contexts/user/api.yaml` | User BC API 契约 |
| `docs/frontend-admin/requirements.md` | 管理后台需求 |
