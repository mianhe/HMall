#!/usr/bin/env bash
# HMall 部署脚本
# 用法: bash deploy.sh [up|down|restart|logs|status|pull|update]
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

# Upstream services first, BFF last (respects dependency order)
SERVICE_ORDER="catalog-service user-service order-service inventory-service payment-service activity-service cart-service fulfillment-service smart-interaction-service promotion-service bff-web"

wait_for_api() {
  echo ""
  echo ">>> 等待 API 就绪（每 15s 检查，最多 5 分钟）..."
  local max_attempts=20
  local attempt=1
  while [ $attempt -le $max_attempts ]; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost/api/categories 2>/dev/null || true)
    if [ "$code" = "200" ]; then
      echo ">>> API 已就绪（第 ${attempt} 次检查）"
      return 0
    fi
    [ $attempt -eq $max_attempts ] && echo ">>> 警告: 超时未就绪，请稍后手动检查"
    attempt=$((attempt + 1))
    sleep 15
  done
}

rolling_update() {
  local services=("$@")
  [ ${#services[@]} -eq 0 ] && return 0

  echo ">>> [构建] 构建镜像（现有服务继续运行）: ${services[*]}"
  $COMPOSE_CMD build "${services[@]}"

  echo ">>> [替换] 逐个更新容器..."
  for svc in "${services[@]}"; do
    echo "    ↻ $svc"
    $COMPOSE_CMD up -d --no-deps "$svc"
  done
}

usage() {
  echo "HMall 部署脚本"
  echo ""
  echo "用法: $0 <command> [service...]"
  echo ""
  echo "命令:"
  echo "  up           构建并启动所有服务（首次部署）"
  echo "  down         停止并移除所有容器"
  echo "  restart      重启所有服务"
  echo "  logs         查看日志（可指定服务名，Ctrl+C 退出）"
  echo "  status       查看所有服务运行状态"
  echo "  pull         拉取最新代码并智能部署（只更新变更的服务）"
  echo "  update       更新指定服务: deploy.sh update catalog-service bff-web"
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
  update)
    shift
    if [ $# -eq 0 ]; then
      echo "错误: 请指定要更新的服务"
      echo "用法: $0 update <service...>"
      echo "示例: $0 update catalog-service bff-web"
      exit 1
    fi
    rolling_update "$@"
    wait_for_api
    echo ""
    echo ">>> 更新完成，服务状态："
    $COMPOSE_CMD ps
    ;;
  pull)
    echo ">>> 拉取最新代码..."
    cd "$PROJECT_ROOT"
    OLD_COMMIT=$(git rev-parse HEAD)
    git pull
    NEW_COMMIT=$(git rev-parse HEAD)

    if [ "$OLD_COMMIT" = "$NEW_COMMIT" ]; then
      echo ">>> 代码已是最新，无需部署"
      $COMPOSE_CMD ps
      exit 0
    fi

    CHANGED=$(git diff --name-only "$OLD_COMMIT" "$NEW_COMMIT")

    # --- Detect what needs rebuilding ---
    REBUILD_BACKEND=()
    REBUILD_NGINX=false
    REBUILD_MCP=false
    REBUILD_ALL_BACKEND=false

    if echo "$CHANGED" | grep -qE '^deploy/Dockerfile\.service'; then
      REBUILD_ALL_BACKEND=true
    fi
    if echo "$CHANGED" | grep -qE '^deploy/docker-compose'; then
      REBUILD_ALL_BACKEND=true
      REBUILD_NGINX=true
      REBUILD_MCP=true
    fi

    if [ "$REBUILD_ALL_BACKEND" = true ]; then
      REBUILD_BACKEND=($SERVICE_ORDER)
    else
      for svc in $SERVICE_ORDER; do
        if echo "$CHANGED" | grep -q "^services/${svc}/"; then
          REBUILD_BACKEND+=("$svc")
        fi
      done
    fi

    if echo "$CHANGED" | grep -qE '^(frontend/|deploy/Dockerfile\.nginx|deploy/nginx/)'; then
      REBUILD_NGINX=true
    fi
    if echo "$CHANGED" | grep -qE '^(hmall-mcp/|deploy/Dockerfile\.mcp|docs/ontology/)'; then
      REBUILD_MCP=true
    fi

    COUNT=${#REBUILD_BACKEND[@]}
    [ "$REBUILD_NGINX" = true ] && COUNT=$((COUNT + 1))
    [ "$REBUILD_MCP" = true ] && COUNT=$((COUNT + 1))

    if [ $COUNT -eq 0 ]; then
      echo ">>> 变更不涉及可部署服务（仅文档/脚本等），跳过部署"
      exit 0
    fi

    echo ">>> 检测到 ${COUNT} 个服务需要更新"

    # Phase 1+2: Build then rolling restart backend
    if [ ${#REBUILD_BACKEND[@]} -gt 0 ]; then
      echo ">>> 后端服务: ${REBUILD_BACKEND[*]}"
      rolling_update "${REBUILD_BACKEND[@]}"
    fi

    # Phase 3: MCP
    if [ "$REBUILD_MCP" = true ]; then
      echo ">>> 更新 hmall-mcp..."
      rolling_update hmall-mcp
    fi

    # Phase 4: Nginx last (backend already up)
    if [ "$REBUILD_NGINX" = true ]; then
      echo ">>> 更新 nginx（含前端）..."
      rolling_update nginx
    fi

    wait_for_api
    echo ""
    echo ">>> 部署完成，服务状态："
    $COMPOSE_CMD ps
    ;;
  *)
    usage
    ;;
esac
