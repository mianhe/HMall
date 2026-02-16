# HMall BFF Web

BFF (Backend for Frontend) 微服务，为 frontend-web 提供统一 API 入口，代理 Catalog、User、Order 的 REST 接口。

## 启动

```bash
# 需先启动 Catalog(8080)、User(8082)、Order(8081)
mvn spring-boot:run
```

默认监听 8085。

## 配置

`application.yml`:

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

## 路由

| 路径前缀 | 下游 |
|----------|------|
| /api/categories | Catalog |
| /api/products | Catalog |
| /api/files | Catalog |
| /api/users | User |
| /api/login | User |
| /api/orders | Order |

## 健康检查

```
GET /health
```

## 联调 frontend-web

将 `frontend-web/vite.config.js` 的 proxy target 改为 `http://localhost:8085` 即可。
