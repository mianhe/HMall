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
ALL_COMPONENTS="db catalog-service user-service order-service inventory-service payment-service activity-service bff-web frontend-admin frontend-web mcp"

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

status_catalog_service() {
  if is_port_listen 8080; then
    echo "  catalog-service   up      http://127.0.0.1:8080"
  else
    echo "  catalog-service   down    -"
  fi
}

status_user_service() {
  if is_port_listen 8082; then
    echo "  user-service   up      http://127.0.0.1:8082"
  else
    echo "  user-service   down    -"
  fi
}

status_order_service() {
  if is_port_listen 8081; then
    echo "  order-service  up      http://127.0.0.1:8081"
  else
    echo "  order-service  down    -"
  fi
}

status_inventory_service() {
  if is_port_listen 8083; then
    echo "  inventory-service   up      http://127.0.0.1:8083"
  else
    echo "  inventory-service   down    -"
  fi
}

status_payment_service() {
  if is_port_listen 8084; then
    echo "  payment-service   up      http://127.0.0.1:8084"
  else
    echo "  payment-service   down    -"
  fi
}

status_activity_service() {
  if is_port_listen 8086; then
    echo "  activity-service   up      http://127.0.0.1:8086"
  else
    echo "  activity-service   down    -"
  fi
}

status_bff_web() {
  if is_port_listen 8085; then
    echo "  bff-web       up      http://127.0.0.1:8085"
  else
    echo "  bff-web       down    -"
  fi
}

status_frontend_admin() {
  if is_port_listen 5173; then
    echo "  frontend-admin   up      http://127.0.0.1:5173"
  else
    echo "  frontend-admin   down    -"
  fi
}

status_frontend_web() {
  if is_port_listen 5174; then
    echo "  frontend-web     up      http://127.0.0.1:5174"
  else
    echo "  frontend-web     down    -"
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
  status_catalog_service
  status_user_service
  status_order_service
  status_inventory_service
  status_payment_service
  status_activity_service
  status_bff_web
  status_frontend_admin
  status_frontend_web
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
  # 端口监听后 PostgreSQL 可能仍在初始化，稍等再让后端连接，避免交替 500
  sleep 2
  echo "Waiting for Kafka (9092)..."
  wait_for_port 9092 "Kafka" || true
  sleep 2
  echo "DB started."
}

stop_db() {
  echo "Stopping DB..."
  docker compose -f "$COMPOSE_FILE" down
  echo "DB stopped."
}

# ---------- Catalog Service ----------
start_catalog_service() {
  if is_port_listen 8080; then
    echo "Catalog-service already running on 8080."
    return 0
  fi
  echo "Starting catalog-service..."
  (cd "${ROOT}/services/catalog-service" && mvn spring-boot:run >> "${LOG_DIR}/catalog-service.log" 2>&1 &)
  echo $! > "${PID_DIR}/catalog-service.pid"
  echo "Waiting for catalog-service (8080)..."
  wait_for_port 8080 "catalog-service" || true
  sleep 3
  echo "Catalog-service started."
}

start_user_service() {
  if is_port_listen 8082; then
    echo "User-service already running on 8082."
    return 0
  fi
  echo "Starting user-service..."
  (cd "${ROOT}/services/user-service" && mvn spring-boot:run >> "${LOG_DIR}/user-service.log" 2>&1 &)
  echo $! > "${PID_DIR}/user-service.pid"
  echo "Waiting for user-service (8082)..."
  wait_for_port 8082 "user-service" || true
  sleep 2
  echo "User-service started."
}

start_order_service() {
  if is_port_listen 8081; then
    echo "Order-service already running on 8081."
    return 0
  fi
  echo "Starting order-service..."
  (cd "${ROOT}/services/order-service" && mvn spring-boot:run >> "${LOG_DIR}/order-service.log" 2>&1 &)
  echo $! > "${PID_DIR}/order-service.pid"
  echo "Waiting for order-service (8081)..."
  if ! wait_for_port 8081 "order-service"; then
    echo "Order-service may have failed to start. Last 20 lines of ${LOG_DIR}/order-service.log:" >&2
    tail -20 "${LOG_DIR}/order-service.log" 2>/dev/null || true
  fi
  sleep 2
  echo "Order-service started."
}

