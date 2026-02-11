# 前端功能需求

前端以**展示为主**，增删改由 MCP 完成。后端需求见 `docs/bounded-contexts/catalog/requirements.md`。

---

## 一、定位

| 端 | 职责 |
|----|------|
| **前端** | 查看、刷新；不做增删改 |
| **MCP** | 类目/商品/规格/SKU 的增删改 |

---

## 二、页面结构

**单页分层展示**：一个页面呈现类目 → 商品（含规格维度）→ SKU 的完整层级。

| 路由 | 页面 | 功能 |
|------|------|------|
| `/` | HomePage | 入口，导航到 Catalog 浏览 |
| `/catalog` | CatalogPage | **一页全览**：类目 → 商品（含 dimension）→ SKU 分层展示 |

---

## 三、Catalog 展示

### 3.1 层级结构

```
[类目 A]
  商品 1（规格维度：颜色、容量）
    SKU：黑色 128G ¥99
    SKU：白色 256G ¥129
  商品 2（规格维度：尺寸）
    SKU：S ¥50
    SKU：M ¥55
[类目 B]
  商品 3
    SKU：...
```

- **类目**：可展开/折叠，展示子类目；叶子类目下挂商品
- **商品**：展示名称、描述、规格维度（dimension 名称列表）
- **SKU**：展示规格组合、价格、展示名

### 3.2 交互

- 支持展开/折叠各级；
- 支持刷新按钮，重新拉取数据；
- 无增删改操作。

### 3.3 后端 API

使用现有接口组装数据：

- `GET /api/categories`（递归按 parentId 获取类目树）
- `GET /api/products?categoryId=:id`（叶子类目下的商品）
- `GET /api/products/:id`、`/dimensions`、`/skus`（商品详情、规格、SKU）

数据量小时，前端多次调用即可；若需优化，可后续增加聚合接口。

---

## 四、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/catalog/requirements.md` | 后端需求与 .feature |
| `docs/frontend/design-input.md` | 前端设计输入（FSD + Atomic、组件结构） |
