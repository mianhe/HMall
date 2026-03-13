# overview.md 格式与约定

> 本文件定义 `analyze-requirement` Skill 产出的 overview.md 的章节结构、格式约定与示例。  
> 写入路径：`docs/business-requirements/<name>/overview.md`

---

## 章节结构

overview.md 由四个章节组成，分别对应 Phase A 的四个步骤：

| 章节 | 对应步骤 | 内容 |
|------|---------|------|
| **一、需求概述与场景** | Step 1 | 为什么做、要达成什么、影响面 + 场景总览表 |
| **二、场景分析（事件流）** | Step 2 | 各场景按深度展开，设计决策内联 |
| **三、变更分析** | Step 3 | 按 BC 分组的详细变更规格 + BC 间数据流 |
| **四、迭代计划** | Step 4 | 可独立验收的迭代拆分 |
| **交付跟踪** | `deliver-requirement` | 按迭代记录工作项执行状态（由交付 Skill 写入） |

---

## 第一章：需求概述与场景

上半部分——需求概述：简要说明业务背景和目标。包含：

- 需求名称与业务目标
- 需求类型（全新 / 扩展已有能力）
- 与已有能力的核心区别（若扩展，用对比表）
- 后端影响面（涉及哪些 BC）
- 前端影响面（哪些页面）

下半部分——场景总览表：将所有场景集中展示。

```markdown
## 一、需求概述与场景

为特定电子产品提供激光雕刻增值服务。...

与现有虚拟服务（碎屏险、延保）的核心区别：

| 维度 | 已有能力 | 本需求 |
|------|---------|--------|
| 履约方式 | 支付后即时激活 | 对实体商品的物理加工，必须在发货前完成 |

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | xxx 主流程 | 主流程 | L3 重分析 | 用户 → ... → 签收 |
| F2 | xxx 配置 | 支撑流程 | L1 轻分析 | 管理员 → ... |
| F3 | xxx 展示 | 支撑流程 | L2 中分析 | 用户进入详情页 → ... |
| F4 | xxx 取消 | 异常流程 | L2 中分析 | 用户 → 取消 → 补偿 |
```

---

## 第二章：场景分析（事件流）

按总览表顺序，逐场景展开。设计决策在分析中自然涌现时，用 blockquote 内联标注。

### L3 重分析：标准事件流表

```markdown
### F1：xxx 主流程（L3）

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|-----|---------|
| 1 | 🟧 EventName | ⌘ CommandName | ⟳ PolicyDescription | BCName | 🆕 新增项描述 |

> **决策 LE1**：镭雕附属于实体履约单，不单独拆单——镭雕是实体商品的物理加工环节，雕刻内容作为实体履约单的 engravingInfo。
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

### L3 数据依赖验证

事件流表完成后，对涉及**决策/分支/门禁**的步骤做数据可达性检查：

```markdown
#### 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 6 | 按类型拆单 | 区分保险 vs 镭雕 | Catalog → Order → Fulfillment | ❌ 需新增 serviceCategory |
| 12 | ship() 门禁 | 是否含镭雕且已完成 | FulfillmentOrder.engravingCompletedAt | ❌ 新增字段 |
```

❌ 项驱动第三章（变更分析）新增模型字段。只检查有决策/分支/门禁的步骤。

### L2 中分析

关键事件/状态变化 + 数据依赖 + 步骤说明 + 影响。**前端场景**：说明页面数据流向、关键组件边界、与 API 的对接点。

### L1 轻分析

自然语言描述操作步骤 + 对主流程的影响边界。

### 查询影响与流程间耦合

有查询逻辑变化时附在相关场景之后；流程间耦合（支撑→主流程消费方式、配置变更影响范围、异常补偿）放在所有场景展开之后。

---

## 第三章：变更分析

**第三章是 Phase B 落地到 BC 文档的唯一输入源。** 必须详细到可以直接"抄写"到各 BC 文档——不是摘要，而是完整规格。

### 每个 BC 的子节结构

按 BC 分组，每个 BC 包含以下子节（按需裁剪——无变更的子节可省略）：

| 子节 | 内容 | 对应 BC 文档 |
|------|------|-------------|
| **影响程度** | 🔴 重大 / 🟡 中等 / 🟢 轻微 / ⚪ 无变更 | — |
| **领域模型变更** | 新增/修改的聚合、实体、值对象（属性名、类型、约束）；不变式变更；状态机变更 | `domain-model.md` |
| **事件流变更** | 新增/修改的领域事件（名称、payload）；新增/修改的 API 端点；集成关系变更 | `event-flow.md` |
| **需求场景变更** | 新增的验收场景（Given/When/Then 要点）；修改的已有场景 | `requirements.md` |

扩展型需求：在每个 BC 的变更描述中标注现有能力与本需求的关系（✅ 可复用 / 🔄 需调整 / 🔲 全新），融入变更描述中。

### 示例

```markdown
### Fulfillment（🔴 重大，🔄 需调整：拆单策略、ship() 前置条件）

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

