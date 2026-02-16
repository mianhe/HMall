# HMall User Service

User 微服务，独立部署。负责用户注册、登录、JWT 认证。

## 运行

- **端口**：8082
- **依赖**：PostgreSQL（端口 5432）

```bash
# 先启动 DB
./scripts/hmall.sh start db

# 启动 user-service
cd services/user-service && mvn spring-boot:run
# 或
./scripts/hmall.sh start user-service
```

## 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8082 |
| spring.datasource.* | 数据库连接 | 同 catalog-service |
| jwt.secret | JWT 签名密钥 | 需配置 |
| jwt.expiration-ms | Token 过期时间（毫秒） | 86400000 |

## 测试

```bash
# 执行 User 验收测试（H2 内存库，无需 PostgreSQL）
mvn test -Dtest=RunCucumberTest

# 或通过脚本
./scripts/hmall.sh test --cucumber-only --bc user
```
