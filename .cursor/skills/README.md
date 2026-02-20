# HMall Skill 体系

本项目的 Agent Skill 覆盖从需求分析到前端实现的完整开发流程。每个 Skill 是一个可独立执行的步骤，按业务场景组合使用。

---

## 流程链

```
analyze-requirement → [add-bounded-context] → evolve-feature → [integration]
                                                    ↕
                                          fix-bug-or-adjust-requirement
                                                    ↓
                                          frontend-development
```

### 典型场景与 Skill 组合

| 场景 | 使用的 Skill（按顺序） |
|------|----------------------|
| **新建 BC** | analyze-requirement → add-bounded-context → evolve-feature × N → integration |
| **已有 BC 重大演进** | analyze-requirement → evolve-feature × N → [integration] |
| **单个 feature 演进** | evolve-feature |
| **Bug 修复或小幅需求调整** | fix-bug-or-adjust-requirement |
| **跨 BC 集成对接** | integration |
| **前端开发** | frontend-development |

---

## 各 Skill 说明

### analyze-requirement — 需求分析

**定位**：开发前的分析与设计，产出需求文档、领域模型、API/事件契约、关联 BC 变更清单。

**适用**：新建 BC、已有 BC 重大演进、跨 BC 流程重构。

**不适用**：单个 feature 的小幅演进（直接用 evolve-feature）。

**关键步骤**：确定范围 → 事件流分析 → 领域建模 → 设计决策澄清 → 需求编写 → 关联 BC 影响分析 → 一致性检查 → 系统文档更新。

---

### add-bounded-context — 新建 BC 骨架

**定位**：创建一个新 BC 的技术骨架（文档目录、后端四层包结构、验收测试脚手架），不含业务代码。

**前置**：analyze-requirement 已完成（有 requirements.md、domain-model.md 等）。

**产出**：DDD 四层包结构、Cucumber 测试脚手架、Maven 模块、`mvn test` 可运行。

---

### evolve-feature — 演进功能

**定位**：在已有 BC 内实现一个 feature，遵循 ATDD 三阶段。

**前置**：BC 骨架已存在；需求已明确（requirements.md 中对应的 feature 和 scenario）。

**关键步骤**：需求与模型确认 → 契约与测试先红 → 实现变绿 → 清理与整洁代码。

---

### integration — 跨 BC 集成

**定位**：在调用方实现出站端口的真实适配器，对接被调用方已有的 API。

**前置**：被调用方的 API 已实现并可用；调用方的 Port 接口已定义。

**产出**：REST 适配器、集成测试（WireMock）、配置。

---

### fix-bug-or-adjust-requirement — 修复 Bug / 调整需求

**定位**：修复已有功能的 Bug，或对已有需求做小幅调整。

**适用**：行为与需求不符、需求微调（不涉及新的领域建模或跨 BC 影响）。

**不适用**：重大需求变更（用 analyze-requirement）。

---

### frontend-development — 前端开发

**定位**：实现或扩展 HMall 前端（frontend-admin、frontend-web）。

**原则**：需求优先、契约对齐、不重复业务规则；新页面或大改时维护 Design Input。

---

## 判断用哪个 Skill

```
需要做什么？
  ├── 新建或重大演进 → analyze-requirement（然后按场景继续）
  ├── 实现一个 feature → evolve-feature
  ├── 修 bug 或小幅调整 → fix-bug-or-adjust-requirement
  ├── 对接两个 BC → integration
  └── 前端页面 → frontend-development
```
