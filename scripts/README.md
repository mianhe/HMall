# HMall 系统操作脚本

在**项目根目录**执行脚本，统一操作：数据库、后端、前端、MCP Server，以及执行用例。

## 命令概览

| 命令 | 说明 | 用法示例 |
|------|------|-----------|
| **start** | 一键启动（默认启动全部） | `./scripts/hmall.sh start` 或 `./scripts/hmall.sh start db backend` |
| **stop** | 停止指定组件或全部 | `./scripts/hmall.sh stop`、`./scripts/hmall.sh stop backend mcp` |
| **status** | 查看各组件运行状态与端口 | `./scripts/hmall.sh status` |
| **restart** | 重启（先停再起） | `./scripts/hmall.sh restart`、`./scripts/hmall.sh restart backend` |
| **test** | 执行后端用例（需数据库已启动） | `./scripts/hmall.sh test`、`./scripts/hmall.sh test --cucumber-only` |

## 组件名称

- **db** — PostgreSQL（Docker，端口 5432）
- **backend** — Spring Boot 后端（端口 8080）
- **frontend** — Vite 前端（端口 5173）
- **mcp** — MCP Server HTTP（端口 3000）

`start` / `stop` / `restart` 可带一个或多个组件；不写时 **start/stop/restart 默认针对全部**。

## 命令与参数详解

### start [db] [backend] [frontend] [mcp]

- 启动顺序：先 **db**，等数据库可连后再起 **backend**，然后可并行起 **frontend** 与 **mcp**。
- 示例：
  - `./scripts/hmall.sh start` — 启动全部
  - `./scripts/hmall.sh start db backend` — 只起数据库与后端
  - `./scripts/hmall.sh start mcp` — 只起 MCP Server

### stop [db] [backend] [frontend] [mcp]

- 停止指定组件；不写组件时停止全部（顺序：mcp → frontend → backend → db）。
- 示例：
  - `./scripts/hmall.sh stop` — 停止全部
  - `./scripts/hmall.sh stop backend frontend` — 只停后端与前端

### status

- 输出各组件是否在运行及监听端口（或 URL）。
- 不接组件参数，一次显示 db / backend / frontend / mcp 四项。

### restart [db] [backend] [frontend] [mcp]

- 先对指定组件执行 stop，再 start；不写组件时对全部重启。
- 示例：
  - `./scripts/hmall.sh restart` — 重启全部
  - `./scripts/hmall.sh restart backend` — 仅重启后端

### test [--cucumber-only] [--clean]

- 在 **backend** 目录执行 Maven 测试；执行前会检查数据库是否已启动，未启动则先启动 db。
- 参数：
  - 无参数：`mvn test`（单元 + Cucumber 验收）
  - `--cucumber-only`：仅验收测试 `mvn test -Dtest=RunCucumberTest`
  - `--clean`：先清理再测 `mvn clean test`
- 示例：
  - `./scripts/hmall.sh test`
  - `./scripts/hmall.sh test --cucumber-only`
  - `./scripts/hmall.sh test --clean`

## 环境与约定

- 脚本需在**项目根目录**执行（或脚本内自动定位到项目根）。
- 数据库使用 `infra/docker-compose.yml`，容器名 `hmall-postgres`。
- 后端/前端/MCP 以后台方式启动时，PID 与日志放在项目根目录下的 `.hmall/`（已加入 .gitignore），便于 `stop` 时按 PID 结束进程。

## 与 Readme 的对应关系

- 一键启动整系统 ≈ Readme「如何启动系统」的整合：`hmall.sh start`。
- 查看状态 = 新增：`hmall.sh status`。
- 重启 = 新增：`hmall.sh restart`。
- 执行用例 = Readme「如何执行测试」的入口：`hmall.sh test`（并可加 `--cucumber-only` / `--clean`）。
