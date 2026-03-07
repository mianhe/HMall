#!/usr/bin/env bash
# HMall 部署脚本
# 用法: bash deploy.sh [up|down|restart|logs|status|pull]
set -e

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.prod.yml"
ENV_FILE="${DEPLOY_DIR}/.env.prod"
PROJECT_ROOT="$(dirname "$DEPLOY_DIR")"

if [ ! -f "$ENV_FILE" ]; then
  echo "错误: ${ENV_FILE} 不存在"
  echo "请先执行: cp ${DEPLOY_DIR}/.env.prod.example ${ENV_FILE}"
  exit 1
fi

COMPOSE_CMD="docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE}"

usage() {
  echo "HMall 部署脚本"
  echo ""
  echo "用法: $0 <command> [service...]"
  echo ""
  echo "命令:"
  echo "  up        构建并启动所有服务（首次部署用这个）"
  echo "  down      停止并移除所有容器"
  echo "  restart   重启所有服务"
  echo "  logs      查看日志（可指定服务名，Ctrl+C 退出）"
  echo "  status    查看所有服务运行状态"
  echo "  pull      拉取最新代码并重新部署"
}

case "${1:-}" in
  up)
    echo ">>> 构建并启动 HMall..."
    $COMPOSE_CMD up -d --build
    echo ""
    echo ">>> 启动完成，服务状态："
    $COMPOSE_CMD ps
    ;;
  down)
    echo ">>> 停止 HMall..."
    $COMPOSE_CMD down
    echo ">>> 已停止"
    ;;
  restart)
    echo ">>> 重启 HMall..."
    $COMPOSE_CMD down
    $COMPOSE_CMD up -d --build
    echo ""
    echo ">>> 重启完成，服务状态："
    $COMPOSE_CMD ps
    ;;
  logs)
    shift
    $COMPOSE_CMD logs -f "$@"
    ;;
  status)
    $COMPOSE_CMD ps
    ;;
  pull)
    echo ">>> 拉取最新代码..."
    cd "$PROJECT_ROOT"
    git pull
    echo ">>> 重新部署..."
    $COMPOSE_CMD up -d --build
    echo ""
    echo ">>> 部署完成，服务状态："
    $COMPOSE_CMD ps
    ;;
  *)
    usage
    ;;
esac
