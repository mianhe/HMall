# HMall

学习用电商商城项目：Spring Boot 后端 + PostgreSQL，迭代 0 已打通「前端 → 后端 → 数据库」链路。

## 技术栈

- **后端**：Java 21、Spring Boot 3、Maven
- **数据库**：PostgreSQL 16（Docker）
- **前端**：静态 HTML（当前在 `backend/src/main/resources/static/`）

## 环境要求

- Java 21
- Maven 3.x
- Docker（用于跑 PostgreSQL）

## 常用命令（记不住就查这里）

### 数据库（PostgreSQL）

在**项目根目录**执行：

| 操作 | 命令 |
|------|------|
| 启动数据库 | `docker compose -f infra/docker-compose.yml up -d` |
| 停止数据库 | `docker compose -f infra/docker-compose.yml down` |
| 查看是否在跑 | `docker ps`（看是否有 `hmall-postgres`） |

### 后端（Spring Boot）

在**项目根目录**执行：

| 操作 | 命令 |
|------|------|
| 启动后端 | `cd backend && mvn spring-boot:run` |
| 停止后端 | 在跑 Spring Boot 的终端里按 `Ctrl+C` |

### 启动整条链路的顺序

1. 启动数据库：`docker compose -f infra/docker-compose.yml up -d`
2. 启动后端：`cd backend && mvn spring-boot:run`
3. 浏览器打开：http://localhost:8080/ ，点「检查」验证

### 停止顺序

1. 后端：在终端按 `Ctrl+C`
2. 数据库：`docker compose -f infra/docker-compose.yml down`

## 项目结构

```
HMall/
├── README.md           # 本文件
├── infra/
│   └── docker-compose.yml   # PostgreSQL 容器配置
└── backend/                 # Spring Boot 后端
    ├── pom.xml
    └── src/main/
        ├── java/com/hmall/   # 主类、Controller 等
        └── resources/       # application.yml、static/index.html
```

## 数据库连接信息（本地）

- 主机：`localhost`
- 端口：`5432`
- 数据库名：`hmall`
- 用户名：`hmall`
- 密码：`hmall_dev`

（与 `infra/docker-compose.yml` 和 `backend/src/main/resources/application.yml` 一致。）
