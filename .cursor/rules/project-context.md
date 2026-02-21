---
description: HMall 项目上下文，自动注入每次对话
globs:
alwaysApply: true
---

# HMall 项目上下文

## 项目概述

HMall 是 DDD + ATDD 驱动的电商系统。技术栈：Java 21 / Spring Boot 3 / PostgreSQL 16（测试用 H2）/ Vue 3 / Vite / Kafka。

## 首要行动

**每次对话开始时，先读取 `docs/project-status.md` 获取最新项目进度和路线图。**

## 方法论与约定

- **问题域**：事件流分析 → 领域建模 → 需求编写（详见 `.cursor/skills/README.md`）
- **解决方案域**：验收场景（`.feature`）→ 先红后绿 → 集成 → 前端
- **架构与实现约定**：`docs/design-principles.md`

## 项目结构

```
docs/
├── project-status.md              # 【必读】项目进度与路线图
├── context-map.md                 # 上下文地图（BC 总览、集成关系）
├── design-principles.md           # 架构与实现约定
├── architecture/                  # 集成技术、事件分析方法
├── bounded-contexts/<context>/    # 各 BC 的 requirements / domain-model / event-flow / api.yaml
├── frontend-admin/                # 管理后台需求、design-input
└── frontend-web/                  # 消费者端需求、design-input

services/
├── catalog-service/               # 商品（Catalog）
├── user-service/                  # 用户（User）
├── order-service/                 # 订单（Order）
├── cart-service/                  # 购物车（Cart）
├── inventory-service/             # 库存（Inventory）
├── payment-service/               # 支付（Payment）
├── fulfillment-service/           # 履约（Fulfillment）
├── activity-service/              # 活动（Activity）
└── bff-web/                       # BFF — 消费者端统一 API 入口

frontend-admin/                    # Vue 3 管理后台
frontend-web/                      # Vue 3 消费者端
hmall-mcp/                         # MCP Server（AI 操作商品数据）
.cursor/skills/                    # Cursor Skills（README.md 含完整方法论与流程）
```

## ⚠️ 改代码前必须先过的检查

即使用户说「帮我改一下这段代码」，也**不要直接动手改**。先判断：

1. **业务逻辑归后端**：如果需要 `if/else` 决定「用哪份数据 / 走哪条规则」，那是业务逻辑，应由后端 API 提供结果，前端只渲染。
2. **测试先于实现**：涉及行为变更时，先在 `.feature` 中写出期望场景（红），再改代码（绿）。
3. **需求先于代码**：引入或修正业务规则时，先更新 `requirements.md`。

纯 UI 调整（样式、文案、布局）不涉及业务逻辑变更，可以直接改。

## 选择正确的 Skill

```
需要做什么？
├── 新建 BC 或重大演进 → analyze-requirement（然后 add-bounded-context → evolve-feature × N）
├── 实现一个功能       → evolve-feature
├── 修 bug 或小幅调整  → fix-bug-or-adjust-requirement
├── 对接两个 BC        → integration
└── 前端页面           → frontend-development
```

Skill 详细说明与组合方式见 `.cursor/skills/README.md`。

## 对话语言

优先使用中文。
