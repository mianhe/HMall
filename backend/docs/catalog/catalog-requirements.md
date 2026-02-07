# 商品限定上下文 - 需求列表

> 需求按「功能（Feature）」组织，共两个功能，每个功能对应后续的一个 .feature 文件；其下为「场景（Scenario）」，对应 Gherkin 中的场景。本文档仅为需求列表，.feature 文件将基于此生成。  
> 实现通道：本上下文通过 REST API 提供，契约见同目录下 `catalog-api.yaml`。  
> **feature 文件位置**：`backend/src/test/resources/features/catalog/`（category.feature、product.feature）

---

## 1. 功能：管理类别

**对应 feature 文件**：`category.feature`（或 `category-management.feature`）

### 场景（Scenario）

- 1.1 创建根级类别（如平板、手机）
- 1.2 在已有类别下创建子类别
- 1.3 查询根目录下所有类别
- 1.4 查询某类别下所有子类别

---

## 2. 功能：管理商品

**对应 feature 文件**：`product.feature`（或 `product-management.feature`）

### 场景（Scenario）

- 2.1 在叶子类别下创建商品（仅叶子节点允许挂商品）
- 2.2 在非叶子类别下创建商品应失败或提示
- 2.3 按类别查看该类别下所有商品
- 2.4 按 ID 查看商品详情

---

## 与 .feature 的对应关系

| 需求文档中的功能 | 后续 .feature 文件 | 场景 |
|------------------|--------------------|------|
| 1. 管理类别 | category.feature | 1.1, 1.2, 1.3, 1.4 |
| 2. 管理商品 | product.feature | 2.1, 2.2, 2.3, 2.4 |

写 .feature 时：每个「功能」一个 feature 文件，每个「场景」对应该文件里的一个 `场景:`（Scenario）。
