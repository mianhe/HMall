# Base URL 生产环境配置审计

服务间调用使用 `base-url` 等配置，生产环境（Docker Compose）中必须指向 Docker 服务名，否则会请求 `localhost` 导致 Connection refused。本文档用于统一扫描与检查。

## 审计结果（按服务）

| 服务 | 依赖的 base-url | application-prod.yml 状态 | 说明 |
|------|-----------------|---------------------------|------|
| **bff-web** | catalog, user, order, inventory, payment, activity, cart, fulfillment | ✅ 完整 | 所有指向 `*-service:port` |
| **cart-service** | catalog | ✅ 完整 | `catalog.base-url: http://catalog-service:8080` |
| **order-service** | catalog, user, inventory, payment, fulfillment | ✅ 完整 | 5 个 base-url 均已配置 |

## 依赖方详情

### cart-service
- `catalog.base-url` → 查询 SKU 信息（加购时）
- 默认 `http://localhost:8080`，prod 需 `http://catalog-service:8080`

### order-service
- `catalog.base-url` → 查询 SKU 信息（下单时）
- `user.base-url` → 校验用户存在
- `inventory.base-url` → 占用库存
- `payment.base-url` → 创建支付单
- `fulfillment.base-url` → 创建履约单（事件驱动，非下单主路径）
- 默认均为 localhost，prod 需指向对应 `*-service:port`

### bff-web
- 仅做 API 代理，prod 中所有下游 base-url 已配置

## 扫描命令（维护用）

```bash
# 查找所有使用 base-url 的适配器
rg '@Value.*base-url|base-url:' services --glob '*.java' --glob '*.yml' -A0

# 检查各服务 application-prod.yml 中的 base-url
for f in services/*/src/main/resources/application-prod.yml; do
  echo "=== $f ==="
  grep -E 'base-url|base_url' "$f" 2>/dev/null || echo "(无)"
done
```

## 提交订单卡顿的常见原因

1. **Base URL 错误**：order-service 调用 catalog/user/inventory/payment 时若使用 localhost 会 Connection refused，需确保 `application-prod.yml` 中全部指向 `*-service:port`
2. **超时过短**：下单需串行调用多个下游，BFF/nginx 默认 25–30s 可能不足，已为 `/api/orders` 单独配置 60s
3. **下游冷启动**：catalog 等 Java 服务启动需 2–4 分钟，刚部署后立即下单可能失败，建议等待服务就绪后再测

## 新增服务时的检查清单

1. 若新服务需调用其他 BC：在 `application.yml` 中使用 `@Value("${xxx.base-url:http://localhost:port}")` 等默认值
2. 必须新增 `application-prod.yml`，显式配置 `xxx.base-url: http://<service-name>:port`
3. Docker Compose 中服务名与 `application-prod.yml` 中的 host 一致（如 `catalog-service`）
