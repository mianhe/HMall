# overview.md 模板

> 本文件是 `analyze-requirement` Skill 的参考资产。Phase A Step 4 产出 overview.md 时，按此模板组织内容。

写入路径：`docs/business-requirements/<name>/overview.md`

---

## 章节结构

| 章节 | 内容 |
|------|------|
| 一、背景与目标 | 为什么做、要达成什么 |
| 二、业务场景与事件流 | 先场景总览（全部流程一览表）→ 再按深度展开各场景 |
| 三、设计决策 | 决策记录表 |
| 四、各 BC 变更规格 | 按 BC 分组的详细变更规格（领域模型、事件流、需求场景） |
| 四a、复用分析（扩展型需求） | 已有能力与本需求的关系（✅ 可复用 / 🔄 需调整 / 🔲 全新），明确前置依赖 |
| 五、迭代计划（若需要） | 拆分为可独立验收的迭代，明确依赖顺序 |

---

## §一 背景与目标

简要说明业务背景和目标。如果本需求引入了与已有能力的核心区别，用对比表呈现：

```markdown
## 一、背景与目标

为特定电子产品提供激光雕刻增值服务。...

与现有虚拟服务（碎屏险、延保）的核心区别：

| 维度 | 已有能力 | 本需求 |
|------|---------|--------|
| 履约方式 | 支付后即时激活 | 对实体商品的物理加工，必须在发货前完成 |
| ... | ... | ... |
```

---

## §二 业务场景与事件流

### 内部结构

1. **场景总览表**——将流程盘点的所有场景集中展示，一目了然：

```markdown
| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | xxx 主流程 | 主流程 | L3 重分析 | 用户 → ... → 签收 |
| F2 | xxx 配置 | 支撑流程 | L1 轻分析 | 管理员 → ... |
| F3 | xxx 取消 | 异常流程 | L2 中分析 | 用户 → 取消 → 补偿 |
```

2. **各场景展开**——按总览表的顺序，逐一展开：

   - **L3 场景**：使用标准事件流表

```markdown
### F1：xxx 主流程（L3）

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|-----|---------|
| 1 | 🟧 EventName | ⌘ CommandName | ⟳ PolicyDescription | BCName | 🆕 新增项描述 |
```

   - **L2 场景**：关键事件 + 状态变化
   - **L1 场景**：步骤描述 + 影响边界

3. **数据依赖验证**（仅 L3）——事件流表完成后，对涉及**决策/分支/门禁**的步骤做数据可达性检查：

```markdown
#### 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 6 | Fulfillment 按类型拆单 | 区分保险 vs 镭雕 | Catalog.Spu.productType → Order.itemType → Fulfillment | ❌ 仅 PHYSICAL/SERVICE |
| 12 | ship() 门禁 | 是否含镭雕且已完成 | Fulfillment.engravingCompletedAt | ❌ 新增字段 |
```

❌ 项驱动 Step 3 新增模型字段或概念。不需要每步都列——只检查有**决策、分支、类型判断、门禁**的步骤。

4. **查询影响**——有查询逻辑变化时，附在相关场景之后

### 事件流表列说明

| 列 | 含义 | 注意 |
|----|------|------|
| **Event 🟧** | 领域事件——已发生的业务事实 | 事件是结果，不是意图 |
| **Command ⌘** | 触发该事件的用户操作或系统命令 | 谁发起了这个动作 |
| **Policy / Rule ⟳** | 事件触发的后续自动反应、门禁条件、校验规则 | 消费方的反应写在这里 |
| **BC** | 事件的**归属方（发布者）**，只标注谁发布 | **不标注消费方向**，消费方在 Policy 列表达 |
| **影响识别** | 该步骤引入的新增/变更 | 🆕 标记新增项 |

主成功路径和补偿路径分别建表。补偿路径表最后一列改为"与现有差异"。

---

## §三 设计决策

```markdown
| # | 决策 | 说明 |
|---|------|------|
| LE1 | 决策简述 | 详细说明 |
```

---

## §四 各 BC 变更规格

**§四 是 Phase B 落地到 BC 文档的唯一输入源。** 必须详细到可以直接"抄写"到各 BC 文档——不是摘要，而是完整规格。

### 每个 BC 的子节结构

按 BC 分组，每个 BC 包含以下子节（按需裁剪——无变更的子节可省略）：

