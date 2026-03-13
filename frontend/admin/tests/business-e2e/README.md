# Frontend Admin Business E2E

管理后台（frontend/admin）的业务验收 E2E 测试。

## 运行

```bash
# 在 frontend/admin 目录下
npm run test:business:e2e
npm run test:business:e2e:list
```

## 前置条件

- **后端服务可用**：BFF (8085)、Catalog (8080) 等。需包含镭雕图案 API（`/api/engraving-patterns`）。
- 若刚更新 BFF 路由或 Catalog 代码，请先重启：`./scripts/hmall.sh restart bff-web catalog-service`
- 测试会启动 admin 开发服务器（默认端口 5192），或复用已启动的实例

## 用例

| 用例 | 说明 |
|------|------|
| BIZ-LE-001 | 图案库：进入页面 -> 新增图案 -> 列表展示 |
| BIZ-LE-002 | 图案库：编辑图案 -> 列表更新 |
| BIZ-LE-003 | 图案库：删除图案 -> 列表移除 |
| BIZ-IO1-001 | 活动监控多维查询：输入 orderId 查询 -> 展示结果或空态（智能运营 Step 1） |