start_inventory_service() {
  if is_port_listen 8083; then
    echo "Inventory-service already running on 8083."
    return 0
  fi
  echo "Starting inventory-service..."
  (cd "${ROOT}/services/inventory-service" && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083 >> "${LOG_DIR}/inventory-service.log" 2>&1 &)
  echo $! > "${PID_DIR}/inventory-service.pid"
  echo "Waiting for inventory-service (8083)..."
  wait_for_port 8083 "inventory-service" || true
  sleep 2
  echo "Inventory-service started."
}

start_payment_service() {
  if is_port_listen 8084; then
    echo "Payment-service already running on 8084."
    return 0
  fi
  echo "Starting payment-service..."
  (cd "${ROOT}/services/payment-service" && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8084 >> "${LOG_DIR}/payment-service.log" 2>&1 &)
  echo $! > "${PID_DIR}/payment-service.pid"
  echo "Waiting for payment-service (8084)..."
  wait_for_port 8084 "payment-service" || true
  sleep 2
  echo "Payment-service started."
}

start_activity_service() {
  if is_port_listen 8086; then
    echo "Activity-service already running on 8086."
    return 0
  fi
  echo "Starting activity-service..."
  (cd "${ROOT}/services/activity-service" && mvn spring-boot:run >> "${LOG_DIR}/activity-service.log" 2>&1 &)
  echo $! > "${PID_DIR}/activity-service.pid"
  echo "Waiting for activity-service (8086)..."
  wait_for_port 8086 "activity-service" || true
  sleep 2
  echo "Activity-service started."
}

start_bff_web() {
  if is_port_listen 8085; then
    echo "Bff-web already running on 8085."
    return 0
  fi
  echo "Starting bff-web..."
  (cd "${ROOT}/services/bff-web" && mvn spring-boot:run >> "${LOG_DIR}/bff-web.log" 2>&1 &)
  echo $! > "${PID_DIR}/bff-web.pid"
  echo "Waiting for bff-web (8085)..."
  wait_for_port 8085 "bff-web" || true
  sleep 2
  echo "Bff-web started."
}

stop_user_service() {
  local pid_file="${PID_DIR}/user-service.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping user-service (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 8082; then
    local p
    p=$(lsof -i :8082 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8082 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "User-service stopped."
}

stop_order_service() {
  local pid_file="${PID_DIR}/order-service.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping order-service (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 8081; then
    local p
    p=$(lsof -i :8081 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8081 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Order-service stopped."
}

stop_inventory_service() {
  local pid_file="${PID_DIR}/inventory-service.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping inventory-service (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 8083; then
    local p
    p=$(lsof -i :8083 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8083 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Inventory-service stopped."
}

stop_payment_service() {
  local pid_file="${PID_DIR}/payment-service.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping payment-service (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 8084; then
    local p
    p=$(lsof -i :8084 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8084 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Payment-service stopped."
}

stop_catalog_service() {
  local pid_file="${PID_DIR}/catalog-service.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping catalog-service (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 8080; then
    local p
    p=$(lsof -i :8080 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8080 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Catalog-service stopped."
}

stop_activity_service() {
  local pid_file="${PID_DIR}/activity-service.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping activity-service (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 8086; then
    local p
    p=$(lsof -i :8086 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8086 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Activity-service stopped."
}

stop_bff_web() {
  local pid_file="${PID_DIR}/bff-web.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping bff-web (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 8085; then
    local p
    p=$(lsof -i :8085 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      echo "Killing process on 8085 (PID $p)..."
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Bff-web stopped."
}

# ---------- Frontend Admin ----------
start_frontend_admin() {
  if is_port_listen 5173; then
    echo "Frontend-admin already running on 5173."
    return 0
  fi
  echo "Starting frontend-admin..."
  (cd "${ROOT}/frontend-admin" && npm run dev >> "${LOG_DIR}/frontend-admin.log" 2>&1 &)
  echo $! > "${PID_DIR}/frontend-admin.pid"
  echo "Waiting for frontend-admin (5173)..."
  wait_for_port 5173 "frontend-admin" || true
  echo "Frontend-admin started."
}

stop_frontend_admin() {
  local pid_file="${PID_DIR}/frontend-admin.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping frontend-admin (PID $pid)..."
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
  echo "Frontend-admin stopped."
}

# ---------- Frontend Web ----------
start_frontend_web() {
  if is_port_listen 5174; then
    echo "Frontend-web already running on 5174."
    return 0
  fi
  echo "Starting frontend-web..."
  (cd "${ROOT}/frontend-web" && npm run dev >> "${LOG_DIR}/frontend-web.log" 2>&1 &)
  echo $! > "${PID_DIR}/frontend-web.pid"
  echo "Waiting for frontend-web (5174)..."
  wait_for_port 5174 "frontend-web" || true
  echo "Frontend-web started."
}

stop_frontend_web() {
  local pid_file="${PID_DIR}/frontend-web.pid"
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping frontend-web (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      sleep 2
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
  if is_port_listen 5174; then
    local p
    p=$(lsof -i :5174 -sTCP:LISTEN -t 2>/dev/null | head -1)
    if [ -n "$p" ]; then
      kill "$p" 2>/dev/null || kill -9 "$p" 2>/dev/null || true
    fi
  fi
  echo "Frontend-web stopped."
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
      catalog-service) start_catalog_service ;;
      user-service) start_user_service ;;
      order-service) start_order_service ;;
      inventory-service) start_inventory_service ;;
      payment-service) start_payment_service ;;
      activity-service) start_activity_service ;;
      bff-web) start_bff_web ;;
      frontend-admin) start_frontend_admin ;;
      frontend-web) start_frontend_web ;;
      mcp) start_mcp ;;
      *) echo "Unknown component: $c" >&2 ;;
    esac
  done
}

