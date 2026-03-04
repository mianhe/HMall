# overview.md 格式与约定

> 本文件定义 `analyze-requirement` Skill 产出的 overview.md 的章节结构、格式约定与示例。  
> 写入路径：`docs/business-requirements/<name>/overview.md`

---

## 章节结构

overview.md 由五个章节组成，分别对应 Phase A 的五个步骤：

| 章节 | 对应步骤 | 内容 |
|------|---------|------|
| **§一 背景与目标** | Step 1 | 为什么做、要达成什么、影响面 |
| **§二 业务场景与事件流** | Step 2 + 3 | 场景总览表 → 各场景按深度展开 |
| **§三 设计决策** | Step 3（渐进积累） | 分析过程中产生的关键决策 |
| **§四 各 BC 变更规格** | Step 4 | 按 BC 分组的详细变更规格 |
| **§五 迭代计划** | Step 5 | 可独立验收的迭代拆分 |

---

## §一 背景与目标

简要说明业务背景和目标。包含：

- 需求名称与业务目标
- 需求类型（全新 / 扩展已有能力）
- 与已有能力的核心区别（若扩展，用对比表）
- 后端影响面（涉及哪些 BC）
- 前端影响面（哪些页面）

```markdown
## 一、背景与目标

为特定电子产品提供激光雕刻增值服务。...

与现有虚拟服务（碎屏险、延保）的核心区别：

| 维度 | 已有能力 | 本需求 |
|------|---------|--------|
| 履约方式 | 支付后即时激活 | 对实体商品的物理加工，必须在发货前完成 |
```

---

## §二 业务场景与事件流

### 场景总览表

Step 2 写入。将所有场景集中展示：

```markdown
| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | xxx 主流程 | 主流程 | L3 重分析 | 用户 → ... → 签收 |
| F2 | xxx 配置 | 支撑流程 | L1 轻分析 | 管理员 → ... |
| F3 | xxx 展示 | 支撑流程 | L2 中分析 | 用户进入详情页 → ... |
| F4 | xxx 取消 | 异常流程 | L2 中分析 | 用户 → 取消 → 补偿 |
```

### 各场景展开

Step 3 写入。按总览表顺序，逐场景展开：

#### L3 重分析：标准事件流表

```markdown
### F1：xxx 主流程（L3）

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|-----|---------|
| 1 | 🟧 EventName | ⌘ CommandName | ⟳ PolicyDescription | BCName | 🆕 新增项描述 |
```

事件流表列说明：

| 列 | 含义 | 注意 |
|----|------|------|
| **Event 🟧** | 领域事件——已发生的业务事实 | 事件是结果，不是意图 |
| **Command ⌘** | 触发该事件的用户操作或系统命令 | 谁发起了这个动作 |
| **Policy / Rule ⟳** | 事件触发的后续自动反应、门禁条件、校验规则 | 消费方的反应写在这里 |
| **BC** | 事件的**归属方（发布者）** | 不标注消费方向，消费方在 Policy 列表达 |
| **影响识别** | 该步骤引入的新增/变更 | 🆕 标记新增项 |

主成功路径和补偿路径分别建表。补偿路径表最后一列改为"与现有差异"。

#### L3 数据依赖验证

事件流表完成后，对涉及**决策/分支/门禁**的步骤做数据可达性检查：

```markdown
#### 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 6 | 按类型拆单 | 区分保险 vs 镭雕 | Catalog → Order → Fulfillment | ❌ 需新增 serviceCategory |
| 12 | ship() 门禁 | 是否含镭雕且已完成 | FulfillmentOrder.engravingCompletedAt | ❌ 新增字段 |
```

❌ 项驱动 Step 4 新增模型字段。只检查有决策/分支/门禁的步骤。

#### L2 中分析

关键事件/状态变化 + 数据依赖 + 步骤说明 + 影响。

#### L1 轻分析

自然语言描述操作步骤 + 对主流程的影响边界。

