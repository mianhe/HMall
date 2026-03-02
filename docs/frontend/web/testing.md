# Frontend Web Testing Guide

本文档定义 `frontend/web` 的前端测试体系。分两层：**Smoke E2E**（全局链路守护）和 **Business E2E**（关键业务需求验收）。

> 本文档与 `docs/design-principles.md` §3.5 和 `.cursor/skills/frontend-development/SKILL.md` 步骤 3 协同。
> 链路依据：Smoke / Business E2E 的用例设计基于 [业务流程全景](../../business-flows.md)（路径枚举与测试覆盖映射）。

## 1. 测试目标与分层

| 层 | 定位 | 何时运行 | 生命周期 |
|----|------|---------|---------|
| **Smoke P0** | 核心交易链路守护 | 每次前端变更（自动化基线） | Evergreen |
| **Smoke P1** | 重要非核心链路守护 | 按需手工运行 | Evergreen |
| **Business E2E** | 关键业务需求前端验收 | 需求开发收尾时编写并运行；后续按需回归 | 非 Evergreen（允许腐化） |

三层关系：
- **Smoke P0** 是每次变更的**必跑自动化基线**，保证核心交易链路可用（能下单）。
- **Smoke P1** 是重要但非核心的链路守护，由开发者**按需手工运行**，不纳入自动化必跑。
- **Business E2E** 是**需求验收工具**，在该需求前端开发完成后编写并运行，确认关键链路可走通，之后保留但不纳入常规回归。
- 三层共享 Page Object / Fixture / Helper（位于 `tests/shared/`）。

### 断言深度原则

两层的断言深度**一致**——都只验证"链路能走通"：
- 验证页面能到达、关键操作能执行、最终结果正确（如订单创建成功）
- **不验证** UI 细节（标签颜色、缩进、具体文案）
- **不验证**每个业务规则

Smoke 和 Business E2E 的区别不在于断言深度，而在于**覆盖的业务路径数量**。

## 2. Smoke E2E 分级

### P0 / P1 分级

| 级别 | 定位 | 判断标准 |
|------|------|---------|
| **P0** | 核心交易链路（没它系统等于不可用） | 用户最基础的花钱路径 |
| **P1** | 重要但非核心（特定功能不可用但系统仍可用） | 新增业务交易链路 |

用例标题中用 `@P0` / `@P1` 标签标注，支持通过 `--grep` 过滤运行。

### 运行策略

| 范围 | 何时运行 | 时间预算 | 硬上限 |
|------|---------|---------|--------|
| **仅 P0**（自动化基线） | 每次前端变更必跑 | ≤ 1 分钟 | ≤ 2 分钟 |
| P0 + P1（全量） | 按需手工运行 | ≤ 2 分钟 | ≤ 5 分钟 |

- P0 是自动化基线，**永不降级**
- P1 不纳入自动化必跑，由开发者按需运行 `npm run test:smoke:e2e`

### 新链路入 Smoke 的判断

对照 [业务流程全景](../../business-flows.md) §四 路径枚举与测试覆盖：

```
新的业务链路出现
  → 更新 business-flows.md 路径枚举
  → 它是交易主链路吗？（用户花钱的路径）
    → 是 → 进 Smoke（P0 或 P1）
    → 否 → 只写 Business E2E
  → 更新 business-flows.md 测试覆盖列
```

## 3. Business E2E 策略

### 编写时机

Business E2E 在**需求前端开发完成后、标记完成之前**编写：

```
后端 BC 开发完成（Cucumber 全绿）
  → 前端实现
    → npm run build 通过
    → npm run test:smoke:e2e 全绿
    → 编写该需求的 Business E2E        ← 此时编写
    → npm run test:business:e2e 全绿
    → 需求标记完成
```

### 生命周期（非 Evergreen）

- **写完跑绿后保留**，代码留在仓库
- **不纳入常规回归基线**——日常不跑
- **按需回归**：当变更影响到某个需求时，手动跑该需求的 Business E2E
- **允许腐化**：长期不维护导致跑不过，不视为阻塞（Smoke 已守住核心链路）
- **新需求来了写新的**，不修改旧的 Business E2E；旧链路被根本性改变时可删除

这与后端 BC 的 Feature 文件（Evergreen）**故意不一致**，因为前端 E2E 的维护成本远高于后端 Cucumber。

### 何为"关键业务需求"

触发条件：**跨 BC 的业务需求引入了前端可感知的新交易链路**。

| 示例 | 是否需要 |
|------|---------|
| 虚拟商品随购（新的下单路径） | 是 |
| 修改按钮颜色、调整布局 | 否 |
| 在已有链路增加一个展示字段 | 否 |

