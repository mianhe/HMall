# HMall 文档

所有文档统一放在 `docs/` 下，按职责分目录。限界上下文归入 `bounded-contexts/`，跨 BC 业务需求归入 `business-requirements/`。

## 文档结构

```
docs/
├── README.md                     # 本索引
├── context-map.md                # 系统结构（BC 总览、集成关系、集成技术选型）
├── business-flows.md             # 业务流程（价值流、事件流、事件总表、路径枚举、测试覆盖）
├── business-process-architecture.md  # 业务流程架构（流程体系、事件分类、智能化分层、演进路线）
├── design-principles.md          # 系统设计原则（架构、分层、数据隔离、约定）
├── project-status.md             # 项目状态与路线图
├── business-requirements/                        # 跨 BC 业务需求的整体方案
│   └── <business-requirement-name>/
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
│       ├── ui-spec.md
│       └── testing.md            # 前端测试说明（Smoke E2E 分层、用例、维护策略）
```

## 文档索引

| 文档 | 回答的问题 |
|------|-----------|
| [context-map.md](./context-map.md) | 系统有哪些部件、怎么连接、用什么技术 |
| [business-flows.md](./business-flows.md) | 有哪些业务流程、怎么流转、怎么验证 |
| [business-process-architecture.md](./business-process-architecture.md) | 业务流程体系是什么、事件如何组织、智能化如何分层、系统向何处演进 |
| [design-principles.md](./design-principles.md) | 怎么设计和实现 |
| [project-status.md](./project-status.md) | 做到哪了 |
| `business-requirements/<name>/overview.md` | 某个业务需求的方案、设计决策、迭代计划 |

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
| Frontend-web | [ui-spec.md](./frontend/web/ui-spec.md) | — | — | [testing.md](./frontend/web/testing.md) |

## 与设计原则的对应

详见 [design-principles.md](./design-principles.md) 第五章「文档与输入」。