| 子节 | 内容 | 对应 BC 文档 |
|------|------|-------------|
| **影响程度** | 🔴 重大 / 🟡 中等 / 🟢 轻微 / ⚪ 无变更 | — |
| **领域模型变更** | 新增/修改的聚合、实体、值对象（属性名、类型、约束）；不变式变更；状态机变更（新状态、新转换） | `domain-model.md` |
| **事件流变更** | 新增/修改的领域事件（名称、payload）；新增/修改的 API 端点（方法、路径、请求/响应概要）；集成关系变更 | `event-flow.md` |
| **需求场景变更** | 新增的验收场景（Given/When/Then 要点）；修改的已有场景；场景编号预分配 | `requirements.md` |

### 示例

```markdown
### Fulfillment（🔴 重大）

#### 领域模型变更
- FulfillmentOrder 聚合新增 `engravingInfo: EngravingInfo`（可选值对象）
  - EngravingInfo: { patternId: Long?, patternName: String?, text: String? }
- 新增 `engravingCompletedAt: Instant`（可选，非空表示雕刻已完成）
- 新增不变式：engravingInfo 非空时，ship() 要求 engravingCompletedAt 已设
- 新增领域事件：EngravingCompleted

#### 事件流变更
- createFulfillment API 扩展：items 携带 itemType（PHYSICAL/INSURANCE/PRODUCTION）+ serviceAttributes
- 新增 API：POST /api/fulfillment/{id}/complete-engraving
- 新增事件：EngravingCompleted → topic: fulfillment.engraving.completed
  - payload: { orderId, fulfillmentOrderId, completedAt, occurredAt }

#### 需求场景变更
- 🔲 新增 1.8：创建含镭雕的履约单 → 提取 engravingInfo
- 🔲 新增 2a：完成雕刻 feature（2a.1-2a.5）
- 🔄 修改 3.3：发货前置条件扩展（engravingInfo 非空时需先完成雕刻）
```

### §四 与 §二 的关系

§二 从跨 BC 视角讲事件流全貌（横切）；§四 从单 BC 视角讲该 BC 需要做什么变更（纵切）。两者互补、不重复。

### §四a 复用分析（扩展型需求）

仅在需求是对已有能力的扩展时需要。标注每个受影响 BC 的现有能力与本需求的关系：

```markdown
| BC | 现有能力 | 关系 | 说明 |
|----|---------|------|------|
| Order | OrderLineItem.serviceAttributes | ✅ 可复用 | 已有虚拟服务随购的 serviceAttributes 机制 |
| Fulfillment | 拆单策略 | 🔄 需调整 | 需要根据 itemType 区分 INSURANCE 与 PRODUCTION |
| Catalog | productType | 🔄 需调整 | 从二分（PHYSICAL/SERVICE）改为三分（PHYSICAL/INSURANCE/PRODUCTION） |
```

标注 ✅ 可复用 / 🔄 需调整 / 🔲 全新，并说明前置依赖（如"依赖虚拟商品迭代 1 已完成"）。

### BC 间数据流

§四 末尾附一节 **BC 间数据流**，明确跨 BC 数据传递关系：

```markdown
### BC 间数据流

- Catalog `Spu.productType` → Order `OrderLineItem.itemType`（创建订单时从 Catalog 快照）
- Order `OrderLineItem.serviceAttributes` → Fulfillment `EngravingInfo`（创建履约单时传递）
- Fulfillment `EngravingCompleted` → Order 雕刻进度（订单明细展示）
- Fulfillment `EngravingCompleted` → Activity 事件时间线（订单旅程回放）
```

---

## §五 迭代计划

### 前端变更

每个迭代应标注受影响的前端页面和变更要点（粗粒度：哪些页面、做什么）。不需要写 UI Spec 细节——具体规格留到后端 API 契约确定后，由 `frontend-development` Skill 编写。

### E2E 验收

引入新业务路径的迭代必须标注 **E2E 验收条件**——用例 ID 前缀（如 `BIZ-LE-001`）和验收场景概述。此标注是 `deliver-requirement` 执行 E2E 交付门禁的依据。未标注的迭代视为不需要新增 E2E。

### 迭代示例

```markdown
### 迭代 0：productType 三分重构

**涉及 BC**：Catalog、Order、Fulfillment
**前置依赖**：无

**后端变更**：
- ProductType 枚举由 PHYSICAL | SERVICE 改为 PHYSICAL | INSURANCE | PRODUCTION
- 既有 SERVICE 数据迁移为 INSURANCE
- Order/Fulfillment 的 ItemType 同步调整

**前端变更**：
- `frontend/admin`：productType 展示适配（INSURANCE/PRODUCTION 标签）

**验收标准**：既有流程不变，仅类型名变化；mvn test 全绿。
```
