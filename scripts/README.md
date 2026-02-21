# HMall 系统操作脚本

在**项目根目录**执行脚本，统一操作：数据库、后端、前端、MCP Server，以及执行用例。

## 命令概览

| 命令 | 说明 | 用法示例 |
|------|------|-----------|
| **start** | 一键启动（默认启动全部） | `./scripts/hmall.sh start` 或 `./scripts/hmall.sh start db catalog-service` |
| **stop** | 停止指定组件或全部 | `./scripts/hmall.sh stop`、`./scripts/hmall.sh stop catalog-service mcp` |
| **status** | 查看各组件运行状态与端口 | `./scripts/hmall.sh status` |
| **restart** | 重启（先停再起） | `./scripts/hmall.sh restart`、`./scripts/hmall.sh restart catalog-service` |
| **seed-inventory** | 为 SKU 设置可用库存（经 BFF），便于提交订单 | `./scripts/hmall.sh seed-inventory`（默认 skuId 1～20）、`./scripts/hmall.sh seed-inventory 1 5 10` |
| **test** | 执行后端用例（需数据库已启动） | `./scripts/hmall.sh test`、`./scripts/hmall.sh test --cucumber-only`、`./scripts/hmall.sh test --bc user` |

## 组件名称

- **db** — 基础设施（Docker：PostgreSQL 5432、Kafka 9092、MinIO 9000）。脚本会在启动 db 后等待 Kafka 就绪再继续。
- **catalog-service** — Catalog 微服务（端口 8080）
- **user-service** — User 微服务（端口 8082）
- **order-service** — Order 微服务（端口 8081）
- **inventory-service** — Inventory 微服务（端口 8083，脚本启动时固定 8083 以与 user-service 错开）
- **payment-service** — Payment 微服务（端口 8084）
- **activity-service** — Activity 微服务（端口 8086），消费 Kafka 事件，提供业务活动查询与统计
- **cart-service** — Cart 微服务（端口 8087），购物车管理
- **fulfillment-service** — Fulfillment 微服务（端口 8088），履约管理（创建/发货/签收/取消）
- **smart-interaction-service** — Smart Interaction 微服务（端口 8089），LLM + MCP 智能交互
- **bff-web** — BFF 微服务（端口 8085），frontend-admin、frontend-web 经此代理调用后端
- **frontend-admin** — 管理后台（Vite，端口 5173）
- **frontend-web** — 消费者端（Vite，端口 5174）
- **mcp** — MCP Server HTTP（端口 3000）

`start` / `stop` / `restart` 可带一个或多个组件；不写时 **start/stop/restart 默认针对全部**。

## 命令与参数详解

### start [db] [catalog-service] [user-service] [order-service] [inventory-service] [payment-service] [activity-service] [cart-service] [fulfillment-service] [smart-interaction-service] [bff-web] [frontend-admin] [frontend-web] [mcp]

- 启动顺序：先 **db**（并等待 PostgreSQL、Kafka 就绪），再起 **catalog-service**、**user-service**、**order-service**、**inventory-service**、**payment-service**、**activity-service**、**cart-service**、**fulfillment-service**，然后 **bff-web**，最后 **frontend-admin**、**frontend-web**、**mcp**。若 order-service 启动超时，脚本会打印 `.hmall/logs/order-service.log` 末尾若干行，便于排查（常见原因：Kafka 未就绪或端口被占用）。
- 示例：
  - `./scripts/hmall.sh start` — 启动全部
  - `./scripts/hmall.sh start db catalog-service` — 只起数据库与后端
  - `./scripts/hmall.sh start mcp` — 只起 MCP Server

### stop [db] [catalog-service] [user-service] [order-service] [inventory-service] [payment-service] [activity-service] [cart-service] [fulfillment-service] [smart-interaction-service] [bff-web] [frontend-admin] [frontend-web] [mcp]

- 停止指定组件；不写组件时停止全部（顺序：mcp → frontend-web → frontend-admin → bff-web → smart-interaction-service → fulfillment-service → cart-service → activity-service → payment-service → inventory-service → order-service → user-service → catalog-service → db）。
- 示例：
  - `./scripts/hmall.sh stop` — 停止全部
  - `./scripts/hmall.sh stop catalog-service frontend-admin` — 只停后端与管理后台

### status

- 输出各组件是否在运行及监听端口（或 URL）。
- 不接组件参数，一次显示 db / catalog-service / user-service / order-service / inventory-service / payment-service / activity-service / cart-service / fulfillment-service / smart-interaction-service / bff-web / frontend-admin / frontend-web / mcp。

