# 阶段一产出确认：Catalog 验收场景（.feature）总览

> 进入阶段二（契约 + Step Definitions）前，对本阶段产出做一次确认。读者可快速了解「我们写了哪些验收场景」；开发者可据此对照需求、实现 Step Definitions 与接口。

---

## 1. .feature 文件列表

| 文件 | 路径 | 功能 | 场景数 |
|------|------|------|--------|
| category.feature | `backend/src/test/resources/features/catalog/category.feature` | 管理类目 | 5 |
| product.feature | `backend/src/test/resources/features/catalog/product.feature` | 管理商品（SPU） | 8 |
| spec-dimension.feature | `backend/src/test/resources/features/catalog/spec-dimension.feature` | 管理 SPU 规格维度与选项 | 9 |
| sku.feature | `backend/src/test/resources/features/catalog/sku.feature` | 管理 SKU | 8 |

**合计**：4 个 feature 文件，30 个场景（含主流程 + 边界/极端情况）。  
**语言**：`# language: zh-CN`，步骤为中文（假如/当/那么）。

---

## 2. 各 .feature 内容简介

### category.feature — 管理类目

**目的**：创建、查询类目树（根类目、子类目），为后续「在叶子类目下挂商品」打基础。

| 场景 | 简要说明 |
|------|----------|
| 创建根级类别 | 创建如「手机」，无父类别，返回名称 |
| 在已有类别下创建子类别 | 在「手机」下创建「智能手机」，返回名称且父为「手机」 |
| 查询根目录下所有类别 | 给定根下「手机」「平板」，返回 2 个且包含二者 |
| 查询某类别下所有子类别 | 给定「手机」下「智能手机」「功能机」，返回 2 个且包含二者 |
| 父类目不存在时创建子类目应失败 | 父类目 ID 999999 下创建子类别 → 失败，404 |

**前置**：无（根类目直接创建）。**产出**：类目 ID、父子关系，供 product 使用。

---

### product.feature — 管理商品（SPU）

**目的**：在**叶子类目**下创建/查询/删除商品（SPU）；非叶子类目创建商品应失败。

| 场景 | 简要说明 |
|------|----------|
| 在叶子类别下创建商品 | 给定叶子「手机」，创建「iPhone 15」+ 描述，返回名称、描述、所属类别 |
| 在非叶子类别下创建商品应失败 | 「手机」下有子类时创建商品 → 失败 + 错误提示（仅叶子可挂商品） |
| 按类别查看该类别下所有商品 | 给定类别下「iPhone 15」「小米14」，返回 2 个且包含二者 |
| 按 ID 查看商品详情 | 给定已有商品，请求详情，返回名称、描述、所属类别 |
| 删除已存在的商品 | 删除后再次请求详情 → 404 |
| 删除不存在的商品应返回 404 | 删除不存在的 ID → 404 |
| 类目 ID 不存在时创建商品应失败 | 类目 ID 999999 下创建商品 → 失败，404 |
| 按 ID 查商品详情时商品不存在应返回 404 | 请求商品 ID 999999 详情 → 404 |

**前置**：叶子类目（来自 category.feature）。**产出**：SPU ID，供 spec-dimension、sku 使用。

---

### spec-dimension.feature — 管理 SPU 规格维度与选项

**目的**：为 SPU 配置「规格维度」（如容量、颜色）及每个维度下的「可选项」（如 128G/256G、黑/白），供创建 SKU 时按 Option 组合。

| 场景 | 简要说明 |
|------|----------|
| 为 SPU 添加规格维度 | 给定 SPU「iPhone 15」，添加维度「容量」、必填、排序 1，返回维度名称 |
| 为某维度添加可选项 | 给定维度「容量」，添加选项「128G」排序 1，返回选项值 |
| 为影响外观的维度添加带图片的选项 | 给定维度「颜色」影响外观，添加选项「黑色」+ 图片 URL，返回选项值及图片 |
| 查询某 SPU 下所有维度及选项 | 给定「容量」含 128G/256G、「颜色」含黑/白，返回 2 维度及各自选项 |
| 查询维度及选项时返回选项的图片 | 给定选项「黑色」带图片，请求维度及选项，断言该选项返回图片 URL |
| 在无效或不存在的 SPU 下添加维度应失败 | 为 SPU ID 999999 添加维度 → 失败，404 |
| 同 SPU 内维度名称重复应失败 | 该 SPU 已有维度「容量」时再添加「容量」→ 失败，错误提示 |
| 同维度内选项值重复应失败 | 维度「容量」已有选项「128G」时再添加「128G」→ 失败，错误提示 |
| 为不存在的维度添加选项应失败 | 为维度 ID 999999 添加选项 → 失败，404 |

