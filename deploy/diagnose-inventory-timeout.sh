#!/bin/bash
# 库存管理页面超时诊断（只读操作，不修改任何代码）
# 用法：bash diagnose-inventory-timeout.sh [公网IP]
# 在服务器上执行：cd ~/hmall/deploy && bash diagnose-inventory-timeout.sh

set -e
HOST=${1:-localhost}

echo "========== 1. 容器状态 =========="
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "hmall|NAMES" || true

echo ""
echo "========== 2. 关键服务是否存在 =========="
for c in hmall-nginx hmall-bff hmall-catalog hmall-inventory; do
  if docker ps -a --format '{{.Names}}' | grep -q "^${c}$"; then
    status=$(docker inspect -f '{{.State.Status}}' "$c" 2>/dev/null || echo "unknown")
    echo "  $c: $status"
  else
    echo "  $c: 未找到"
  fi
done

echo ""
echo "========== 3. 从宿主机测试 API（通过 Nginx） =========="
echo "  GET /api/categories ..."
code=$(curl -s -o /tmp/categories.json -w "%{http_code}" --max-time 20 "http://${HOST}/api/categories" || echo "000")
echo "    HTTP $code"
if [ "$code" = "200" ]; then
  echo "    content length: $(wc -c < /tmp/categories.json)"
fi

echo "  GET /api/inventory/stock/1 (测试单条库存) ..."
code=$(curl -s -o /tmp/stock.json -w "%{http_code}" --max-time 20 "http://${HOST}/api/inventory/stock/1" || echo "000")
echo "    HTTP $code"
if [ "$code" = "200" ]; then
  cat /tmp/stock.json | head -c 200
  echo ""
elif [ "$code" != "200" ]; then
  cat /tmp/stock.json 2>/dev/null | head -5 || true
fi

echo ""
echo "========== 4. Docker 网络内直连（BFF、inventory、catalog） =========="
NET=$(docker inspect hmall-nginx -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' 2>/dev/null || echo "deploy_hmall-net")
run_curl() {
  docker run --rm --network "$NET" curlimages/curl:latest -s -o /dev/null -w "%{http_code}" --max-time 15 "$1" 2>/dev/null || echo "000"
}
echo "  BFF /api/categories: HTTP $(run_curl "http://hmall-bff:8085/api/categories")"
echo "  BFF /api/inventory/stock/1: HTTP $(run_curl "http://hmall-bff:8085/api/inventory/stock/1")"
echo "  inventory-service /api/inventory/stock/1: HTTP $(run_curl "http://hmall-inventory:8083/api/inventory/stock/1")"
echo "  catalog-service /api/categories: HTTP $(run_curl "http://hmall-catalog:8080/api/categories")"

echo ""
echo "========== 7. 最近 50 行 BFF 日志 =========="
docker logs hmall-bff --tail 50 2>&1 || true

echo ""
echo "========== 8. 最近 50 行 inventory-service 日志 =========="
docker logs hmall-inventory --tail 50 2>&1 || true

echo ""
echo "========== 诊断结束 =========="