### restart [db] [catalog-service] [user-service] [order-service] [inventory-service] [payment-service] [activity-service] [cart-service] [fulfillment-service] [smart-interaction-service] [bff-web] [frontend-admin] [frontend-web] [mcp]

- 先对指定组件执行 stop，再 start；不写组件时对全部重启。
- 示例：
  - `./scripts/hmall.sh restart` — 重启全部
  - `./scripts/hmall.sh restart catalog-service` — 仅重启后端

### seed-inventory [skuId ...]

- 经 BFF 调用 Inventory，为指定 skuId 设置可用库存 99；不传参数时对 skuId 1～50 执行。**提交订单前若提示库存不足，可先执行此命令或到管理后台 http://localhost:5173 库存页设置。**
- 需 BFF 与 inventory-service 已启动。
- 示例：
  - `./scripts/hmall.sh seed-inventory` — 为 1～20 设置库存
  - `./scripts/hmall.sh seed-inventory 1 2 3` — 仅为 skuId 1、2、3 设置

### test [--cucumber-only] [--clean] [--bc catalog|user|order|inventory|payment|activity|cart|fulfillment|smart-interaction|bff|all]

- 执行全部微服务测试（catalog-service + user-service + order-service + inventory-service + payment-service + activity-service + cart-service + fulfillment-service + smart-interaction-service + bff-web）；执行前会检查数据库是否已启动。
- **测试与生产数据隔离**：验收测试使用 H2 内存库（`application-test.yml`），与 PostgreSQL 完全隔离，测试结束后不会清空生产/开发库。
- 参数：
  - 无参数：`mvn test`（单元 + Cucumber 验收）
  - `--cucumber-only`：仅验收测试 `mvn test -Dtest=RunCucumberTest`
  - `--clean`：先清理再测 `mvn clean test`
  - `--bc <catalog|user|order|inventory|payment|activity|cart|fulfillment|smart-interaction|bff|all>`：仅执行指定微服务/BC 的测试
  - 示例：
  - `./scripts/hmall.sh test` — 执行全部微服务测试（含 payment-service）
  - `./scripts/hmall.sh test --cucumber-only`
  - `./scripts/hmall.sh test --cucumber-only --bc user` — 仅 user-service（User）
  - `./scripts/hmall.sh test --cucumber-only --bc catalog` — 仅 catalog-service
  - `./scripts/hmall.sh test --cucumber-only --bc order` — 仅 order-service（Order）
  - `./scripts/hmall.sh test --cucumber-only --bc inventory` — 仅 inventory-service（Inventory）
  - `./scripts/hmall.sh test --cucumber-only --bc payment` — 仅 payment-service（Payment）
  - `./scripts/hmall.sh test --cucumber-only --bc activity` — 仅 activity-service（Activity）
  - `./scripts/hmall.sh test --cucumber-only --bc cart` — 仅 cart-service（Cart）
  - `./scripts/hmall.sh test --cucumber-only --bc fulfillment` — 仅 fulfillment-service（Fulfillment）
  - `./scripts/hmall.sh test --cucumber-only --bc smart-interaction` — 仅 smart-interaction-service（Smart Interaction / AI Chat）
  - `./scripts/hmall.sh test --cucumber-only --bc bff` — 仅 bff-web（BFF）
  - `./scripts/hmall.sh test --clean`

## 环境与约定

- 脚本需在**项目根目录**执行（或脚本内自动定位到项目根）。
- 数据库使用 `infra/docker-compose.yml`，容器名 `hmall-postgres`。
- 后端/前端/MCP 以后台方式启动时，PID 与日志放在项目根目录下的 `.hmall/`（已加入 .gitignore），便于 `stop` 时按 PID 结束进程。

## 图片下载（演示数据）

从网页获取商品图做展示数据时，可用 `download-images.js`：

1. 在项目根目录新建 `urls.txt`，每行一个图片 URL（可从华为商城等页面「复制图片地址」或从开发者工具 Network 里复制）。
2. 执行：`node scripts/download-images.js`（默认读 `urls.txt`，输出到 `scripts/downloaded-images/`）。
3. 可选参数：`node scripts/download-images.js [urls文件路径] [输出目录]`。

**注意**：仅建议在本地/演示环境使用，勿将他人站点图片用于对外商用。

## 与 Readme 的对应关系

- 一键启动整系统 ≈ Readme「如何启动系统」的整合：`hmall.sh start`。
- 查看状态 = 新增：`hmall.sh status`。
- 重启 = 新增：`hmall.sh restart`。
- 执行用例 = Readme「如何执行测试」的入口：`hmall.sh test`（并可加 `--cucumber-only` / `--clean`）。
