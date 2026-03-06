# BFF 限界上下文 - 需求列表

BFF (Backend for Frontend) 为 `frontend/web` 提供统一 API 入口，代理 Catalog、User、Order 的 REST 接口，解决多微服务场景下前端跨域、多入口等问题。POC 阶段聚焦透传代理。

---

## 一、背景与目标

### 现状

| 前端 | 调用方式 | 下游服务 |
|------|----------|----------|
| frontend/web | Vite proxy 分流：`/api/users`、`/api/login` → User:8082；其余 `/api/*` → Catalog:8080 | Catalog、User |
| frontend/admin | 直接调用（或同类 proxy） | Catalog |

- **Catalog**：8080
- **User**：8082
- **Order**：8081

当前 `frontend/web` 通过 Vite 开发时代理到不同端口，生产环境需统一入口。Order 交易流程将增加对 Order 服务的调用，前端需同时访问 Catalog、User、Order 三个服务。

### POC 目标

1. **单一入口**：`frontend/web` 所有 API 请求统一发往 BFF，由 BFF 代理到各下游
2. **验证可行性**：Java 实现、可独立启动、代理 Catalog / User / Order 的既有接口
3. **为 Order 前端铺路**：BFF 就绪后，`frontend/web` 可将 baseURL 指向 BFF，开发 Order 交易流程
4. **保持增量**：`frontend/admin` 暂不改动，仍可直接调用 Catalog（或后续再迁）

---

## 二、POC 阶段需求

| # | 需求 | 状态 |
|---|------|------|
| 1 | 独立可运行，监听 8085 | ✅ 已实现 |
| 2 | 反向代理：/api/categories、/api/products、/api/files → Catalog | ✅ 已实现 |
| 3 | 反向代理：/api/users、/api/login → User | ✅ 已实现 |
| 4 | 反向代理：/api/orders → Order | ✅ 已实现 |
| 5 | 下游 base URL 可配置 | ✅ 已实现 |
| 6 | CORS 允许 frontend/web origin | ✅ 已实现 |

### POC 阶段「不做」

| 能力 | 说明 |
|------|------|
| 鉴权透传 | JWT 校验、Token 透传等，后续迭代 |
| 聚合接口 | 如「结账页一次性拉取商品+地址」等聚合，后续按需加 |
| ~~frontend-admin 迁移~~ | ✅ 已完成，`frontend/admin` 经 BFF 代理 |

---

## 三、路由与下游映射

| 路径前缀 | 下游服务 | 下游端口（默认） |
|----------|----------|------------------|
| `/api/categories` | Catalog | 8080 |
| `/api/products` | Catalog | 8080 |
| `/api/files` | Catalog | 8080 |
| `/api/engraving-patterns` | Catalog | 8080 |
| `/api/users` | User | 8082 |
| `/api/login` | User | 8082 |
| `/api/orders` | Order | 8081 |

---

## 四、配置

```yaml
bff:
  catalog:
    base-url: http://localhost:8080
  user:
    base-url: http://localhost:8082
  order:
    base-url: http://localhost:8081
```

环境变量可覆盖：`BFF_CATALOG_BASE_URL`、`BFF_USER_BASE_URL`、`BFF_ORDER_BASE_URL`。

---

## 五、验收标准

1. BFF 启动后，`curl http://localhost:8085/api/categories` 返回与直接请求 Catalog 一致的结果
2. `curl http://localhost:8085/api/login -X POST -H "Content-Type: application/json" -d '{"username":"x","password":"y"}'` 可转发到 User 并返回
3. `curl http://localhost:8085/api/orders?userId=1` 可转发到 Order 并返回
4. `frontend/web` 将 proxy target 改为 BFF 后，现有页面功能正常

---

## 六、后续迭代

- JWT 校验与透传
- 结账页聚合接口（如 `GET /api/checkout/preview`）
- 健康检查、监控、链路追踪

> **注**：AI 智能对话模块已拆分为独立限界上下文 **Smart Interaction**，详见 [smart-interaction/requirements.md](../smart-interaction/requirements.md)。
