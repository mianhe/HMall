---
name: event-driven-analysis
description: 以事件为核心的跨 BC 业务分析：先选分析模式（重流程/轻流程/纯领域），再执行事件流分析、领域建模、Saga 设计。Event Storming 仅在重流程模式使用。触发词：事件驱动分析、event-driven analysis、Event Storming、Saga 设计。
---

# 事件驱动的业务分析

以事件为核心的跨限界上下文（BC）业务分析方法，适用于订单、交易等编排型场景。整体流程：**事件优先发现 → 领域建模（关联事件与对象）→ Saga 设计**。三者迭代，而非一次性线性完成。

完整方法与理论说明见：`docs/architecture/event-driven-business-analysis.md`。

---

## Step 0：分析范围与模式选择（必须先做）

在开始分析前，先确定**分析范围**和**分析模式**，以决定后续是否使用 Event Storming、以及投入程度。

| 模式 | 适用场景 | Event Storming | 产出侧重 |
|------|----------|----------------|----------|
| **重流程** | 跨 BC 编排型流程（订单、交易、履约等），流程复杂、异步与补偿多 | 使用 | 事件流 + 领域建模 + Saga 设计 |
| **轻流程** | 单 BC 或简单跨 BC，流程较短、补偿简单 | 可选简化版 | 事件清单 + 领域建模，Saga 可简化为步骤表 |
| **纯领域** | 无显著流程，以聚合/实体/值对象建模为主 | 不使用 | 领域模型，事件仅作补充 |

**执行**：询问或根据用户描述判断当前问题属于哪种模式，在后续步骤中按对应模式执行。**Event Storming 仅在重流程模式下使用**；轻流程、纯领域模式可跳过或简化 Step 1 的 Event Storming 工作坊形式。

---

## Step 1：事件流分析（问题域）

**目标**：发现业务中发生的「事情」，建立时间线与因果关系。

**方法**（重流程模式）：
- 使用 **Event Storming** 或类似工作坊，在时间线上贴出领域事件（橙色）
- 补充命令（蓝色）、策略「当 X 发生则做 Y」（淡紫）、聚合（黄色）
- 区分主流程事件与旁支事件

**轻流程/纯领域模式**：可简化为列出事件清单，不必做完整工作坊。

**产出**：
- 主流程事件链：`OrderCreated → InventoryReserved → PaymentCompleted → FulfillmentOrderCreated → ...`
- 逆向/补偿事件链：`OrderCancelled ← InventoryReleased ← PaymentRefunded ← ...`
- 事件与命令、策略的对应关系

**注意**：此阶段不必强求「事件名与领域对象一一对应」，先以业务语言表达清楚即可。

---

## Step 2：领域建模（问题域 → 方案域）

**目标**：识别聚合、实体、值对象，明确事件与领域对象的归属关系。

**方法**：
- 为每个事件追问：「这件事发生在哪个业务对象上？」
- 识别聚合根、子对象（如 Order / OrderLineItem / PaymentRef / FulfillmentRef）
- 将事件归类到对应聚合，统一命名规范（如 `{聚合}{动作过去式}`：OrderCreated、PaymentCompleted）

**产出**：
- 领域模型图（聚合、实体、关系）
- 事件清单（含所属聚合）
- 各聚合的状态/生命周期（可用「事件驱动」表达：状态 = 已应用事件的累积结果）

**迭代**：建模过程中会反哺事件流——发现缺失事件、合并或拆分事件、规范事件名称。这种反复是正常且有益的。

---

## Step 3：Saga 设计（方案域，重流程/轻流程模式）

**目标**：将事件流转化为可实现的分布式事务方案，含补偿机制。

**方法**：
- 主流程事件链 → Saga 正向步骤（T1, T2, T3, ...）
- 逆向事件链 → 补偿步骤（C1, C2, C3, ...），顺序与正向相反
- 明确每个步骤：触发事件、执行 BC、产出事件、补偿动作

**产出**：
- Saga 步骤表
- Saga 日志/状态机设计（重流程模式）
- 编排式 vs 协同式的选择

**纯领域模式**：可跳过本步，或仅产出简单的步骤表。

---

## 关键约定

| 约定 | 说明 |
|------|------|
| **事件与领域对象** | 每个事件应归属某个聚合，命名体现「谁发生了什么事」 |
| **事件与本地事务** | 通常「一个本地事务 → 一个出站事件」，事件在事务提交后发布 |
| **事件优先** | 分析从事件入手，领域建模和 Saga 都以事件流为骨架 |
| **迭代精化** | 事件流 ↔ 领域建模 ↔ Saga 设计 可多次往返，逐步收敛 |

---

## 产出文档位置

分析产出的领域模型、事件契约、Saga 定义，写入：

- `docs/bounded-contexts/<context>/domain-model.md`
- `docs/bounded-contexts/<context>/requirements.md`（若涉及需求编写）
- 视需要新增 `docs/bounded-contexts/<context>/events.md`、`saga-design.md` 等

---

## 与 HMall 的关系

本方法用于 Order 等编排型 BC 的业务分析与架构设计，与 `docs/design-principles.md` 中的 DDD 分层、限界上下文约定互补。分析完成后，可用 `evolve-feature` 等 Skill 实现具体功能。

---

## 参考

- **方法论文档**：`docs/architecture/event-driven-business-analysis.md`
- **设计原则**：`docs/design-principles.md`
- **功能实现**：`evolve-feature` Skill
