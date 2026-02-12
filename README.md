# HMall

学习用电商商城项目：Spring Boot 后端 + Vue 3 前端 + PostgreSQL，并提供 **MCP Server** 供 AI 对话（Cursor、Claude Desktop 等）操作商品数据。商品限定上下文已实现类别/商品管理及验收测试；前端为管理后台（类别与商品维护）。

本项目采用 **ATDD（验收测试驱动开发）** 与 **DDD（领域驱动设计）** 的开发方法。

## 开发方法简述

### ATDD 相关做法

- **先写验收、再实现**：用 Gherkin 在 `.feature` 里写场景（假如/当/那么），用 Cucumber + Step Definitions 调 REST API 做断言；新场景先红（接口未实现），再按实现路径一步步做到绿。
- **契约与实现一致**：接口形态以 OpenAPI（如 `docs/bounded-contexts/catalog/api.yaml`）为准，Step Definitions 的请求与断言与契约对齐。
- **分步实现与验收**：按「实现路径」文档（如 `docs/bounded-contexts/catalog/process/implementation-path.md`）拆成小步，每步只实现最少接口、让对应用例先绿，再进入下一步。
- **变绿后重构与清理**：实现全绿后对生产代码和测试代码做一次整理（去冗余、统一风格、语义相同的步骤收口到共用断言等），验收保持全绿。

### DDD 相关做法

- **限界上下文**：后端按上下文划分（当前已做「商品限定上下文」Catalog：类目、商品 SPU、规格维度与选项、SKU），文档与代码目录按上下文组织（如 `docs/bounded-contexts/catalog/`、`features/catalog/`）。
- **分层与职责**：领域层（实体、仓储接口）→ 基础设施层（JPA 实体、仓储实现）→ 应用层（用例、事务、业务校验与领域异常）→ API 层（Controller、DTO、统一异常转 HTTP 状态码）。依赖由外向内，领域不依赖框架。
- **领域表达**：聚合与实体在领域层用纯 Java 表达；业务校验失败（如「未选齐必填维度」「价格不能为负」）用领域异常（如 `SkuValidationException`）抛出，由 API 层统一转 400 等。

## 技术栈

- **后端**：Java 21、Spring Boot 3、Maven
- **数据库**：PostgreSQL 16（Docker）
- **前端**：Vue 3、Vite、Vue Router、Tailwind CSS、axios  
  - `frontend-admin`（管理后台，端口 5173）  
  - `frontend-web`（消费者端，端口 5174）
- **MCP**：Node.js MCP Server（在 `hmall-mcp/`），stdio 或 HTTP 两种方式，供任意 MCP Client 调用 Catalog 接口

## 功能与需求文档（哪里看有哪些功能）

| 端 | 文档 | 说明 |
|----|------|------|
| **后端** | `docs/bounded-contexts/catalog/requirements.md` | 后端功能列表，与 .feature 一一对应 |
| **前端（管理后台）** | `docs/frontend-admin/requirements.md` | 管理后台：单页分层展示 Catalog |
| **前端（消费者端）** | `docs/frontend-web/requirements.md` | 消费者端：登录、商城浏览 |
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
| 一键启动全部（DB + 后端 + 前端 + MCP） | `./scripts/hmall.sh start` |
| 查看各组件状态 | `./scripts/hmall.sh status` |
| 停止全部 | `./scripts/hmall.sh stop` |
| 重启全部或指定组件 | `./scripts/hmall.sh restart` 或 `./scripts/hmall.sh restart backend` |
| 执行后端用例（自动起 DB 若未起） | `./scripts/hmall.sh test`（可加 `--cucumber-only` 或 `--clean`） |

以下为手动启动方式。数据库在 Docker 中运行，后端和前端分别在本机启动。

在**项目根目录**执行：

| 操作 | 命令 |
|------|------|
| 启动数据库 | `docker compose -f infra/docker-compose.yml up -d` |
| 启动后端 | `cd backend && mvn spring-boot:run` |
| 启动管理后台 | `cd frontend-admin && npm install && npm run dev` |
| 启动消费者端 | `cd frontend-web && npm install && npm run dev` |
| 停止后端 | 在跑 Spring Boot 的终端里按 `Ctrl+C` |
| 停止前端 | 在跑 Vite 的终端里按 `Ctrl+C` |
| 停止数据库 | `docker compose -f infra/docker-compose.yml down` |