**前置**：已有 SPU（来自 product.feature）。**产出**：维度 ID、选项 ID，供 sku.feature 创建 SKU 时「选 Option + 价格」。

---

### sku.feature — 管理 SKU

**目的**：在 SPU 下按「每个必填维度选一 Option + 价格」创建 SKU，并支持按 SPU 查列表、按 SPU+SKU ID 查详情；非法 SPU 或未选齐必填维度应失败。

| 场景 | 简要说明 |
|------|----------|
| 在已有 SPU 下创建 SKU（选齐必填维度 Option 与价格） | 给定 SPU 有维度容量/颜色及选项，选容量 128G、颜色黑、价格 599900 → 成功，返回价格与所选规格 |
| 在不存在或无效的 SPU 下创建 SKU 应失败 | SPU ID 999999 创建 SKU → 失败，404 |
| 未选齐必填维度或 Option 不属于该 SPU 应失败 | 仅选容量不选颜色 → 失败，错误提示（未选齐必填维度：缺少颜色） |
| 按 SPU 查询该 SPU 下所有 SKU | 给定 2 个 SKU（128G 黑 599900、256G 白 699900），返回 2 个且包含两价格 |
| 按 SPU ID 与 SKU ID 查询 SKU 详情 | 给定已有 SKU，请求详情，返回价格及规格（容量 128G、颜色黑） |
| 创建 SKU 时传入不属于该 SPU 的 Option 应失败 | 使用非本 SPU 的 Option 创建 → 失败，错误提示 |
| 创建 SKU 时价格为负数应失败 | 价格 -100 分创建 → 失败，错误提示 |
| 按 SPU 与 SKU 查详情时 SKU 不存在或不属于该 SPU 应返回 404 | 该 SPU 下请求 SKU ID 999999 详情 → 404 |

**前置**：SPU + 该 SPU 下已配置的维度与选项（来自 spec-dimension.feature）。**产出**：SKU ID。

---

## 3. 主路径与依赖关系（给开发者）

验收时的**推荐主路径**（与需求文档主路径一致）：

```
类目(category) → 商品/SPU(product) → 规格维度与选项(spec-dimension) → SKU(sku)
```

- **category**：建根类目、子类目，得到叶子类目 ID。
- **product**：在叶子类目下建 SPU，得到 SPU ID。
- **spec-dimension**：为 SPU 添加维度、为维度添加选项，得到 dimensionId、optionIds。
- **sku**：在 SPU 下选 Option + 价格创建 SKU，得到 SKU ID；查列表、查详情。

Step Definitions 的 Given 需按此依赖准备数据（或调已有 API 造数）。

---

## 4. 如何运行（启动验收）

- **入口类**：`com.hmall.catalog.acceptance.RunCucumberTest`
- **命令**：在 `backend` 目录下执行  
  `mvn test -Dtest=RunCucumberTest`
- **作用**：扫描 `src/test/resources/features` 下所有 `.feature`，使用 glue `com.hmall.catalog.acceptance` 中的 Step Definitions 执行。
- **报告**：控制台 pretty 输出；HTML 报告可生成在 `target/reports/cucumber.html`（若已配置）。

**当前状态**：category、product 已有 Step Definitions 且接口已实现（可绿）；spec-dimension、sku 尚无 Step Definitions 与对应 API，运行时会**未定义步骤**或**失败**，待阶段二补契约与步骤后预期**先红**（如 404），再在阶段三实现到绿。

---

## 5. 与需求文档的对应

| 需求文档功能 | .feature 文件 | 场景对应 |
|--------------|----------------|----------|
| 1. 管理类目 | category.feature | 1.1～1.5 |
| 2. 管理商品 | product.feature | 2.1～2.7 |
| 3. 管理 SPU 规格维度与选项 | spec-dimension.feature | 3.1～3.9 |
| 4. 管理 SKU | sku.feature | 4.1～4.8 |

阶段一产出确认如上；确认无误后即可进入阶段二（契约 + 测试脚本）。
