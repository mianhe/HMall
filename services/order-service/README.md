# HMall Order Service

Order 微服务，独立部署。调用 Catalog 获取 SKU 信息，调用 Inventory 占用/释放库存，调用 Payment 创建支付单与退款。

## 运行

- **端口**：8081
- **依赖**：Catalog（8080）、User（8082）、Inventory（8083，可选）、Payment（8084，可选）、PostgreSQL（5432）。默认不依赖 Kafka，出站事件使用 NoOp；需发往 Kafka 时先启动 Kafka 并加 `--spring.profiles.active=kafka`。

```bash
# 先启动 DB、Catalog、User、Inventory（如需真实支付再启动 payment-service）
./scripts/hmall.sh start db catalog-service user-service inventory-service

# 启动 order-service（配置了 inventory.base-url / payment.base-url 时会真实调用库存/支付）
cd services/order-service && mvn spring-boot:run
# 或
./scripts/hmall.sh start order-service
```

## 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8081 |
| catalog.base-url | Catalog 服务地址 | http://localhost:8080 |
| user.base-url | User 服务地址 | http://localhost:8082 |
| inventory.base-url | Inventory 服务地址；不配置则使用 NoOp 桩 | http://localhost:8083 |
| payment.base-url | Payment 服务地址；不配置则使用 NoOp 桩（创建支付/退款不真实调用） | http://localhost:8084 |
| spring.datasource.* | 数据库连接 | 同 catalog-service |
| spring.autoconfigure.exclude | 默认排除 Kafka，保证无 Kafka 时也能启动 | KafkaAutoConfiguration |
| spring.profiles.active | 设为 kafka 时启用 Kafka 发事件（需 Kafka 已启动） | 无 |

## 测试

```bash
# 验收测试（Catalog/User/Inventory 用 Stub，无需下游服务）
mvn test -Dtest=RunCucumberTest

# Order–Inventory 集成测试（WireMock 模拟 Catalog/User/Inventory，验证占用/释放请求）
mvn test -Dtest=OrderInventoryIntegrationTest

# Order–Payment 集成测试（WireMock 模拟 Payment，验证创建支付/退款请求）
mvn test -Dtest=OrderPaymentIntegrationTest

# 或通过脚本
./scripts/hmall.sh test --cucumber-only --bc order
```

## 验证 Order–Inventory 集成

1. **自动化**：运行集成测试  
   `mvn test -Dtest=OrderInventoryIntegrationTest`  
   会验证：下单时 Order 向 Inventory 发送 `POST /api/inventory/occupy`（含 orderId、items），取消时发送 `POST /api/inventory/release`（含 orderId）。

2. **联调**：同时启动 Order 与 Inventory，用真实请求验证。  
   - 启动：`./scripts/hmall.sh start db catalog-service user-service inventory-service order-service`  
   - 在 Inventory 中为某 SKU 设置库存（如 `PUT /api/inventory/stock/{skuId}` body `{"available":10}`）。  
   - 通过 BFF 或直接调用 Order 创建订单（该 SKU），再查 Inventory 或占用记录，确认库存被占用；取消订单后确认释放。

## 验证 Order–Payment 集成

- **验收测试**：不配置 `payment.base-url` 时使用 NoOp 桩，验收全绿。
- **联调**：配置 `payment.base-url: http://localhost:8084` 并启动 payment-service，下单时 Order 会调用 `POST /api/payments` 创建支付单，取消已支付订单时会调用 `POST /api/payments/refund` 退款。