**顺序**：先起数据库 → 再起后端 → 再起前端。浏览器打开 **http://localhost:5173/** 使用管理后台（前端通过代理访问后端 API：http://localhost:8080）。

**可选 — MCP Server（供 AI 对话操作商品）**：在 `hmall-mcp/` 下执行 `npm install && npm run start:http`，默认监听 http://127.0.0.1:3000/mcp，任意 MCP Client 配置该 URL 即可使用。详见 `hmall-mcp/README.md`。

---

## 2. 如何执行测试

所有命令在 **`backend`** 目录下执行（先 `cd backend`）。

| 说明 | 命令 |
|------|------|
| **执行所有用例**（单元 + 验收 Cucumber） | `mvn test` |
| 仅执行 Cucumber 验收测试 | `mvn test -Dtest=RunCucumberTest` |
| 先清理再执行所有测试（推荐偶尔跑一次） | `mvn clean test` |

> 验收测试会连本地 PostgreSQL（`localhost:5432/hmall`），跑前请先启动数据库。

---

## 3. 在哪里看测试结果

| 位置 | 说明 |
|------|------|
| **终端输出** | `mvn test` 运行过程中会打印每个场景通过/失败，最后有 `BUILD SUCCESS` 或 `BUILD FAILURE`。 |
| **Cucumber HTML 报告** | `backend/target/reports/cucumber.html` — 用浏览器打开可看场景列表、步骤、通过/失败。 |
| **Surefire 报告**（可选） | `backend/target/surefire-reports/` — JUnit 文本/XML 报告，IDE 或 CI 常用。 |

跑完测试后可直接打开：

```bash
open backend/target/reports/cucumber.html
```

（Windows 下可用 `start backend/target/reports/cucumber.html`。）

---

## 项目结构

```
├── Readme.md               # 本文件
├── docs/                   # 文档（按上下文分目录）
│   ├── bounded-contexts/   # 限界上下文
│   │   ├── context-map.md  # 上下文地图
│   │   └── catalog/        # Catalog 需求、领域模型、API 契约、process/
│   ├── frontend-admin/     # 管理后台前端设计输入、需求
│   └── frontend-web/       # 消费者端前端需求
├── scripts/                # 系统操作脚本（一键启动/停止/状态/重启/测试）
│   ├── hmall.sh            # 入口脚本
│   └── README.md           # 命令与参数说明
├── infra/
│   └── docker-compose.yml  # PostgreSQL（Docker）
├── backend/                # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/hmall/  # 主类、API、应用层、领域、基础设施
│   │   └── resources/      # application.yml、static/
│   └── src/test/
│       ├── java/.../acceptance  # Cucumber 验收测试（Step Definitions）
│       └── resources/features/catalog/  # .feature（category, product, spec-dimension, sku）
├── frontend-admin/         # Vue 3 + Vite 管理后台（端口 5173）
├── frontend-web/           # Vue 3 + Vite 消费者端（端口 5174）
│   ├── package.json
│   ├── src/
│   │   ├── shared/         # api（catalog 接口）、ui（AppHeader 等）
│   │   ├── pages/          # 首页、Catalog 一页全览（展示为主，增删改由 MCP）
│   │   └── router/
│   └── vite.config.js      # 开发时代理 /api → localhost:8080
└── hmall-mcp/              # MCP Server（stdio + HTTP，供 AI Client 调 Catalog API）
    ├── index.js            # stdio 入口（由 Client 按需拉起）
    ├── index-http.js       # HTTP 入口（独立进程，对外 URL）
    ├── tools/catalog.js    # Catalog 相关 tools
    └── README.md           # 安装、运行方式、Tools 列表、Client 配置
```

## 数据库连接信息（本地）

- 主机：`localhost`
- 端口：`5432`
- 数据库名：`hmall`
- 用户名：`hmall`
- 密码：`hmall_dev`

（与 `infra/docker-compose.yml` 和 `backend/src/main/resources/application.yml` 一致。）

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

当前 Catalog 相关表：`category`、`spu`、`spec_dimension`、`spec_option`、`sku`、`sku_spec_value`。
