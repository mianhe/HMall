---
name: add-new-feature
description: 在某一限界上下文（BC）内增加新功能：新增或修改 .feature 文件、补充 Scenario，按 ATDD 三阶段（定义需求与模型 → 契约与测试脚本先红 → 实现变绿）完成。触发词：增加新功能、add feature、新增 feature、修改 feature。
---

# 在 BC 内增加新功能

在**已有**限界上下文中增加新功能：新增 .feature 文件，或在既有 .feature 中新增/修改 Scenario。按三阶段顺序执行；带 **【需开发者确认】** 的步骤须先产出、经确认后再继续。

| 阶段 | 目标 |
|------|------|
| **一、定义需求与模型** | 说清「做什么」和「领域长什么样」 |
| **二、定义契约并实现测试脚本** | 定接口、写验收脚本，**先红** |
| **三、实现与验收** | 后端（及可选前端）实现直到**全绿** |

文档与代码位置按**当前限界上下文**约定（如 Catalog 用 `docs/bounded-contexts/catalog/`、`features/catalog/`；其他上下文用对应目录）。

**实现须遵循** `docs/design-principles.md`：分层、仓储/应用服务/DTO 命名、异常映射（400/404）、domain 与 JPA 分离、验收测试与 feature 一一对应等。

---

## 步骤 To-Do

### 阶段一：定义需求与模型

步骤 1、2 可交替。

| 步骤 | 动作 | 确认 |
|------|------|------|
| **1** | 在**需求文档**中补新场景；在 **.feature** 中写 Gherkin（`backend/src/test/resources/features/<上下文>/`），需求与 feature 一一对应。可**新增** .feature 或在既有 .feature 中**新增/修改 Scenario**。 | 【需开发者确认】需求与 Feature 一致；若涉及领域模型变更，是否进入步骤 2。 |
| **2**（若涉及模型） | 在**领域模型文档**中补/改：实体、值对象、属性、关系、不变式（按聚合）。与需求、契约一致。 | 【需开发者确认】模型、表结构、不变式无误后再进入阶段二。 |

### 阶段二：定义契约并实现测试脚本

阶段结束时应**验收先红**（接口未实现或未满足契约）。

| 步骤 | 动作 | 确认 |
|------|------|------|
| **3** | 在 **OpenAPI YAML** 中补/改 path、method、request、response，与领域模型及步骤 4 的请求/断言一致。 | 【需开发者确认】契约无误后再写步骤 4。 |
| **4** | 写 **Step Definitions**：Given/When/Then 按契约调 REST API，保存最后一次响应用于 Then；跑验收，**预期新场景红**（如 404/405）。 | — |

### 阶段三：实现与验收

| 步骤 | 动作 |
|------|------|
| **5** | **后端实现直到变绿**：领域 → 基础设施 → 应用 → API 分层实现，遵循 `docs/design-principles.md` 中的架构与实现约定。若步骤 2 已更新模型，按文档从内到外；若实现中才发现需改模型，先更新领域模型文档并【需开发者确认】再改代码。 |
| **6** | **重构与清理**：变绿后对本次改动做重构与清理（生产代码去冗余、测试/Step Definitions 去重复、.feature 可读性）。验收保持全绿。 |
| **7**（可选） | 前端对接：契约稳定后按后端返回展示，不重复业务规则。 |

---

## 执行要点

- **阶段一**：.feature 路径 `backend/src/test/resources/features/<上下文>/`；在 Feature 上添加该 BC 的 tag（如 Catalog 用 `@catalog`、User 用 `@user`），以便 `./scripts/hmall.sh test --bc <context>` 可筛选执行。
- **阶段二**：OpenAPI 与 Step Definitions 的请求/断言一致；验收命令如 `mvn test -Dtest=RunCucumberTest`，预期新场景红。
- **阶段三**：领域 → 基础设施 → 应用 → API；异常与契约一致（如 404 用 IllegalArgumentException + ExceptionHandler）。变绿后做步骤 6 的清理。

---

## 检查清单

- [ ] **阶段一**：需求与 .feature 一致；若涉及模型，领域模型已更新且经确认
- [ ] **阶段二**：契约已更新且经确认；Step Definitions 已实现，验收为新场景红
- [ ] **阶段三**：后端全绿；重构与清理已完成，验收仍全绿；前端（若做）仅依后端返回

---

## 实现中才发现需变更领域模型

1. 更新领域模型文档（实体、关系、不变式）。
2. **【需开发者确认】** 文档与变更说明。
3. 再从内到外改代码；若接口或数据形状变化，同步改 feature / Step Definitions / 前端。
4. 跑验收直到绿。

---

## 参考

- **设计原则**：`docs/design-principles.md`（架构、分层、实现约定、验收约定；原则冲突以该文档为准）。
- 文档与代码位置按**当前限界上下文**定。路径形如：`docs/bounded-contexts/<上下文名>/`（需求、契约、领域模型）、`backend/src/test/resources/features/<上下文名>/`（.feature）。例如 Catalog：`docs/bounded-contexts/catalog/domain-model.md`、`docs/bounded-contexts/catalog/requirements.md`、`docs/bounded-contexts/catalog/api.yaml`。
- 项目内示例（Catalog 上下文）：类目/商品增删改查见 `category.feature`、`product.feature`、`catalog-api.yaml`、对应 Step Definitions 与应用层、Controller。
