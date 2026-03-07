# HMall 阿里云部署指南

## 前提条件

- 阿里云 ECS 实例（建议 2C8G，Ubuntu 24.04）
- 代码已推送到 Git 远程仓库（GitHub / Gitee 等）
- 安全组已开放 80 端口（HTTP）和 22 端口（SSH）

## 部署前测试（推送代码前执行）

推送到远程仓库前，在本机运行以下测试确保代码正常。

### 后端单元 + 验收测试

需要本地 Docker 运行（PostgreSQL + Kafka）：

```bash
./scripts/hmall.sh test
```

如果本地 Docker 未启动，可直接用 Maven 运行（测试使用 H2 内存库，不依赖外部服务）：

```bash
# 全部服务
for svc in catalog-service user-service order-service inventory-service \
           payment-service activity-service cart-service fulfillment-service \
           smart-interaction-service bff-web; do
  echo "=== Testing $svc ==="
  (cd services/$svc && mvn test -q) && echo "PASS" || echo "FAIL"
done

# 单个服务
cd services/catalog-service && mvn test

# 仅 Cucumber 验收测试
./scripts/hmall.sh test --cucumber-only

# 指定 BC
./scripts/hmall.sh test --bc cart
```

### 前端构建检查

```bash
cd frontend/admin && npm run build
cd frontend/web && npm run build

# admin 带 /admin/ 路径前缀（生产环境使用的方式）
cd frontend/admin && npx vite build --base=/admin/
```

## 第一步：初始化服务器

SSH 登录到你的 ECS：

```bash
ssh root@<你的公网IP>
```

下载并执行初始化脚本（安装 Docker、配置 Swap、开防火墙）：

```bash
# 方式一：如果已 clone 了仓库
sudo bash ~/hmall/deploy/init-server.sh

# 方式二：手动执行
apt-get update && apt-get upgrade -y
curl -fsSL https://get.docker.com | sh
systemctl enable docker && systemctl start docker
usermod -aG docker $USER

# 配置 4GB Swap
fallocate -l 4G /swapfile
chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

**重要**：执行完后退出 SSH 并重新登录，使 docker 用户组生效。

## 第二步：拉取代码

```bash
git clone <你的仓库地址> ~/hmall
cd ~/hmall
```

## 第三步：配置环境变量

```bash
cd ~/hmall/deploy
cp .env.prod.example .env.prod
vim .env.prod
```

需要修改的配置：

| 变量 | 说明 | 示例 |
|------|------|------|
| `DB_PASSWORD` | 数据库密码 | 自定义强密码 |
| `MINIO_PASSWORD` | 对象存储密码 | 自定义强密码 |
| `PUBLIC_HOST` | 你的公网 IP 或域名 | `47.115.230.90` |
| `ZHIPU_API_KEY` | 智谱 AI Key（可选） | 从智谱开放平台获取 |

## 第四步：启动部署

```bash
cd ~/hmall/deploy
bash deploy.sh up
```

首次构建需要 20-40 分钟（下载 Maven 依赖 + 编译 11 个服务 + 构建前端）。后续更新会很快（Docker 缓存）。

查看构建日志：

```bash
bash deploy.sh logs          # 所有服务
bash deploy.sh logs nginx    # 指定服务
```

## 第五步：验证

所有容器启动后，打开浏览器访问：

| 地址 | 说明 |
|------|------|
| `http://<公网IP>/` | 用户前台 |
| `http://<公网IP>/admin/` | 管理后台 |
| `http://<公网IP>/api/catalog/categories` | API 测试 |

## 日常运维

```bash
cd ~/hmall/deploy

# 查看服务状态
bash deploy.sh status

# 查看日志
bash deploy.sh logs                     # 全部
bash deploy.sh logs catalog-service     # 单个服务

# 拉取最新代码并重新部署
bash deploy.sh pull

# 停止所有服务
bash deploy.sh down

# 重启所有服务
bash deploy.sh restart
```

## 数据持久化

以下数据通过 Docker Volume 持久化，`deploy.sh down` 不会删除：

- `pgdata` — PostgreSQL 数据库
- `minio_data` — MinIO 对象存储文件

查看 Volume：

```bash
docker volume ls | grep hmall
```

## 故障排查

### 某个服务启动失败

```bash
# 查看该服务日志
bash deploy.sh logs <service-name>

# 重启单个服务
docker compose -f docker-compose.prod.yml --env-file .env.prod restart <service-name>
```

### 内存不足

```bash
# 检查内存和 Swap 使用
free -h

# 检查各容器内存占用
docker stats --no-stream
```

### 需要重新构建某个服务

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build <service-name>
```