### 场景分析与变更分析的关系

场景分析从跨 BC 视角讲事件流全貌（横切）；变更分析从单 BC 视角讲具体变更（纵切）。两者互补、不重复。

### 前端变更（若有显著前端改动）

前端变更与各 BC 子节并列，**不归属任何 BC**：

```markdown
### 前端（frontend/admin）（🔲 全新）

#### 新增页面与组件
- `pages/OpsPage.vue`：智能运营主页面，三区域布局（固定指标栏 / 动态画布 / AI 侧边栏）
- `shared/composables/useOpsCanvas.js`：画布状态管理，监听 tool_result 更新渲染状态
- `shared/ui/ops/OpsStatsPanel.vue`：统计数据卡片组，props: `{ data: StatsObject }`

#### 数据流与状态
- `useOpsCanvas` 通过 `onToolCallSuccess` 回调订阅全局 `aiChat`，工具名 → 画布状态机
- 画布状态：`EMPTY | LOADING | STATS | TIMELINE | RECENT`

#### 界面规格（粗粒度）
（ASCII 布局图或文字描述，细节留给 `frontend-development` Skill）

#### 手工验收 checklist
- [ ] 进入页面，固定指标栏展示今日统计
- [ ] AI 侧边栏常驻可见，可正常发消息
- [ ] 输入「最近发生了什么」→ 画布渲染事件列表
```

纯 CRUD 或简单展示的前端改动不需要此节，在迭代计划的「前端」行说明即可。

### BC 间数据流

```markdown
### BC 间数据流

- Catalog `EngravingPattern` → 图案 API → 前端选择
- Order `serviceAttributes` → Fulfillment `EngravingInfo`（创建履约单时传递）
- Fulfillment `EngravingCompleted` → Activity 事件时间线
```

---

## 第四章：迭代计划

将变更拆为可独立验收的迭代。每个迭代标注：

- **涉及 BC** 与 **前置依赖**
- **后端变更**（概要）
- **前端变更**（粗粒度：哪些页面、做什么；UI Spec 细节留给 `frontend-development` Skill）
- **验收标准**
- **E2E 验收**（引入新业务路径的迭代标注用例前缀如 `BIZ-LE-001` + 场景概述，是 `deliver-requirement` 执行门禁的依据）

```markdown
### 迭代 0：图案库（Catalog）

**涉及 BC**：Catalog
**前置依赖**：无

**后端**：EngravingPattern CRUD；图案列表 API。
**前端**：`frontend/admin` 图案库管理页。
**验收**：Admin 可配图案；`GET /api/engraving-patterns` 返回图案列表。
```

---

## 交付跟踪

交付跟踪章节由 `deliver-requirement` Skill 在执行交付时创建和维护。`analyze-requirement` **不写入此章节**，仅在模板中预留位置。

### 格式约定

- 按迭代分组，标题格式 `### 迭代 N：<名称> [✅]`（完成后加 ✅）
- 每个迭代一张工作项跟踪表
- 迭代完成后附**交付日期**和**下一迭代指引**

```markdown
## 交付跟踪

### 迭代 0：图案库（Catalog） ✅

| # | 工作项 | Skill | 状态 | 说明 |
|---|--------|-------|------|------|
| 1 | Catalog: EngravingPattern 域对象 + API | evolve-feature | ✅ 完成 | 6 scenario 全绿 |
| 2 | frontend/admin: 图案库管理页 | frontend-development | ✅ 完成 | 路由 + CRUD |
| 3 | E2E 交付门禁 | deliver-requirement | ✅ 完成 | Smoke P0 通过 |

**交付日期**：2026-03-04
**下一迭代**：迭代 1（xxx），前置依赖：yyy
```

### 状态标记

| 标记 | 含义 |
|------|------|
| ⬜ 待执行 | 尚未开始 |
| 🔄 进行中 | 正在执行 |
| ✅ 完成 | 已完成 |

### 与迭代计划的关系

- **迭代计划**（第四章）由 `analyze-requirement` 写入，定义"做什么"
- **交付跟踪**由 `deliver-requirement` 写入，记录"做到哪了"
- 迭代计划中的迭代标题在交付完成后也应加 `✅ 已完成` 标记
