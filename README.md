# HMall

学习用电商商城项目：Spring Boot 微服务 + Vue 3 前端 + PostgreSQL，并提供 **MCP Server** 供 AI 对话（Cursor、Claude Desktop 等）操作商品数据。已拆分三个限界上下文为独立微服务：**Catalog**（商品、类目、规格）、**User**（用户、登录）、**Order**（订单）；前端为管理后台与消费者端。

本项目采用 **ATDD（验收测试驱动开发）** 与 **DDD（领域驱动设计）** 的开发方法。

## 开发方法简述

### ATDD 相关做法

- **先写验收、再实现**：用 Gherkin 在 `.feature` 里写场景（假如/当/那么），用 Cucumber + Step Definitions 调 REST API 做断言；新场景先红（接口未实现），再按实现路径一步步做到绿。
- **契约与实现一致**：接口形态以 OpenAPI（如 `docs/bounded-contexts/catalog/api.yaml`）为准，Step Definitions 的请求与断言与契约对齐。
- **分步实现与验收**：按需求与验收测试拆成小步，每步只实现最少接口、让对应用例先绿，再进入下一步。
- **变绿后重构与清理**：实现全绿后对生产代码和测试代码做一次整理（去冗余、统一风格、语义相同的步骤收口到共用断言等），验收保持全绿。

### DDD 相关做法

- **限界上下文**：按上下文拆分为独立微服务：Catalog（类目、商品 SPU、规格维度与选项、SKU）、User（用户、登录、JWT）、Order（订单）。各微服务独立部署、独立验收测试，文档与代码按上下文组织（如 `docs/bounded-contexts/catalog/`、`features/catalog/`）。
- **分层与职责**：领域层（实体、仓储接口）→ 基础设施层（JPA 实体、仓储实现）→ 应用层（用例、事务、业务校验与领域异常）→ API 层（Controller、DTO、统一异常转 HTTP 状态码）。依赖由外向内，领域不依赖框架。
- **领域表达**：聚合与实体在领域层用纯 Java 表达；业务校验失败（如「未选齐必填维度」「价格不能为负」）用领域异常（如 `SkuValidationException`）抛出，由 API 层统一转 400 等。

## 技术栈

- **后端**：Java 21、Spring Boot 3、Maven（三个微服务）
  - `catalog-service`（Catalog，端口 8080）
  - `user-service`（User，端口 8082）
  - `order-service`（Order，端口 8081）
- **数据库**：PostgreSQL 16（Docker）
- **前端**：Vue 3、Vite、Vue Router、Tailwind CSS、axios  
  - `frontend/admin`（管理后台，端口 5173）  
  - `frontend/web`（消费者端，端口 5174）
- **MCP**：Node.js MCP Server（在 `hmall-mcp/`），stdio 或 HTTP 两种方式，供任意 MCP Client 调用 Catalog 接口

## 功能与需求文档（哪里看有哪些功能）

| 端 | 文档 | 说明 |
|----|------|------|
| **后端** | `docs/bounded-contexts/catalog/requirements.md` | 后端功能列表，与 .feature 一一对应 |
| **前端（管理后台）** | `docs/frontend/admin/ui-spec.md` | 管理后台：单页分层展示 Catalog |
| **前端（消费者端）** | `docs/frontend/web/ui-spec.md` | 消费者端：登录、商城浏览 |
| **MCP** | `hmall-mcp/README.md` | MCP Server 的 Tools 列表、stdio/HTTP 启动方式、Client 配置说明 |

## 环境要求

- Java 21、Maven 3.x（跑后端或后端测试时）
- Node.js 18+、npm（跑前端时）
- Docker（跑 PostgreSQL 时；应用暂不支持在 Docker 中运行）

---

## 1. 如何启动系统

**推荐**：使用脚本一键操作（启动/停止/查看状态/重启/执行用例），详见 **`scripts/README.md`**。在项目根目录执行：

| 操作 | 命令 |
|------|------|
| 一键启动全部（DB + 三个微服务 + 前端 + MCP） | `./scripts/hmall.sh start` |
| 查看各组件状态 | `./scripts/hmall.sh status` |
| 停止全部 | `./scripts/hmall.sh stop` |
| 重启全部或指定组件 | `./scripts/hmall.sh restart` 或 `./scripts/hmall.sh restart catalog-service` |
| 执行全部微服务用例（自动起 DB 若未起，末尾有汇总） | `./scripts/hmall.sh test`（可加 `--cucumber-only`、`--clean` 或 `--bc catalog|user|order`） |

以下为手动启动方式。数据库在 Docker 中运行，各微服务和前端分别在本机启动。

在**项目根目录**执行：

| 操作 | 命令 |
|------|------|
| 启动数据库 | `docker compose -f infra/docker-compose.yml up -d` |
| 启动 catalog-service（Catalog） | `cd services/catalog-service && mvn spring-boot:run` |
| 启动 user-service（User） | `cd services/user-service && mvn spring-boot:run` |
| 启动 order-service（Order） | `cd services/order-service && mvn spring-boot:run` |
| 启动管理后台 | `cd frontend/admin && npm install && npm run dev` |
| 启动消费者端 | `cd frontend/web && npm install && npm run dev` |
| 停止微服务/前端 | 在对应终端里按 `Ctrl+C` |
| 停止数据库 | `docker compose -f infra/docker-compose.yml down` |

