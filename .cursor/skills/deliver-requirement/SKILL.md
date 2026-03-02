---
name: deliver-requirement
description: 编排业务需求的交付：从分析方案的迭代计划出发，按依赖顺序调度各 Skill，跟踪完成进度，并通过 E2E 交付门禁确保端到端链路可用。触发词：交付需求、deliver requirement、需求交付、交付验收、E2E 验收。
---

# 交付业务需求

从 `analyze-requirement` 产出的**迭代计划**出发，编排各 Skill 的执行顺序，跟踪完成进度，并在交付前通过端到端验收门禁确保业务链路可用。

**核心原则**：编排可以按复杂度简化，但 E2E 交付门禁不可跳过。

> Skill 体系总览与流程链见 `.cursor/skills/README.md`。

---

## 何时用 / 不用

| 用 | 不用 |
|----|------|
| `analyze-requirement` 完成后，需要执行迭代计划中的工作项 | 直接知道改哪个 BC 的哪个 Feature → `evolve-feature` |
| 需要跨多个 Skill 协调并最终验收一个业务需求 | Bug 修复或场景微调 → `fix-bug-or-adjust-feature` |
| 需要在交付前确保端到端链路可用 | 单纯的 BC 间对接（无业务需求上下文）→ `integration` |

**判断标准**：如果有 `overview.md` 迭代计划，并且该迭代包含 2 个以上工作项（多 BC / 含集成 / 含前端），就应该用本 Skill。

---

## Step 0：读取交付范围与模式选择

### 0a. 读取迭代计划

- 读 `docs/business-requirements/<name>/overview.md` 的迭代计划
- 确认当前要交付的是哪个迭代（若有多个迭代，按顺序逐个交付）
- 列出本迭代所有工作项：
  - 哪些 BC 需要 `evolve-feature`
  - 哪些 BC 间需要 `integration`
  - 哪些前端需要 `frontend-development`
  - E2E 验收条件（`BIZ-xxx` 标注）

### 0b. 确认依赖顺序

- 通常：先下游（被调用方）后上游（调用方）
- `integration` 在相关 BC 的 `evolve-feature` 都完成后执行
- `frontend-development` 在后端 API 就绪后执行
- E2E 门禁在所有工作项完成后执行

### 0c. 选择模式

| 模式 | 条件 | 行为差异 |
|------|------|---------|
| **轻量** | 单 BC + 单迭代 + 无跨 BC 集成 | 跳过编排跟踪表，直接列工作项清单 → 执行 → E2E 门禁 |
| **标准** | 多 BC 或多迭代或含跨 BC 集成 | 完整编排跟踪 + 依赖排序 + 逐项执行 + E2E 门禁 |

### 0d. 创建/更新交付跟踪（标准模式）

在 `overview.md` 末尾新增或更新「交付跟踪」章节：

```markdown
## 交付跟踪

### 迭代 N：<迭代名称>

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | <BC-A> 特性变更 | evolve-feature | — | ⬜ 待执行 |
| 2 | <BC-B> 特性变更 | evolve-feature | — | ⬜ 待执行 |
| 3 | <BC-A> → <BC-B> 集成 | integration | #1, #2 | ⬜ 待执行 |
| 4 | Web 前端对接 | frontend-development | #1, #2, #3 | ⬜ 待执行 |
| 5 | E2E 交付门禁 | deliver-requirement | #1–#4 | ⬜ 待执行 |
```

---

## Step 1：按序执行各工作项

按依赖顺序，逐一调度对应 Skill 执行各工作项：

- 每个工作项在**独立对话**中执行（避免上下文溢出）
- 完成一个工作项后，更新交付跟踪表状态（⬜ → 🔄 → ✅）
- 若执行中发现需要调整迭代计划（如新增集成、发现遗漏 BC），先更新计划再继续

**各 Skill 的调度要点**：

| Skill | 调度要点 |
|-------|---------|
| `evolve-feature` | 按迭代计划指定的 BC 和特性变更执行；若涉及多个 BC，先下游后上游 |
| `integration` | 在上下游 BC 的 evolve-feature 都完成后执行 |
| `frontend-development` | 后端 API 就绪后执行；Smoke P0 仍在 frontend-development 内部执行 |

