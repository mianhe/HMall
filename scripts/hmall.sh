#!/usr/bin/env bash
# HMall 系统操作脚本：一键启动/停止/状态/重启/执行用例
# 在项目根目录执行：./scripts/hmall.sh <command> [options] [components]

set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

COMPOSE_FILE="${ROOT}/infra/docker-compose.yml"
CONTAINER_NAME="hmall-postgres"
PID_DIR="${ROOT}/.hmall/pids"
LOG_DIR="${ROOT}/.hmall/logs"
ALL_COMPONENTS="db backend frontend mcp"

# 确保目录存在
mkdir -p "$PID_DIR" "$LOG_DIR"

# ---------- 状态检测 ----------
is_port_listen() {
  local port=$1
  if command -v lsof >/dev/null 2>&1; then
    lsof -i ":$port" -sTCP:LISTEN -t >/dev/null 2>&1
  else
    nc -z 127.0.0.1 "$port" 2>/dev/null
  fi
}

status_db() {
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${CONTAINER_NAME}$"; then
    echo "  db       up      port 5432 (PostgreSQL)"
  else
    echo "  db       down    -"
  fi
}

status_backend() {
  if is_port_listen 8080; then
    echo "  backend  up      http://127.0.0.1:8080"
  else
    echo "  backend  down    -"
  fi
}

status_frontend() {
  if is_port_listen 5173; then
    echo "  frontend up      http://127.0.0.1:5173"
  else
    echo "  frontend down    -"
  fi
}

status_mcp() {
  if is_port_listen 3000; then
    echo "  mcp      up      http://127.0.0.1:3000/mcp"
  else
    echo "  mcp      down    -"
  fi
}

cmd_status() {
  echo "HMall components:"
  status_db
  status_backend
  status_frontend
  status_mcp
}

# ---------- 等待端口 ----------
wait_for_port() {
  local port=$1
  local name=${2:-"port $port"}
  local max=30
  local n=0
  while ! is_port_listen "$port"; do
    n=$((n + 1))
    if [ $n -ge $max ]; then
      echo "Timeout waiting for $name" >&2
      return 1
    fi
    sleep 1
  done
  return 0
}

# ---------- DB ----------
start_db() {
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${CONTAINER_NAME}$"; then
    echo "DB already running."
    return 0
  fi
  echo "Starting DB..."
  docker compose -f "$COMPOSE_FILE" up -d
  echo "Waiting for PostgreSQL (5432)..."
  wait_for_port 5432 "PostgreSQL" || true
  echo "DB started."
}

stop_db() {
  echo "Stopping DB..."
  docker compose -f "$COMPOSE_FILE" down
  echo "DB stopped."
}

# ---------- Backend ----------
start_backend() {
  if is_port_listen 8080; then
    echo "Backend already running on 8080."
    return 0
  fi
  echo "Starting backend..."
  (cd "${ROOT}/backend" && mvn spring-boot:run >> "${LOG_DIR}/backend.log" 2>&1 &)
  echo $! > "${PID_DIR}/backend.pid"
  echo "Waiting for backend (8080)..."
  wait_for_port 8080 "backend" || true
  echo "Backend started."
}

stop_backend() {
  local pid_file="${PID_DIR}/backend.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping backend (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  # 若 PID 文件不存在但端口被占用，尝试按端口杀（兼容未用本脚本启动的进程）
  if is_port_listen 8080; then
    local p
    p=$(lsof -i :8080 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8080 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Backend stopped."
}

# ---------- Frontend ----------
start_frontend() {
  if is_port_listen 5173; then
    echo "Frontend already running on 5173."
    return 0
  fi
  echo "Starting frontend..."
  (cd "${ROOT}/frontend" && npm run dev >> "${LOG_DIR}/frontend.log" 2>&1 &)
  echo $! > "${PID_DIR}/frontend.pid"
  echo "Waiting for frontend (5173)..."
  wait_for_port 5173 "frontend" || true
  echo "Frontend started."
}

stop_frontend() {
  local pid_file="${PID_DIR}/frontend.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping frontend (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 5173; then
    local p
    p=$(lsof -i :5173 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Frontend stopped."
}

# ---------- MCP ----------
start_mcp() {
  if is_port_listen 3000; then
    echo "MCP already running on 3000."
    return 0
  fi
  echo "Starting MCP Server..."
  (cd "${ROOT}/hmall-mcp" && npm run start:http >> "${LOG_DIR}/mcp.log" 2>&1 &)
  echo $! > "${PID_DIR}/mcp.pid"
  echo "Waiting for MCP (3000)..."
  wait_for_port 3000 "MCP" || true
  echo "MCP started."
}

stop_mcp() {
  local pid_file="${PID_DIR}/mcp.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping MCP (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 3000; then
    local p
    p=$(lsof -i :3000 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "MCP stopped."
}

# ---------- start/stop 分发 ----------
run_start() {
  local components="$*"
  [ -z "$components" ] && components=$ALL_COMPONENTS

  for c in $components; do
    case "$c" in
      db) start_db ;;
      backend) start_backend ;;
      frontend) start_frontend ;;
      mcp) start_mcp ;;
      *) echo "Unknown component: $c" >&2 ;;
    esac
  done
}

run_stop() {
  local components="$*"
  [ -z "$components" ] && components="mcp frontend backend db"
  for c in $components; do
    case "$c" in
      db) stop_db ;;
      backend) stop_backend ;;
      frontend) stop_frontend ;;
      mcp) stop_mcp ;;
      *) echo "Unknown component: $c" >&2 ;;
    esac
  done
}

# ---------- test ----------
cmd_test() {
  local cucumber_only=0
  local clean=0
  local bc_filter=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --cucumber-only) cucumber_only=1 ;;
      --clean) clean=1 ;;
      --bc)
        shift
        if [ -z "${1:-}" ]; then
          echo "Error: --bc requires a value (catalog|user|all)" >&2
          exit 1
        fi
        case "$1" in
          catalog) bc_filter="@catalog" ;;
          user) bc_filter="@user" ;;
          all) bc_filter="" ;;
          *)
            echo "Error: --bc must be catalog, user, or all" >&2
            exit 1
            ;;
        esac
        ;;
    esac
    shift
  done

  if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${CONTAINER_NAME}$"; then
    echo "DB not running. Starting DB..."
    start_db
  fi

  local mvn_opts=""
  [ -n "$bc_filter" ] && mvn_opts="$mvn_opts -Dcucumber.filter.tags=$bc_filter"

  (cd "${ROOT}/backend" && \
    if [ $clean -eq 1 ]; then mvn clean test $mvn_opts; \
    elif [ $cucumber_only -eq 1 ]; then mvn test -Dtest=RunCucumberTest $mvn_opts; \
    else mvn test $mvn_opts; fi)
}

# ---------- main ----------
usage() {
  echo "Usage: $0 <command> [options] [components]"
  echo "  command:  start | stop | status | restart | test"
  echo "  components: db | backend | frontend | mcp (default: all for start/stop/restart)"
  echo "  test options: [--cucumber-only] [--clean] [--bc catalog|user|all]"
  echo "See scripts/README.md for details."
}

case "${1:-}" in
  start)
    shift
    run_start "$@"
    ;;
  stop)
    shift
    run_stop "$@"
    ;;
  status)
    cmd_status
    ;;
  restart)
    shift
    run_stop "${*:-$ALL_COMPONENTS}"
    run_start "${*:-$ALL_COMPONENTS}"
    ;;
  test)
    shift
    cmd_test "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac
