# HMall 质量守护机制

防止后端测试被悄悄破坏的三层防线。

## 架构概览

```
开发者改代码 → [Layer 1] Pre-push Hook → [Layer 2] GitHub Actions CI
      ↑                                            ↓
  AI 辅助改代码 ← [Layer 3] Cursor Rule          远端全量验证
```

| 层级 | 机制 | 触发时机 | 范围 | 耗时 |
|------|------|---------|------|------|
| Layer 1 | Pre-push Hook | `git push` 前 | 仅受影响的 BC | ~5-20s |
| Layer 2 | GitHub Actions CI | 推送/PR 到 main | 全量（10 BC 并行） | ~2-3min |
| Layer 3 | Cursor Rule | AI 改动 `services/**` 时 | 受影响的 BC | ~5-20s |

## Layer 1: Pre-push Hook（本地门禁）

### 原理

推送前自动检测本次改动涉及哪些服务目录，只运行受影响的 BC 测试。

- 改了 `services/order-service/` → 只跑 order-service 的测试
- 改了根 `pom.xml` → 跑全量测试
- 改了 `docs/` 或 `deploy/` → 跳过测试

### 安装

```bash
git config core.hooksPath .githooks
```

> 项目内所有开发者 clone 后只需执行一次这条命令即可激活。

### 日常使用

```bash
# 正常推送 — hook 自动运行
git push

# 紧急跳过（不推荐）
git push --no-verify
```

### 文件位置

`.githooks/pre-push`

## Layer 2: GitHub Actions CI（远端门禁）

### 原理

每次推送到 main 或创建 PR 时，自动触发全量 CI：

- **10 个后端服务并行测试**（matrix strategy），单个失败不阻塞其他服务
- **2 个前端构建检查**（web + admin）
- 失败时自动上传 surefire-reports 供排查

### 配置

`.github/workflows/ci.yml`

### PR 保护（推荐）

在 GitHub 仓库 Settings → Branches → Branch protection rules 中：

1. 选择 `main` 分支
2. 勾选 **Require status checks to pass before merging**
3. 添加 `ci-result` 为必须通过的 check

这样任何测试失败的代码都无法合入 main。

## Layer 3: Cursor Rule（AI 协作守护）

### 原理

当 AI 修改 `services/**` 下的文件时，自动触发规则提醒运行受影响 BC 的测试。AI 必须在测试全绿后才能报告改动完成。

### 文件位置

`.cursor/rules/test-after-change.mdc`

## 变更-服务映射规则

| 文件路径模式 | 受影响服务 | 测试命令 |
|------------|-----------|---------|
| `services/catalog-service/**` | Catalog | `cd services/catalog-service && mvn test -q` |
| `services/user-service/**` | User | `cd services/user-service && mvn test -q` |
| `services/order-service/**` | Order | `cd services/order-service && mvn test -q` |
| `services/inventory-service/**` | Inventory | `cd services/inventory-service && mvn test -q` |
| `services/payment-service/**` | Payment | `cd services/payment-service && mvn test -q` |
| `services/activity-service/**` | Activity | `cd services/activity-service && mvn test -q` |
| `services/cart-service/**` | Cart | `cd services/cart-service && mvn test -q` |
| `services/fulfillment-service/**` | Fulfillment | `cd services/fulfillment-service && mvn test -q` |
| `services/smart-interaction-service/**` | SmartInteraction | `cd services/smart-interaction-service && mvn test -q` |
| `services/bff-web/**` | BFF | `cd services/bff-web && mvn test -q` |
| `pom.xml`（根） | **全部** | `./scripts/hmall.sh test` |
| `docs/**`, `deploy/**`, `scripts/**` | 无 | 跳过 |

## 全量测试命令

```bash
# 全量测试（10 BC，约 65 秒）
./scripts/hmall.sh test

# 单个 BC 测试
./scripts/hmall.sh test --bc catalog
./scripts/hmall.sh test --bc order

# 直接 Maven（不依赖 Docker）
cd services/order-service && mvn test -q
```

## 故障排查

### Pre-push Hook 测试失败

```bash
# 查看哪个服务失败
git push 2>&1 | grep "✗"

# 单独运行失败服务的测试，查看详情
cd services/<failed-service> && mvn test
```

### CI 测试失败

1. 在 GitHub Actions 页面查看失败的 job
2. 下载 `test-results-<service>` artifact 查看 surefire-reports
3. 本地复现：`cd services/<service> && mvn test`
