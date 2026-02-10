---
name: atdd-new-feature
description: 按 ATDD 开发后端新功能：三阶段——① 定义需求与模型 ② 定义契约并实现测试脚本（先红）③ 实现与验收（变绿）。通用流程，适用于任意限界上下文。触发词如「用 ATDD 做新功能」「按 ATDD 开发」。
---

# ATDD 新功能开发

**流程**：先做 Git 准备，再按三阶段顺序执行，最后合并回主干；带 **【需开发者确认】** 的步骤须先产出、经确认后再继续。

| 阶段 | 目标 |
|------|------|
| **一、定义需求与模型** | 说清「做什么」和「领域长什么样」 |
| **二、定义契约并实现测试脚本** | 定接口、写验收脚本，**先红** |
| **三、实现与验收** | 后端（及可选前端）实现直到**全绿** |

文档与代码位置按**当前限界上下文**约定（如 Catalog 用 `docs/catalog/`、`features/catalog/`；其他上下文用对应目录）。

---

## Git 护栏（开始前 / 结束后）

为避免改动落在临时 worktree 或 detached HEAD，建议固定执行：

1. **开始前创建分支**：从 `main` 拉出 `feature/<name>`（必要时显式创建 worktree 并在该目录开发）。
2. **开发中仅在 feature 分支提交**：不直接在 `main` 开发。
3. **结束后合并主干**：验收全绿后，将 `feature/<name>` 合并回 `main`（推荐 `--no-ff` 保留 feature 轨迹）。
4. **合并前提**：feature 分支与 main 工作区均为干净状态，且测试通过。

> 建议命令（示例）：  
> `git switch main && git pull`  
> `git switch -c feature/<name>`  
> （可选）`git worktree add .hmall/worktrees/feature-<name> feature/<name>`  
> 开发完成后：`git switch main && git merge --no-ff feature/<name>`

## 步骤 To-Do

### 阶段一：定义需求与模型

步骤 1、2 可交替：先补需求与 .feature，或先改模型再补场景。

| 步骤 | 动作 | 确认 |
|------|------|------|
| **1** | 在**需求文档**中补新场景及与 .feature 对应；在 **.feature** 中写 Gherkin（`backend/src/test/resources/features/<上下文>/`），需求与 feature 一一对应。 | 【需开发者确认】需求与 Feature 一致；若涉及领域模型变更，是否进入步骤 2。 |
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
| **5** | **后端实现直到变绿**：领域 → 基础设施 → 应用 → API 分层实现；每层可跑一次验收。若步骤 2 已更新模型，按文档从内到外；若实现中才发现需改模型，先更新领域模型文档并【需开发者确认】再改代码。 |
| **6** | **重构与清理**：变绿后对本次改动做重构与清理，保证代码整洁。包含：生产代码（去冗余、统一风格、抽共用逻辑等）；测试代码与 Step Definitions（去重复步骤、语义一致的步骤统一为共用断言、.feature 可读性如参数加引号以匹配参数化步骤）。验收保持全绿。 |
| **7**（可选） | 前端对接：契约稳定后按后端返回展示，不重复业务规则。 |

---

## 执行要点

**阶段一**

- 需求文档、.feature：路径与命名依项目约定（如 `docs/<context>/requirements.md`、`features/<context>/*.feature`）。Gherkin 风格与既有一致（假如/当/那么），场景可验证（状态码、响应体、错误信息）。
- 领域模型文档：路径依项目（如 `docs/<context>/domain-model.md`）。实体、关系、不变式按聚合写；与步骤 1 可交替推敲。

**阶段二**

- 契约：在对应 OpenAPI YAML 中补 path、method、parameters、requestBody、responses（2xx/4xx/5xx 与 schema）。与 Step Definitions 的请求和断言一致。
- Step Definitions：用 TestRestTemplate 等按契约调 API；运行验收（如 `mvn test -Dtest=RunCucumberTest`），预期新场景红。

**阶段三**

- 后端四层：领域（实体/仓储接口）→ 基础设施（JPA/表、仓储实现）→ 应用（用例、事务、校验、异常与契约一致）→ API（Controller、契约状态码与 body）。实现中若需变更领域模型：先更文档、确认后再改代码。
- **重构与清理**（步骤 6）：变绿后做一次整理。生产代码：去冗余、统一风格（如 .toList()）、合并重复异常处理等。测试与 Step Definitions：语义相同的步骤共用私有断言方法；可删除的字面步骤改为 .feature 中参数加引号后删掉；400 等错误断言可收口到统一方法。整理后再跑验收，保持全绿。

---

## 检查清单

- [ ] **Git 流程**：已从 `main` 创建 `feature/*` 开发；最终已将 feature 合并回 `main`
- [ ] **阶段一**：需求与 .feature 一致；若涉及模型，领域模型已更新且经确认
- [ ] **阶段二**：契约已更新且经确认；Step Definitions 已实现，验收为新场景红
- [ ] **阶段三**：后端全绿；重构与清理已完成（生产 + 测试/Step Definitions），验收仍全绿；前端（若做）仅依后端返回

---

## 实现中才发现需变更领域模型

1. 更新领域模型文档（实体、关系、不变式）。
2. **【需开发者确认】** 文档与变更说明。
3. 再从内到外改代码；若接口或数据形状变化，同步改 feature / Step Definitions / 前端。
4. 跑验收直到绿。

---

## 参考

- 文档与代码位置按**当前限界上下文**定（如本项目 Catalog：`backend/docs/catalog/`、`backend/src/test/resources/features/catalog/`、`catalog-api.yaml`；其他上下文类推）。
- 项目内示例：删除商品见 `product.feature`、`catalog-api.yaml`、`ProductStepDefinitions`、应用层与 Controller。
