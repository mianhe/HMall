#!/usr/bin/env bash
# HMall 服务器初始化脚本 (Ubuntu 24.04)
# 用法: sudo bash init-server.sh
set -e

echo "=== HMall 服务器初始化 ==="

# 1. 系统更新
echo ">>> 更新系统包..."
apt-get update && apt-get upgrade -y

# 2. 安装 Docker（使用阿里云源加速）
echo ">>> 安装 Docker..."
if ! command -v docker &>/dev/null; then
  apt-get install -y ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable docker
  systemctl start docker
  usermod -aG docker "$SUDO_USER"
  echo "Docker 已安装。"
else
  echo "Docker 已安装，跳过。"
fi

# 3. 配置 Docker 镜像加速器
echo ">>> 配置 Docker 镜像加速..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'DAEMON_EOF'
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
DAEMON_EOF
systemctl daemon-reload
systemctl restart docker
echo "Docker 镜像加速已配置。"

# 4. 配置 4GB Swap
echo ">>> 配置 Swap..."
if [ ! -f /swapfile ]; then
  fallocate -l 4G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "4GB Swap 已配置。"
else
  echo "Swap 已存在，跳过。"
fi

# 5. 安装 Git
echo ">>> 安装 Git..."
apt-get install -y git

# 6. 配置防火墙
echo ">>> 配置防火墙 (ufw)..."
apt-get install -y ufw
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

echo ""
echo "=== 初始化完成 ==="
echo ""
echo "后续步骤："
echo "  1. 退出并重新 SSH 登录（使 docker 用户组生效）"
echo "  2. git clone <你的仓库地址> ~/hmall"
echo "  3. cd ~/hmall/deploy"
echo "  4. cp .env.prod.example .env.prod"
echo "  5. vim .env.prod  # 修改密码和公网 IP"
echo "  6. bash deploy.sh up"
