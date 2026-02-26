# HMall 文档

所有文档统一放在 `docs/` 下，按职责分目录。限界上下文归入 `bounded-contexts/`，架构与方法论归入 `architecture/`。

## 文档结构

```
docs/
├── README.md                     # 本索引
├── context-map.md                # 上下文地图（BC 总览、集成关系、部署形态）
├── design-principles.md          # 系统设计原则（架构、分层、数据隔离、约定）
├── project-status.md             # 项目状态与路线图
├── architecture/
│   ├── integration.md            # 集成技术选型（REST、事件总线 Kafka）
│   ├── event-driven-business-analysis.md  # 事件驱动业务分析方法
│   └── ai-chat.md               # 智能对话交互系统设计（LLM + MCP）
├── business-requirements/                        # 跨 BC 业务需求的整体方案
│   └── <epic-name>/
│       └── overview.md           # 背景、事件流、设计决策、影响摘要、迭代计划
├── bounded-contexts/
│   ├── catalog/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   ├── api.yaml
│   │   └── sample-data/
│   ├── user/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   └── api.yaml
│   ├── order/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   ├── api.yaml
│   │   ├── event-flow.md
│   │   └── saga-design.md
│   ├── inventory/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   └── event-flow.md
│   ├── payment/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   ├── api.yaml
│   │   └── event-flow.md
│   ├── bff/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   └── api.yaml
│   ├── smart-interaction/
│   │   └── requirements.md
│   ├── activity/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   └── api.yaml
│   ├── cart/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   └── api.yaml
│   ├── fulfillment/
│   │   ├── domain-model.md
│   │   ├── requirements.md
│   │   ├── api.yaml
│   │   └── event-flow.md
│   └── ...
├── frontend/
│   ├── admin/
│   │   └── ui-spec.md
│   └── web/
│       └── ui-spec.md
```

## 文档索引

| 类型 | 文档 | 说明 |
|------|------|------|
| **系统总览** | [context-map.md](./context-map.md) | BC 边界、集成关系、各 BC 为独立微服务 |
| **业务需求方案** | `business-requirements/<name>/overview.md` | 跨 BC 业务需求的整体方案、设计决策、迭代计划 |
| **设计原则** | [design-principles.md](./design-principles.md) | DDD 分层、验收约定、文档与输入 |
| **项目进度** | [project-status.md](./project-status.md) | BC 路线图、前端进度、关键决策 |
| **集成技术** | [architecture/integration.md](./architecture/integration.md) | REST、事件总线（Kafka） |
| **事件分析方法** | [architecture/event-driven-business-analysis.md](./architecture/event-driven-business-analysis.md) | 事件流 → 领域建模 → Saga 设计 |
| **智能对话** | [architecture/ai-chat.md](./architecture/ai-chat.md) | LLM + MCP 对话式操作系统设计 |

## 各 BC 文档

| BC | 需求 | 领域模型 | API 契约 | 其他 |
|----|------|----------|----------|------|
| Catalog | [requirements.md](./bounded-contexts/catalog/requirements.md) | [domain-model.md](./bounded-contexts/catalog/domain-model.md) | [api.yaml](./bounded-contexts/catalog/api.yaml) | sample-data/ |
| User | [requirements.md](./bounded-contexts/user/requirements.md) | [domain-model.md](./bounded-contexts/user/domain-model.md) | [api.yaml](./bounded-contexts/user/api.yaml) | — |
| Order | [requirements.md](./bounded-contexts/order/requirements.md) | [domain-model.md](./bounded-contexts/order/domain-model.md) | [api.yaml](./bounded-contexts/order/api.yaml) | event-flow, saga-design |
| Inventory | [requirements.md](./bounded-contexts/inventory/requirements.md) | [domain-model.md](./bounded-contexts/inventory/domain-model.md) | — | event-flow |
| Payment | [requirements.md](./bounded-contexts/payment/requirements.md) | [domain-model.md](./bounded-contexts/payment/domain-model.md) | [api.yaml](./bounded-contexts/payment/api.yaml) | event-flow |
| BFF | [requirements.md](./bounded-contexts/bff/requirements.md) | [domain-model.md](./bounded-contexts/bff/domain-model.md) | [api.yaml](./bounded-contexts/bff/api.yaml) | — |
| Smart Interaction | [requirements.md](./bounded-contexts/smart-interaction/requirements.md) | — | — | — |
| Activity | [requirements.md](./bounded-contexts/activity/requirements.md) | [domain-model.md](./bounded-contexts/activity/domain-model.md) | [api.yaml](./bounded-contexts/activity/api.yaml) | — |
| Cart | [requirements.md](./bounded-contexts/cart/requirements.md) | [domain-model.md](./bounded-contexts/cart/domain-model.md) | [api.yaml](./bounded-contexts/cart/api.yaml) | — |
| Fulfillment | [requirements.md](./bounded-contexts/fulfillment/requirements.md) | [domain-model.md](./bounded-contexts/fulfillment/domain-model.md) | [api.yaml](./bounded-contexts/fulfillment/api.yaml) | event-flow |
| Frontend-admin | [ui-spec.md](./frontend/admin/ui-spec.md) | — | — | — |
| Frontend-web | [ui-spec.md](./frontend/web/ui-spec.md) | — | — | — |

## 与设计原则的对应

详见 [design-principles.md](./design-principles.md) 第五章「文档与输入」。