**顺序**：先起数据库 → 再起三个微服务（catalog-service、user-service、order-service）→ 再起前端。浏览器打开 **http://localhost:5173/** 使用管理后台，**http://localhost:5174/** 使用消费者端。前端代理 `/api` 按路径转发到对应微服务（8080/8082/8081）。

**可选 — MCP Server（供 AI 对话操作商品）**：在 `hmall-mcp/` 下执行 `npm install && npm run start:http`，默认监听 http://127.0.0.1:3000/mcp，任意 MCP Client 配置该 URL 即可使用。详见 `hmall-mcp/README.md`。

---

## 2. 如何执行测试

**推荐**：使用脚本执行全部微服务测试，末尾会输出汇总（通过/失败/错误数）：

```bash
./scripts/hmall.sh test --cucumber-only
```

可按 BC 只跑指定微服务：`--bc catalog` / `--bc user` / `--bc order`。详见 `scripts/README.md`。

**单独跑某微服务**：在对应目录下执行（先 `cd services/catalog-service` 或 `cd services/user-service` 或 `cd services/order-service`）：

| 说明 | 命令 |
|------|------|
| **执行所有用例**（单元 + 验收 Cucumber） | `mvn test` |
| 仅执行 Cucumber 验收测试 | `mvn test -Dtest=RunCucumberTest` |
| 先清理再执行 | `mvn clean test` |

> Catalog、User 的验收测试使用 H2 内存库；Order 使用 WireMock 模拟 Catalog；跑全量脚本时会自动起 DB 若未起。

---

## 3. 在哪里看测试结果

| 位置 | 说明 |
|------|------|
| **终端汇总** | `./scripts/hmall.sh test --cucumber-only` 跑完后会打印「HMall 测试汇总」，列出各微服务 passed/failed/errors 及总计。 |
| **各微服务终端输出** | 运行过程中会打印每个场景通过/失败，最后有 `BUILD SUCCESS` 或 `BUILD FAILURE`。 |
| **Cucumber HTML 报告** | `services/*/target/reports/cucumber.html`（catalog-service、user-service、order-service）— 用浏览器打开可看场景列表、步骤。 |
| **Surefire 报告**（可选） | 各微服务 `target/surefire-reports/` — JUnit 文本/XML 报告，IDE 或 CI 常用。 |

---

## 项目结构

```
├── Readme.md               # 本文件
├── docs/                   # 文档
│   ├── context-map.md      # 上下文地图（BC 总览）
│   ├── architecture/       # 架构、集成技术、事件分析方法
│   ├── bounded-contexts/   # 各 BC 需求、领域模型、API 契约
│   │   ├── catalog/
│   │   └── user/
│   ├── frontend/
│   │   ├── admin/          # 管理后台界面规格（ui-spec）
│   │   └── web/            # 消费者端界面规格（ui-spec）
├── scripts/                # 系统操作脚本（一键启动/停止/状态/重启/测试）
│   ├── hmall.sh            # 入口脚本
│   └── README.md           # 命令与参数说明
├── infra/
│   └── docker-compose.yml  # PostgreSQL（Docker）
├── services/               # 后端微服务
│   ├── catalog-service/    # Catalog（端口 8080）
│   ├── user-service/       # User（端口 8082）
│   └── order-service/      # Order（端口 8081）
├── frontend/
│   ├── admin/              # Vue 3 + Vite 管理后台（端口 5173）
│   └── web/                # Vue 3 + Vite 消费者端（端口 5174）
│       └── vite.config.js  # 代理 /api/users、/api/login→8082，其余/api→8080
└── hmall-mcp/              # MCP Server（stdio + HTTP，供 AI Client 调 Catalog API）
    └── README.md
```

## 数据库连接信息（本地）

- 主机：`localhost`
- 端口：`5432`
- 数据库名：`hmall`
- 用户名：`hmall`
- 密码：`hmall_dev`

（与 `infra/docker-compose.yml` 和 `services/*/application.yml` 一致。）

---

## 如何查看数据库结构

本机未安装 `psql` 时，可用已运行的 Postgres 容器执行（需先启动数据库：`docker compose -f infra/docker-compose.yml up -d`）。

**进入交互式 psql**（在项目根目录执行）：

```bash
docker exec -it hmall-postgres psql -U hmall -d hmall
```

进入后常用命令：

| 操作 | 命令 |
|------|------|
| 列出所有表 | `\dt` |
| 查看某表结构（列、类型、约束） | `\d 表名`，如 `\d category`、`\d sku` |
| 执行 SQL | 输入 SQL 并以**分号 `;`** 结尾，如 `select * from sku;` |
| 退出 | `\q` |

**不进入交互、只查表列表：**

```bash
docker exec hmall-postgres psql -U hmall -d hmall -c "\dt"
```

当前各微服务相关表：`category`、`spu`、`spec_dimension`、`spec_option`、`sku`、`sku_spec_value`（Catalog）；`users`（User）；`orders`、`order_line_item`（Order）。