## 4. 完成标准（质量门禁）

### 每次前端变更

| 步骤 | 命令 | 要求 |
|------|------|------|
| 构建 | `npm run build` | 无编译错误 |
| Smoke P0 | `npm run test:smoke:e2e:p0` | P0 全绿 |

### 开发/变更关键业务需求时

除上述基线外：

| 步骤 | 命令 | 要求 |
|------|------|------|
| Business E2E | `npm run test:business:e2e -- --grep "BIZ-<需求>"` | 相关用例全绿 |

## 5. 目录结构

```
frontend/web/tests/
├── shared/                             # 共享层：Page Object / Fixture / Helper
│   ├── pages/
│   ├── fixtures/
│   └── helpers/
│
├── smoke-e2e/                          # Smoke E2E：全局链路守护（Evergreen）
│   ├── specs/
│   │   ├── smoke-mainline.spec.cjs           @P0  N2O-1
│   │   ├── smoke-cart-checkout.spec.cjs      @P0  N2O-2
│   │   └── smoke-virtual-product.spec.cjs    @P1  N2O-3
│   └── playwright.config.cjs
│
└── business-e2e/                       # Business E2E：需求验收（非 Evergreen）
    ├── specs/
    │   ├── virtual-product/
    │   │   ├── purchase-with-service.spec.cjs
    │   │   └── cart-service-grouping.spec.cjs
    │   └── supplementary-purchase/
    │       └── supplementary-purchase.spec.cjs
    └── playwright.config.cjs
```

## 6. 用例清单

### Smoke E2E

| 编号 | 级别 | 描述 | 覆盖路径 |
|------|------|------|----------|
| `SMOKE-E2E-001` | P0 | 直接购买主链路 | N2O-1 |
| `SMOKE-E2E-003` | P0 | 购物车结算主链路 | N2O-2 |
| `SMOKE-E2E-002` | P1 | 虚拟商品直接购买 | N2O-3 |

### Business E2E

| 编号 | 业务需求 | 描述 | 覆盖路径 |
|------|---------|------|----------|
| `BIZ-VP-001` | 虚拟商品 | 随购服务：选服务 → 下单 → 订单含服务明细 | N2O-3 |
| `BIZ-VP-002` | 虚拟商品 | 购物车含服务：加购 → 购物车可见 → 结算成功 | N2O-4 |
| `BIZ-SP-001` | 补购服务 | 已交付订单 → 可补购服务列表 → 补购下单 | N2O-5 |

> 路径编号定义见 [业务流程全景](../../business-flows.md) §四。

## 7. 命名规范

| 层 | 格式 | 示例 |
|----|------|------|
| Smoke E2E | `SMOKE-E2E-xxx @P级别 描述` | `SMOKE-E2E-001 @P0 主链路` |
| Business E2E | `BIZ-<需求缩写>-xxx 描述` | `BIZ-VP-001 随购服务` |

## 8. 运行方式

```bash
cd frontend/web
```

### Smoke E2E

```bash
npm run test:smoke:e2e:p0           # 仅 P0（每次变更必跑）
npm run test:smoke:e2e              # 全量 P0 + P1（按需手工运行）
npm run test:smoke:e2e:list         # 列出用例
npm run test:smoke:e2e:report       # 查看报告
npm run test:smoke:e2e:clean        # 清理产物
```

### Business E2E

```bash
npm run test:business:e2e                       # 全部（通常不需要）
npm run test:business:e2e -- --grep "BIZ-VP"    # 只跑虚拟商品需求
npm run test:business:e2e:list                  # 列出用例
```

## 9. 运行前置条件

- BFF 与核心后端服务可访问（前端 `/api` 代理可用）。
- 本机安装 Chrome（配置使用 Playwright `channel: "chrome"`）。
- 仓库根或 `frontend/web` 已安装 npm 依赖。

## 10. Smoke 维护策略

| 场景 | 动作 |
|------|------|
| 新增交易链路 | 先写 Business E2E，再评估是否提升进 Smoke（P0/P1） |
| P0 超时 | 优化执行效率（P0 永不降级） |
| UI 结构调整 | 仅改 `shared/pages/` |
| 功能下线 | 删除对应用例 |

## 11. 技术基础设施

| 技术 | 用途 |
|------|------|
| [Playwright](https://playwright.dev/) | 浏览器自动化，驱动真实 Chrome |
| Page Object Model | UI 选择器封装在 `shared/pages/`，业务步骤与 UI 解耦 |
| Playwright Fixtures | 登录态等前置条件，依赖注入 |
| `test.step` + Given/When/Then | 结构化测试步骤 |
| `@P0` / `@P1` 标签 | Smoke 分级，`--grep` 过滤运行 |
