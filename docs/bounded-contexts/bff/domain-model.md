# BFF 限界上下文 - 领域模型

BFF 作为前端专用的后端层，核心职责是**路由与代理**，无传统领域实体。模型以路由规则为主。

---

## 路由规则

| 路径前缀 | 下游服务 | 说明 |
|----------|----------|------|
| `/api/categories` | Catalog | 类目 |
| `/api/products` | Catalog | 商品、SKU、规格、展示图 |
| `/api/users` | User | 用户 |
| `/api/login` | User | 登录 |
| `/api/orders` | Order | 订单 |

BFF 接收请求后按路径前缀选择下游 base URL，透传请求并回写响应。不聚合、不转换。

---

## 与 requirements.md 的关系

详细需求与验收标准见同目录 [requirements.md](./requirements.md)。
