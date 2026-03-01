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
领域模型 ────────指导和约束────────→ 代码实现（四层架构）
```

**系统级**：业务需求（跨 BC）、上下文地图（`context-map.md`）、系统级事件流、端到端测试。
**前端**：消费后端 API 契约；持有界面规格（`ui-spec.md`）和代码实现。
**端到端测试**：使用前端验证系统级业务链路；包含冒烟测试和业务需求测试。

## 方法论与约定

- **问题域**：事件流分析 → 领域建模 → 特性编写（详见 `.cursor/skills/README.md`）
- **解决方案域**：验收测试（`.feature`）→ 先红后绿 → 集成 → 前端
- **架构与实现约定**：`docs/design-principles.md`

## 项目结构

```
docs/
├── project-status.md              # 【必读】项目进度与路线图
├── context-map.md                 # 系统结构（BC 总览、集成关系、集成技术）
├── business-flows.md              # 业务流程（价值流、事件流、路径枚举、测试覆盖）
├── design-principles.md           # 架构与实现约定
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

## ⚠️ 开发约束（不可绕过）

以下三条约束适用于**所有**涉及业务逻辑的变更，无论起因是需求讨论、bug 修复、重构还是对话中的临时决策。纯 UI 调整（样式、文案、布局）不涉及业务逻辑变更，可以直接改。

### 约束一：必须走 Skill 流程

任何涉及领域模型、特性、验收测试或 API 契约的变更，都必须通过对应的 Skill 执行。**禁止在对话中"顺手改代码"。**

当对话中产生了变更决策时：
1. 先明确该变更属于哪个 Skill（见下方「选择正确的 Skill」）
2. **读取并执行该 Skill 的完整流程**（从文档更新开始，而非从代码开始）

### 约束二：文档先于代码，传导链不可断

变更的传导链为：**决策 → 特性文档 → 验收测试 → 代码实现**，每一环都不可跳过。

- 引入或修正业务规则时，先更新特性（`requirements.md`）
- 涉及行为变更时，先在 `.feature` 中写出期望场景（红），再改代码（绿）
- 业务逻辑归后端：前端只渲染后端 API 返回的结果，不做业务判断

### 约束三：实现从域对象开始，自内向外

后端代码修改必须遵循 DDD 分层依赖方向（详见 `docs/design-principles.md` §2.4a）：

**域对象 → 仓储接口/实现 → 应用服务 → API/DTO**

禁止从外层（DTO/Entity）开始改然后"回头补"域对象——这是类型不一致和遗漏的主要来源。

## 选择正确的 Skill

```
需要做什么？
├── 分析业务需求（新建 BC / 重大演进 / 跨 BC）→ analyze-requirement
├── 演进一个特性（已知改哪个 BC 的哪个 Feature）→ evolve-feature
├── 修 bug 或小幅调整  → fix-bug-or-adjust-feature
├── 对接两个 BC        → integration
└── 前端页面           → frontend-development
```

Skill 详细说明与组合方式见 `.cursor/skills/README.md`。

## 对话语言

优先使用中文。