> **注意**：本步骤是编排调度，具体的技术实现由各 Skill 自行负责。`deliver-requirement` 不介入 evolve-feature 内部的 ATDD 流程。

---

## Step 2：E2E 交付门禁（必做，不可跳过）

所有工作项完成后，执行端到端验收。这是交付前的**强制门禁**。

### 2a. Smoke P0 全量回归

```bash
cd frontend/web && npm run test:smoke:e2e:p0
```

确保既有核心链路未被破坏。若失败，排查是本次变更引入的回归还是环境问题。

### 2b. Business E2E 验收

根据 `overview.md` 迭代计划中标注的 E2E 验收条件（`BIZ-xxx`），编写并运行 Business E2E：

- 在 `tests/business-e2e/specs/<需求名>/` 下新增用例
- 用例命名格式：`BIZ-<需求缩写>-xxx 描述`
- 断言深度：验证业务链路能走通（操作链路端到端），不验证 UI 细节
- 运行验证：`npm run test:business:e2e -- --grep "BIZ-<需求>"`
- **必须全绿**后方可继续

### 2c. 评估 Smoke 入选

从已通过的 Business E2E 中评估是否纳入 Smoke 长期守护：

| 级别 | 条件 |
|------|------|
| P0 | 核心交易链路（系统不可用级别） |
| P1 | 重要但非核心链路 |
| 不入选 | 非系统级可用性链路，仅保留 Business E2E |

### 2d. 开发者最终确认

列出本迭代交付的完整内容：
- 各 BC 的特性变更摘要
- 集成变更
- 前端变更（受影响页面 + 预期行为）
- E2E 验收结果

**停顿，请开发者确认。** 开发者确认通过后方可标记迭代完成。

---

## Step 3：迭代完成与后续

### 3a. 更新交付跟踪

将本迭代所有工作项标记为 ✅ 完成，E2E 门禁标记为 ✅ 通过。

### 3b. 判断后续

| 情况 | 动作 |
|------|------|
| 还有后续迭代 | 标注下一个迭代，等待后续调用 |
| 这是最后一个迭代 | 执行 Step 4（需求级收尾） |
| 无迭代计划（轻量模式） | 直接执行 Step 4 |

### 3c. 更新 `docs/project-status.md`

更新 BC 状态、路线图、变更日志。

---

## Step 4：需求级收尾（最后一个迭代完成后）

| 检查项 | 说明 |
|--------|------|
| 全量 Smoke 回归 | `npm run test:smoke:e2e:p0` 确认所有 P0 链路全绿 |
| 所有迭代 E2E 回归 | 跑一遍所有迭代的 Business E2E，确认无退化 |
| 文档完整性 | `overview.md` 交付跟踪全部 ✅；`project-status.md` 已更新 |
| 遗留问题 | 若有已知限制或后续优化点，记录在 `overview.md` |

---

## 约定

| 约定 | 说明 |
|------|------|
| **文档是状态载体** | 交付进度持久化在 `overview.md` 的交付跟踪章节，不依赖对话记忆 |
| **门禁不可跳过** | 无论轻量还是标准模式，E2E 交付门禁都是必做步骤 |
| **编排不介入实现** | `deliver-requirement` 只做调度和验收，不介入各 Skill 内部的技术决策 |
| **迭代独立可验收** | 每个迭代完成后都有独立的 E2E 验收，不积压到最后 |

---

## 检查清单

- [ ] 迭代计划已读取，工作项和依赖顺序已确认
- [ ] 交付跟踪已创建（标准模式）或工作项清单已列出（轻量模式）
- [ ] 所有工作项按序完成（各 Skill 执行完毕）
- [ ] **Smoke P0 全量回归通过**
- [ ] **Business E2E 全绿**（覆盖 overview.md 标注的所有 BIZ-xxx 场景）
- [ ] **Smoke 入选已评估**（若有新交易链路）
- [ ] **开发者已最终确认**
- [ ] 交付跟踪已更新为完成状态
- [ ] `project-status.md` 已更新

---

## 参考

- **Skill 体系总览**：`.cursor/skills/README.md`
- **业务需求方案**：`docs/business-requirements/<name>/overview.md`
- **前端测试说明**：`docs/frontend/web/testing.md`
