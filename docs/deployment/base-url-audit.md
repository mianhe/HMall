# Base URL 生产环境配置审计

服务使用的 URL 配置在生产环境（Docker Compose）中必须正确覆盖，否则会出现 Connection refused、404 或跳转错误。本文档说明审计方法与历史漏扫根因。

## 为何漏掉 mockPayBaseUrl

首次审计时漏掉了 `payment.mock-pay-base-url`，导致「立即支付」跳转到 localhost:8084 出现 404。根因：

1. **概念范围过窄**：只关注「服务间调用的 base-url」（A 类），忽略了「客户端可见 URL」（B 类）
2. **注入方式不同**：mockPayBaseUrl 通过 `@ConfigurationProperties` 注入，不在 `@Value` 里，易被关键词搜索遗漏
3. **用途不同**：用于生成 payUrl 返回给前端、供浏览器跳转，不是服务调用服务
4. **扫描命令过窄**：`rg 'base-url'` 能命中 `xxx.base-url`，但未显式纳入「生成给客户端使用的 URL」这一类

## 正确分类

| 类型 | 含义 | prod 应配置为 |
|------|------|---------------|
| **A** | 服务间调用：Service A → HTTP → Service B | Docker 服务名，如 `http://catalog-service:8080` |
| **B** | 客户端可见 URL：服务生成 URL 供浏览器/客户端访问 | 公网地址，如 `http://${PUBLIC_HOST}` |

## 审计结果（按服务）

| 服务 | 依赖的 base-url | application-prod.yml 状态 | 说明 |
|------|-----------------|---------------------------|------|
| **bff-web** | catalog, user, order, inventory, payment, activity, cart, fulfillment | ✅ 完整 | 所有指向 `*-service:port` |
| **cart-service** | catalog | ✅ 完整 | `catalog.base-url: http://catalog-service:8080` |
| **order-service** | catalog, user, inventory, payment, fulfillment | ✅ 完整 | 5 个 base-url 均已配置 |
| **payment-service** | mock-pay-base-url（生成 payUrl） | ✅ 完整 | prod 用 `http://${PUBLIC_HOST}`，nginx 代理 `/mock-pay` |

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

## 统一扫描（推荐）

使用脚本做完整审计，覆盖 A 类 + B 类 + 显式清单：

```bash
node scripts/audit-prod-urls.mjs
```

脚本会：
- 扫描 `application*.yml`（排除 test）中含 localhost/127.0.0.1 的 key
- 扫描 Java 中 `@Value` / `@ConfigurationProperties` 含 localhost 的默认值
- 显式检查已知的 Type B 项（如 `payment.mock-pay-base-url`）
- 对照 `application-prod.yml` 判断是否已覆盖

## 手工扫描（辅助）

```bash
rg 'localhost|127\.0\.0\.1' services --glob '*.yml' --glob '*.java' | grep -v test
```

## 提交订单卡顿的常见原因

1. **Base URL 错误**：order-service 调用 catalog/user/inventory/payment 时若使用 localhost 会 Connection refused，需确保 `application-prod.yml` 中全部指向 `*-service:port`
2. **超时过短**：下单需串行调用多个下游，BFF/nginx 默认 25–30s 可能不足，已为 `/api/orders` 单独配置 60s
3. **下游冷启动**：catalog 等 Java 服务启动需 2–4 分钟，刚部署后立即下单可能失败，建议等待服务就绪后再测

## 新增服务时的检查清单

1. 若新服务需**调用其他 BC**（Type A）：在 `application.yml` 中用 `@Value("${xxx.base-url:http://localhost:port}")` 等默认值；在 `application-prod.yml` 中配置 `xxx.base-url: http://<service-name>:port`
2. 若新服务**生成给客户端/浏览器访问的 URL**（Type B）：在 `application-prod.yml` 中配置为公网地址（如 `http://${PUBLIC_HOST}`），并在 `scripts/audit-prod-urls.mjs` 的 `KNOWN_CLIENT_FACING` 中补充一项，避免后续漏扫
3. Docker Compose 中服务名与 `application-prod.yml` 中的 host 一致（如 `catalog-service`）
