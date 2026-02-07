# HMall

学习用电商商城项目：Spring Boot 后端 + PostgreSQL，迭代 0 已打通「前端 → 后端 → 数据库」链路；商品限定上下文已实现类别/商品管理及验收测试。

## 技术栈

- **后端**：Java 21、Spring Boot 3、Maven
- **数据库**：PostgreSQL 16（Docker）
- **前端**：静态 HTML（当前在 `backend/src/main/resources/static/`）

## 环境要求

- Java 21、Maven 3.x（本机跑应用或跑测试时需要）
- Docker（仅用于跑 PostgreSQL 数据库；应用暂不支持在 Docker 中运行）

---

## 1. 如何启动系统

数据库在 Docker 中运行，后端在本机用 Maven 启动。

在**项目根目录**执行：

| 操作 | 命令 |
|------|------|
| 启动数据库 | `docker compose -f infra/docker-compose.yml up -d` |
| 启动后端 | `cd backend && mvn spring-boot:run` |
| 停止后端 | 在跑 Spring Boot 的终端里按 `Ctrl+C` |
| 停止数据库 | `docker compose -f infra/docker-compose.yml down` |

顺序：先起数据库，再 `mvn spring-boot:run`，浏览器打开 http://localhost:8080/

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
HMall/
├── README.md              # 本文件
├── infra/
│   └── docker-compose.yml # PostgreSQL（Docker）
└── backend/               # Spring Boot 后端
    ├── pom.xml
    ├── src/main/
    │   ├── java/com/hmall/     # 主类、API、应用层、领域、基础设施
    │   └── resources/         # application.yml、static/
    ├── src/test/
    │   ├── java/.../acceptance # Cucumber 验收测试
    │   └── resources/features/catalog/  # category.feature, product.feature
    └── docs/                 # 文档（catalog/ 需求·领域·API，实现步骤等）
```

## 数据库连接信息（本地）

- 主机：`localhost`
- 端口：`5432`
- 数据库名：`hmall`
- 用户名：`hmall`
- 密码：`hmall_dev`

（与 `infra/docker-compose.yml` 和 `backend/src/main/resources/application.yml` 一致。）
