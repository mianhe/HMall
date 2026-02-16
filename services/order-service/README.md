# HMall Order Service

Order 微服务，独立部署。调用 Catalog 服务获取 SKU 信息。

## 运行

- **端口**：8081
- **依赖**：Catalog 服务（端口 8080）、PostgreSQL（端口 5432）

```bash
# 先启动 DB 和 catalog-service（Catalog）
./scripts/hmall.sh start db catalog-service

# 启动 order-service
cd services/order-service && mvn spring-boot:run
# 或
./scripts/hmall.sh start order-service
```

## 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8081 |
| catalog.base-url | Catalog 服务地址 | http://localhost:8080 |
| spring.datasource.* | 数据库连接 | 同 catalog-service |

## 测试

```bash
# 执行 Order 验收测试（WireMock 模拟 Catalog，无需 catalog-service 运行）
mvn test -Dtest=RunCucumberTest

# 或通过脚本
./scripts/hmall.sh test --cucumber-only --bc order
```
