# Frontend Admin Testing Guide

本文档定义 `frontend/admin` 的前端测试体系。当前仅包含 **Business E2E**（关键业务需求验收）。

> 与 `docs/design-principles.md` §3.5 和 `.cursor/skills/frontend-development/SKILL.md` 协同。

## 1. 测试目标

| 层 | 定位 | 何时运行 |
|----|------|---------|
| **Business E2E** | 关键业务需求前端验收 | 需求开发收尾时编写并运行；后续按需回归 |

管理后台暂无 Smoke E2E 基线；新增管理功能时编写 Business E2E 验收。

## 2. 断言深度

- 验证页面能到达、关键操作能执行、最终结果正确
- 不验证 UI 细节（标签颜色、缩进、具体文案）

## 3. 目录结构

```
frontend/admin/tests/
└── business-e2e/
    ├── specs/
    │   └── engraving-pattern/
    │       └── engraving-pattern.spec.cjs
    ├── pages/
    │   └── engraving-pattern.page.cjs
    ├── playwright.config.cjs
    └── README.md
```

## 4. 用例清单

| 编号 | 业务需求 | 描述 |
|------|---------|------|
| BIZ-LE-001 | 镭雕图案库 | 进入页面 -> 新增图案 -> 列表展示 |
| BIZ-LE-002 | 镭雕图案库 | 编辑图案 -> 列表更新 |
| BIZ-LE-003 | 镭雕图案库 | 删除图案 -> 列表移除 |

## 5. 运行方式

```bash
cd frontend/admin
npm run test:business:e2e
npm run test:business:e2e:list
```

## 6. 前置条件

- 后端服务可用（BFF 8085、Catalog 等），`/api` 代理可用
- 本机安装 Chrome
- 仓库根或 `frontend/admin` 已安装 npm 依赖（含 @playwright/test）