#### 查询影响与流程间耦合

有查询逻辑变化时附在相关场景之后；流程间耦合（支撑→主流程消费方式、配置变更影响范围、异常补偿）放在所有场景展开之后。

---

## §三 设计决策

Step 3 分析过程中渐进积累，Step 4 可补充。

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
| **领域模型变更** | 新增/修改的聚合、实体、值对象（属性名、类型、约束）；不变式变更；状态机变更 | `domain-model.md` |
| **事件流变更** | 新增/修改的领域事件（名称、payload）；新增/修改的 API 端点；集成关系变更 | `event-flow.md` |
| **需求场景变更** | 新增的验收场景（Given/When/Then 要点）；修改的已有场景 | `requirements.md` |

### 示例

```markdown
### Fulfillment（🔴 重大）

#### 领域模型变更
- FulfillmentOrder 新增 `engravingInfo: EngravingInfo`（可选值对象）
  - EngravingInfo: { patternId: Long?, patternName: String?, text: String? }
- 新增 `engravingCompletedAt: Instant`（可选，非空表示雕刻已完成）
- 新增不变式：engravingInfo 非空时，ship() 要求 engravingCompletedAt 已设

#### 事件流变更
- 新增 API：POST /api/fulfillment/{id}/complete-engraving
- 新增事件：EngravingCompleted → topic: fulfillment.engraving.completed
  - payload: { orderId, fulfillmentOrderId, completedAt, occurredAt }

#### 需求场景变更
- 🔲 新增 1.8：创建含镭雕的履约单 → 提取 engravingInfo
- 🔲 新增 2a：完成雕刻 feature（2a.1-2a.5）
- 🔄 修改 3.3：发货前置条件扩展（有镭雕时需先完成）
```

### §四 与 §二 的关系

§二 从跨 BC 视角讲事件流全貌（横切）；§四 从单 BC 视角讲具体变更（纵切）。两者互补、不重复。

### 复用分析（扩展型需求）

仅在需求是对已有能力的扩展时需要。标注每个受影响 BC 的现有能力与本需求的关系：

```markdown
| BC | 现有能力 | 关系 | 说明 |
|----|---------|------|------|
| Order | serviceAttributes 机制 | ✅ 可复用 | 镭雕内容放 serviceAttributes |
| Fulfillment | 拆单策略 | 🔄 需调整 | 实体单需携带 engravingInfo |
| Catalog | productType | 🔄 需调整 | 新增 serviceCategory 枚举 |
```

标注 ✅ 可复用 / 🔄 需调整 / 🔲 全新，说明前置依赖。

### BC 间数据流

§四 末尾附 BC 间数据流，明确跨 BC 数据传递链：

```markdown
### BC 间数据流

- Catalog `Spu.serviceCategory` → 可选服务 API → 前端识别服务种类
- Order `serviceAttributes` → Fulfillment `EngravingInfo`（创建履约单时传递）
- Fulfillment `EngravingCompleted` → Activity 事件时间线
```

---

## §五 迭代计划

将变更拆为可独立验收的迭代。每个迭代标注：

- **涉及 BC** 与 **前置依赖**
- **后端变更**（概要）
- **前端变更**（粗粒度：哪些页面、做什么；UI Spec 细节留给 `frontend-development` Skill）
- **验收标准**
- **E2E 验收**（引入新业务路径的迭代标注用例前缀如 `BIZ-LE-001` + 场景概述，是 `deliver-requirement` 执行门禁的依据）

```markdown
### 迭代 0：图案库与服务类型（Catalog）

**涉及 BC**：Catalog
**前置依赖**：无

**后端**：SPU 新增 serviceCategory；EngravingPattern CRUD；可选服务 API 返回 serviceCategory。
**前端**：`frontend/admin` 图案库管理页。
**验收**：Admin 可配图案；可选服务 API 含 serviceCategory；前端可识别镭雕。
```
