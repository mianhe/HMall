# HMall 文档

所有文档统一放在根 `docs/` 下，按「文档所有者」分目录；限界上下文归入 `bounded-contexts/`，过程性文档归入各上下文内部。

## 文档结构

```
docs/
├── design-principles.md        # 系统设计原则（架构、分层、数据隔离、约定）
├── README.md                  # 本索引
├── bounded-contexts/          # 限界上下文
│   ├── context-map.md         # 上下文地图
│   ├── catalog/               # Catalog 上下文
│   │   ├── domain-model.md    # 领域模型
│   │   ├── requirements.md   # 需求列表
│   │   ├── api.yaml           # API 契约
│   │   ├── domain-model-diagram.png
│   │   └── process/           # 过程性文档
│   │       ├── implementation-path.md
│   │       ├── catalog-api-doc.md
│   │       └── ...
│   └── user/                  # User 上下文（规划中）
│       └── ...
└── frontend/                  # 前端
    ├── design-input.md
    └── requirements.md
```

## 对应关系

| 文档所有者 | 可复现输入 | 过程性文档 |
|------------|------------|------------|
| Catalog | `docs/bounded-contexts/catalog/`（根下文件） | `docs/bounded-contexts/catalog/process/` |
| User | `docs/bounded-contexts/user/` | `docs/bounded-contexts/user/process/` |
| Fulfillment | `docs/bounded-contexts/fulfillment/`（规划中） | — |
| Frontend | `docs/frontend/` | — |

## 与设计原则的对应

详见 [design-principles.md](./design-principles.md) 第五章「文档与输入」。
