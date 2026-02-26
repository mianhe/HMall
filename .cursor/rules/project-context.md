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

## 开发领域模型（核心概念速览）

> 完整定义见 `.cursor/skills/domain-model.md`。所有 Skill 的输入、产出均使用下列概念。

**限界上下文（BC）** 是枢纽，持有两类资产：

| 问题域（定义业务） | 解决方案域（变为软件） |
|---|---|
| 领域模型 → `domain-model.md` | 验收测试 → `.feature` + StepDefinition |
| BC 级事件流 → `event-flow.md` | API 契约 → `api.yaml` |
| 特性（Feature/Scenario）→ `requirements.md` | 代码实现 → 四层架构 |

**驱动链条**（开发过程中各对象如何串联）：

```
特性 ──验收并守护──→ 验收测试 ──调用──→ API 契约
                                          ↓
领域模型 ──────────映射──────────→ 代码实现（四层架构）
```

**系统级**：业务需求（跨 BC）、上下文地图（`context-map.md`）、系统级事件流。
**前端**：消费后端 API 契约；持有界面规格（`ui-spec.md`）和代码实现。

## 方法论与约定

- **问题域**：事件流分析 → 领域建模 → 特性编写（详见 `.cursor/skills/README.md`）
- **解决方案域**：验收测试（`.feature`）→ 先红后绿 → 集成 → 前端
- **架构与实现约定**：`docs/design-principles.md`

## 项目结构

```
docs/
├── project-status.md              # 【必读】项目进度与路线图
├── context-map.md                 # 上下文地图（BC 总览、集成关系）
├── design-principles.md           # 架构与实现约定
├── architecture/                  # 集成技术、事件分析方法
├── bounded-contexts/<context>/    # 各 BC 的 requirements / domain-model / event-flow / api.yaml
├── frontend/admin/                # 管理后台界面规格（ui-spec）
└── frontend/web/                  # 消费者端界面规格（ui-spec）

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

frontend/admin/                    # Vue 3 管理后台
frontend/web/                      # Vue 3 消费者端
hmall-mcp/                         # MCP Server（AI 操作商品数据）
.cursor/skills/                    # Cursor Skills（README.md 含完整方法论与流程）
```

## ⚠️ 改代码前必须先过的检查

即使用户说「帮我改一下这段代码」，也**不要直接动手改**。先判断：

1. **业务逻辑归后端**：如果需要 `if/else` 决定「用哪份数据 / 走哪条规则」，那是业务逻辑，应由后端 API 提供结果，前端只渲染。
2. **测试先于实现**：涉及行为变更时，先在 `.feature` 中写出期望场景（红），再改代码（绿）。
3. **特性先于代码**：引入或修正业务规则时，先更新特性（`requirements.md`）。

纯 UI 调整（样式、文案、布局）不涉及业务逻辑变更，可以直接改。

## ⚠️ 后端逻辑改动后的强制自检

**每次修改后端 Java 代码中的行为逻辑后**（新增分支、改变条件、增删参数、调整流程），必须按顺序完成以下两轮检查：

### 第一轮：测试与特性覆盖

1. **`.feature` 场景**：是否有验收场景覆盖了新增/变更的行为？没有则补。
2. **`requirements.md`**：特性描述是否仍然准确？状态标记（✅/🔲）是否需要更新？
3. **领域模型文档**（`domain-model.md`）：实体、属性、行为、数据库表映射是否需要同步？

### 第二轮：清理与一致性

4. **代码清理**：本次改动是否引入了重复代码、未使用的参数、过长方法、命名不一致等坏味道？有则重构。
5. **文档一致性**：本次涉及的所有文档（`requirements.md`、`domain-model.md`、`api.yaml` 等）之间是否存在矛盾或过时描述？有则统一。

两轮检查完成后，在回复中简要说明每项的结论（需要/不需要及原因）。不允许跳过。

## 选择正确的 Skill

```
需要做什么？
├── 新建 BC 或重大演进 → analyze-requirement（然后 add-bounded-context → evolve-feature × N）
├── 演进一个特性       → evolve-feature
├── 修 bug 或小幅调整  → fix-bug-or-adjust-feature
├── 对接两个 BC        → integration
└── 前端页面           → frontend-development
```

Skill 详细说明与组合方式见 `.cursor/skills/README.md`。

## 对话语言

优先使用中文。
