---
description: HMall 项目上下文，自动注入每次对话
globs:
alwaysApply: true
---

# HMall 项目上下文

## 这是什么项目

HMall 是一个 DDD + ATDD 驱动的电商系统练习项目。技术栈：Java 21 / Spring Boot 3 / PostgreSQL 16（测试用 H2）/ Vue 3 / Vite。

## 首要行动

**每次对话开始时，先读取 `docs/project-status.md` 获取最新项目进度和路线图。** 该文件记录了各 BC 的完成状态、下一步计划、前端进度和关键决策。

## 核心方法论

- **ATDD**：需求 → Gherkin `.feature` → Cucumber Step Definitions → 先红后绿
- **DDD**：限界上下文、四层分层（api → application → domain ← infrastructure）
- **前端**：需求优先、契约对齐；页面/组件结构见各前端 `design-input.md`（按需）
- 架构与前后端约定见 `docs/design-principles.md`

## 项目结构速查

```
docs/
├── project-status.md              # 【必读】项目进度与路线图
├── context-map.md                 # 上下文地图（BC 总览、集成关系）
├── design-principles.md           # 架构与实现约定
├── architecture/                  # 集成技术、事件分析方法
├── bounded-contexts/<context>/    # 各 BC 的 requirements / domain-model / api.yaml
├── frontend-admin/                # 管理后台需求、design-input（页面/组件结构）
└── frontend-web/                  # 消费者端需求、design-input（页面/组件结构）

services/
├── catalog-service/               # Catalog 微服务（com.hmall.catalog）
├── user-service/                  # User 微服务（com.hmall.user）
└── order-service/                 # Order 微服务（com.hmall.order）
frontend-admin/                    # Vue 3 管理后台
frontend-web/                      # Vue 3 消费者端
hmall-mcp/                         # MCP Server（AI 操作商品数据）

.cursor/skills/                    # Cursor Skills（新增 BC、新增 feature、前端开发等）
```

## 新增功能的标准流程

1. 新增 BC → 使用 `.cursor/skills/add-bounded-context/SKILL.md`
2. BC 内增加功能 → 使用 `.cursor/skills/evolve-feature/SKILL.md`
3. 前端开发 → 使用 `.cursor/skills/frontend-development/SKILL.md`（先 requirements，按需 design-input，再实现）

## 对话语言

优先使用中文。