run_stop() {
  local components="$*"
  [ -z "$components" ] && components="mcp frontend-web frontend-admin bff-web activity-service payment-service inventory-service order-service user-service catalog-service db"
  for c in $components; do
    case "$c" in
      db) stop_db ;;
      catalog-service) stop_catalog_service ;;
      user-service) stop_user_service ;;
      order-service) stop_order_service ;;
      inventory-service) stop_inventory_service ;;
      payment-service) stop_payment_service ;;
      activity-service) stop_activity_service ;;
      bff-web) stop_bff_web ;;
      frontend-admin) stop_frontend_admin ;;
      frontend-web) stop_frontend_web ;;
      mcp) stop_mcp ;;
      *) echo "Unknown component: $c" >&2 ;;
    esac
  done
}

# ---------- test ----------
SUMMARY_DIR="${ROOT}/.hmall/test-results"

# 解析 Maven 日志中的 "Tests run: N, Failures: F, Errors: E"，输出汇总表
print_test_summary() {
  echo ""
  echo "=========================================="
  echo "  HMall 测试汇总"
  echo "=========================================="
  local total_run=0 total_fail=0 total_err=0 any_fail=0
  shopt -s nullglob 2>/dev/null || true
  for log in "${SUMMARY_DIR}"/*.log; do
    [ -f "$log" ] || continue
    local key
    key=$(basename "$log" .log)
    local name
    case "$key" in
      catalog-service) name="Catalog" ;;
      user-service) name="User" ;;
      order-service) name="Order" ;;
      inventory-service) name="Inventory" ;;
      payment-service) name="Payment" ;;
      activity-service) name="Activity" ;;
      *) name="$key" ;;
    esac
    local run fail err
    run=$(grep -o 'Tests run: [0-9]*' "$log" 2>/dev/null | tail -1 | grep -o '[0-9]*')
    fail=$(grep -o 'Failures: [0-9]*' "$log" 2>/dev/null | tail -1 | grep -o '[0-9]*')
    err=$(grep -o 'Errors: [0-9]*' "$log" 2>/dev/null | tail -1 | grep -o '[0-9]*')
    run=${run:-0}; fail=${fail:-0}; err=${err:-0}
    total_run=$((total_run + run))
    total_fail=$((total_fail + fail))
    total_err=$((total_err + err))
    [ "$fail" -gt 0 ] || [ "$err" -gt 0 ] && any_fail=1
    local passed=$((run - fail - err))
    printf "  %-22s %3d passed, %d failed, %d errors\n" "$name" "$passed" "$fail" "$err"
  done
  echo "------------------------------------------"
  local total_passed=$((total_run - total_fail - total_err))
  printf "  %-22s %3d passed, %d failed, %d errors\n" "Total" "$total_passed" "$total_fail" "$total_err"
  echo "=========================================="
  return $any_fail
}

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
          echo "Error: --bc requires a value (catalog|user|order|inventory|payment|all)" >&2
          exit 1
        fi
        case "$1" in
          catalog) bc_filter="@catalog" ;;
          user) bc_filter="@user" ;;
          order) bc_filter="@order" ;;
          inventory) bc_filter="@inventory" ;;
          payment) bc_filter="@payment" ;;
          activity) bc_filter="@activity" ;;
          all) bc_filter="" ;;
          *)
            echo "Error: --bc must be catalog, user, order, inventory, payment, activity, or all" >&2
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

  mkdir -p "$SUMMARY_DIR"
  rm -f "${SUMMARY_DIR}"/*.log

  local mvn_opts=""
  [ -n "$bc_filter" ] && mvn_opts="$mvn_opts -Dcucumber.filter.tags=$bc_filter"

  local overall_rc=0
  run_service_test() {
    local dir=$1
    local name="${2:-$dir}"
    local log="${SUMMARY_DIR}/${dir}.log"
    echo ""
    echo "========== Running $name tests =========="
    (cd "${ROOT}/services/${dir}" && \
      if [ $clean -eq 1 ]; then mvn clean test $mvn_opts; \
      elif [ $cucumber_only -eq 1 ]; then mvn test -Dtest=RunCucumberTest $mvn_opts; \
      else mvn test $mvn_opts; fi) 2>&1 | tee "$log"
    [ "${PIPESTATUS[0]}" -ne 0 ] && overall_rc=1 || true
  }

  if [ "$bc_filter" = "@order" ]; then
    run_service_test "order-service" "order-service (Order)"
  elif [ "$bc_filter" = "@catalog" ]; then
    run_service_test "catalog-service" "Catalog"
  elif [ "$bc_filter" = "@user" ]; then
    run_service_test "user-service" "user-service (User)"
  elif [ "$bc_filter" = "@inventory" ]; then
    run_service_test "inventory-service" "inventory-service (Inventory)"
  elif [ "$bc_filter" = "@payment" ]; then
    run_service_test "payment-service" "payment-service (Payment)"
  elif [ "$bc_filter" = "@activity" ]; then
    run_service_test "activity-service" "activity-service (Activity)"
  else
    # 全部微服务：catalog-service、user-service、order-service、inventory-service、payment-service
    run_service_test "catalog-service" "Catalog"
    run_service_test "user-service" "user-service (User)"
    run_service_test "order-service" "order-service (Order)"
    run_service_test "inventory-service" "inventory-service (Inventory)"
    run_service_test "payment-service" "payment-service (Payment)"
    run_service_test "activity-service" "activity-service (Activity)"
    print_test_summary || overall_rc=1
  fi

  exit $overall_rc
}

# ---------- seed-inventory ----------
# 为指定 skuId 设置可用库存（经 BFF 调用），便于提交订单。不传参数时默认 1～20。
seed_inventory() {
  local sku_ids="${*:-}"
  if [ -z "$sku_ids" ]; then
    sku_ids="1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50"
  fi
  if ! is_port_listen 8085; then
    echo "BFF (8085) 未启动，请先执行: $0 start bff-web" >&2
    return 1
  fi
  echo "为 SKU 设置库存（经 BFF）..."
  for sku in $sku_ids; do
    if curl -s -o /dev/null -w "%{http_code}" -X PUT "http://127.0.0.1:8085/api/inventory/stock/${sku}" \
      -H "Content-Type: application/json" -d '{"available":99}' | grep -qE '^200|^201'; then
      echo "  skuId ${sku} -> available 99"
    fi
  done
  echo "完成。若商品仍提示库存不足，请在管理后台 http://localhost:5173 库存页为对应 SKU 设置可用数量。"
}

# ---------- main ----------
usage() {
  echo "Usage: $0 <command> [options] [components]"
  echo "  command:  start | stop | status | restart | test | seed-inventory"
  echo "  components: db | catalog-service | user-service | order-service | inventory-service | payment-service | activity-service | bff-web | frontend-admin | frontend-web | mcp (default: all for start/stop/restart)"
  echo "  seed-inventory: 可选 skuId 列表，不传则对 1～50 设置 available=99"
  echo "  test options: [--cucumber-only] [--clean] [--bc catalog|user|order|inventory|payment|activity|all]"
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
  seed-inventory)
    shift
    seed_inventory "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac
