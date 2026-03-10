---
name: deploy
description: 部署 HMall 到阿里云 ECS：本地测试 → 提交推送 → 服务器拉取 → 构建启动 → 验证。支持首次部署与日常更新两种路径。触发词：部署、deploy、发布、上线、更新服务器。
---

# 部署 HMall

将代码部署到阿里云 ECS 服务器。分**首次部署**和**日常更新**两条路径。

| 路径 | 适用场景 | 核心步骤 |
|------|---------|---------|
| 首次部署 | 服务器从未部署过 HMall | 服务器初始化 → 克隆仓库 → 配置 → 构建启动 |
| 日常更新 | 已有运行中的环境 | 本地测试 → 提交推送 → 服务器拉取重部署 |

---

## 核心原则

1. **本地先行，服务器只拉不改**：所有代码变更必须在本地完成、测试通过、提交推送后，服务器通过 `git pull` 获取。**严禁直接在服务器上修改代码。**
2. **环境配置与代码分离**：`.env.prod` 只存在于服务器上（不入 Git），包含密码和 API Key 等敏感信息。
3. **构建在服务器完成**：Docker 多阶段构建，服务器上 `docker compose up --build` 自动编译打包。
4. **最小影响部署**：`deploy.sh pull` 智能检测变更范围，只重建有改动的服务；滚动更新先构建新镜像再逐个替换，减少停机时间。

---

## 关键确认点

**步骤 0** 为路径判定确认点。**步骤 7（验证）** 完成后停顿请开发者确认。其余步骤自主连续执行。

---

## 前置条件

- SSH 密钥已配置到服务器（`ssh root@<IP>` 可免密登录）
- 服务器安全组已开放 22（SSH）、80（HTTP）、443（HTTPS）端口
- 本地 Git 工作区干净或已准备好提交

---

## 步骤 To-Do

### 步骤 0：判定路径

| 动作 | 确认 |
|------|------|
| 确认服务器 IP。若不清楚，从 `deploy/.env.prod.example` 中的 `PUBLIC_HOST` 或询问用户获取。 | |
| SSH 连接服务器，检查是否有运行中的 HMall（`docker ps \| grep hmall`）。有则走**日常更新**，无则走**首次部署**。 | 关键 |

---

### 路径 A：首次部署

#### A1. 服务器初始化

| 步骤 | 动作 |
|------|------|
| 1 | SSH 连接服务器，检查 Docker、Git、Swap 状态 |
| 2 | 执行 `deploy/init-server.sh`（安装 Docker + 镜像加速 + Swap + 防火墙）。若 `apt-get upgrade` 超过 10 分钟无进展，可能卡在海外 PPA 下载，终止后手动跳过 upgrade 继续后续步骤 |
| 3 | 验证：`docker --version`、`docker compose version`、`swapon --show`、`ufw status` |

#### A2. 克隆仓库

| 步骤 | 动作 |
|------|------|
| 4 | `git clone <仓库地址> ~/hmall` |
| 5 | 验证：`ls ~/hmall/deploy/docker-compose.prod.yml` |

#### A3. 配置环境

| 步骤 | 动作 |
|------|------|
| 6 | `cd ~/hmall/deploy && cp .env.prod.example .env.prod` |
| 7 | 填写 `.env.prod` 中的敏感配置。需要向用户确认或从本地环境读取：`DB_PASSWORD`、`MINIO_PASSWORD`、`PUBLIC_HOST`（服务器公网 IP）、`ZHIPU_API_KEY`、`QWEN_API_KEY`、`DEEPSEEK_API_KEY`。API Key 可从本地 `.env` 文件或 shell 环境变量中读取 |

#### A4. 构建启动

| 步骤 | 动作 |
|------|------|
| 8 | `cd ~/hmall/deploy && bash deploy.sh up` |
| 9 | 首次构建约 20-40 分钟。定期检查进度（`docker ps -a`），关注镜像拉取 → Maven 依赖下载 → 编译 → 容器启动各阶段 |

跳转到 **步骤 7：验证**。

---

### 路径 B：日常更新

#### B1. 本地测试

| 步骤 | 动作 |
|------|------|
| 1 | 运行受影响服务的测试：`cd services/<service> && mvn test`。或全量：`./scripts/hmall.sh test` |
| 2 | 若有前端变更，运行 `cd frontend/web && npm run build` 和/或 `cd frontend/admin && npm run build` |
| 3 | 所有测试通过后，提交并推送：`git add . && git commit && git push` |

#### B2. 服务器拉取与重部署

| 步骤 | 动作 |
|------|------|
| 4 | SSH 连接服务器 |
| 5 | `cd ~/hmall/deploy && bash deploy.sh pull`（智能部署：自动检测变更范围，只重建有改动的服务） |
| 6 | 若需手动更新指定服务：`cd ~/hmall/deploy && bash deploy.sh update <service...>`（先构建镜像再逐个替换容器） |

`deploy.sh pull` 的智能检测规则：
- 仅后端 `services/<svc>/` 改动 → 只重建该服务
- `Dockerfile.service` 或 `docker-compose.prod.yml` 改动 → 重建所有后端
- `frontend/`、`deploy/nginx/` 改动 → 重建 Nginx
- `hmall-mcp/`、`Dockerfile.mcp`、`docs/ontology.md` 改动 → 重建 MCP
- 仅文档/脚本改动 → 跳过部署

滚动更新流程：先构建所有新镜像（旧容器保持运行），再按依赖顺序逐个替换容器。

---

### 步骤 7：验证（两条路径共用）

| 步骤 | 动作 | 确认 |
|------|------|------|
| 7a | 检查所有容器运行状态：`docker ps --format "table {{.Names}}\t{{.Status}}"` | |
| 7b | 确认基础设施健康：PostgreSQL (healthy)、Kafka (healthy)、MinIO (running) | |
| 7c | 从服务器内部测试 API：`curl -s http://localhost/api/categories`（200 = BFF 正常）、`curl -s http://localhost/api/ai/skills`（200 = AI 服务正常） | |
| 7d | 从外部测试公网访问：`curl -s -o /dev/null -w "%{http_code}" http://<公网IP>/`（200 = 前端正常） | |
| 7e | 向用户报告部署结果，列出所有服务状态、资源使用情况（内存/磁盘），并提供访问地址 | 关键 |

---

### 步骤 8：初始数据（仅首次部署）

首次部署时数据库为空，需要初始化业务数据。

| 步骤 | 动作 |
|------|------|
| 8a | **Skill 数据**：从本地 API（`http://localhost:8089/api/ai/skills`）导出 Skill，通过生产 API（`http://<公网IP>/api/ai/skills`）POST 创建。注意 URL 路径是 `/api/ai/skills`（经 Nginx 代理） |
| 8b | **其他业务数据**：如类目、商品等，由用户通过管理后台手动创建，或通过 API 批量导入 |

---

## 部署中发现问题的修复流程

部署或验证过程中发现代码/配置问题时（如健康检查命令路径错误、Host 校验失败、Nginx 路由缺失），**必须遵循以下闭环，不得直接在服务器上修改代码**：

```
服务器诊断（日志/状态/curl）→ 回到本地修改 → 本地提交推送 → 服务器 git pull + 重部署 → 重新验证
```

| 步骤 | 动作 | 在哪执行 |
|------|------|---------|
| 1. 诊断 | 查看日志（`docker logs`）、容器状态（`docker ps`）、测试连通性（`curl`）。**只读操作。** | 服务器 |
| 2. 修复 | 根据诊断结果，在本地仓库中修改相关代码或配置文件 | 本地 |
| 3. 提交推送 | `git add . && git commit -m "fix: ..." && git push` | 本地 |
| 4. 拉取重部署 | `cd ~/hmall/deploy && bash deploy.sh pull`（智能检测变更），或 `git pull && bash deploy.sh update <service>` | 服务器 |
| 5. 重新验证 | 回到步骤 7（验证），确认问题已解决 | 服务器 |

> **严禁捷径**：即使"只改一行"、"先跑通再说"，也必须走完 commit → push → pull 闭环。参见 `no-remote-code-edit` 规则。

---

## 故障排查

| 现象 | 排查步骤 |
|------|---------|
| 容器状态为 Created（未启动） | 检查 `depends_on` 中的依赖服务是否 healthy。依赖链：基础设施 → 后端服务 → BFF/MCP → Nginx |
| 后端 healthy 但 BFF/Nginx 仍 Created | `docker compose up -d` 再次触发依赖检查。若 Kafka 仍 unhealthy，可能是健康检查超时不足 |
| MCP 循环重启（ENOENT ontology.md） | 确认 `.dockerignore` 中有 `!docs/ontology.md` 例外，`Dockerfile.mcp` 中 COPY 到 `/docs/ontology.md`（绝对路径） |
| Nginx 启动失败 host not found | MCP upstream 使用了变量 + resolver（`deploy/nginx/default.conf`），若仍失败检查 Docker DNS `127.0.0.11` 是否可用 |
| 服务启动后 MCP 403 Forbidden | MCP SDK 的 Host 头校验拒绝了请求。检查 `docker-compose.prod.yml` 中 `MCP_ALLOWED_HOSTS` 是否包含公网 IP |
| `apt-get upgrade` 卡住 | 阿里云访问海外 PPA 可能很慢。`kill` 卡住的 apt 进程，跳过 upgrade 手动继续后续安装步骤 |
| AI 助手不可用 | 检查 `.env.prod` 中 API Key 是否填写，`docker logs hmall-smart-interaction` 查看错误 |
| 内存不足 | `free -h` 检查。Swap 应为 4GB。`docker stats --no-stream` 查看各容器内存。考虑限制 JVM：在 Dockerfile 或 compose 中加 `-Xmx` |
| 某服务需单独重建 | `bash deploy.sh update <service-name>`（推荐），或 `docker compose ... up -d --build <service>` |
| `deploy.sh pull` 跳过了需要的服务 | 智能检测基于 git diff，若前次部署失败需用 `bash deploy.sh update <service...>` 手动触发 |

---

## 检查清单

- [ ] 路径已判定（首次部署 / 日常更新）
- [ ] **本地测试通过**（日常更新时必须在推送前验证）
- [ ] **代码已提交推送**（服务器通过 git pull 获取，不直接改服务器代码）
- [ ] 服务器环境就绪（Docker、Swap、防火墙）
- [ ] `.env.prod` 已配置（密码、公网 IP、API Key）
- [ ] 所有容器运行正常（`docker ps` 无 Created/Restarting 状态）
- [ ] 基础设施 healthy（PostgreSQL、Kafka）
- [ ] 公网可访问：前端首页、管理后台（`/admin/`）、API
- [ ] 初始数据已导入（仅首次：Skill 等）
- [ ] **开发者已确认**部署结果

---

## 参考

- `deploy/init-server.sh`：服务器初始化脚本
- `deploy/deploy.sh`：部署脚本（up/down/restart/logs/status/pull/update），pull 支持智能变更检测
- `deploy/docker-compose.prod.yml`：生产环境编排
- `deploy/.env.prod.example`：环境变量模板
- `deploy/Dockerfile.service`：Java 微服务镜像构建
- `deploy/Dockerfile.nginx`：Nginx + 前端构建
- `deploy/Dockerfile.mcp`：MCP Server 构建
- `deploy/nginx/default.conf`：Nginx 路由配置
- `docs/deployment/guide.md`：部署指南（面向人类阅读）
- `docs/quality-guard.md`：质量守护机制
