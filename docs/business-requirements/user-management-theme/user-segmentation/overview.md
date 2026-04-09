# 业务需求：用户分群与圈选

> 所属 Theme：[`用户管理体系`](../theme.md)

---

## 一、需求概述与场景

当前系统已在 Promotion 侧支持“按等级/标签定向配置”，但 User 侧缺少可运营的配置入口与圈选能力，导致配置可写但人群不可控。

本需求目标：

1. 提供用户标签设置能力（单用户维护 + 批量导入可后续迭代）
2. 提供用户等级管理能力（L1/L2/L3 等）
3. 提供圈选能力（按等级/标签规则生成目标用户集并可预览）

需求类型：**扩展已有能力**（已有用户基础信息、登录、地址；新增“用户运营画像”能力）。

与现有能力的核心区别：

| 维度 | 现有能力 | 本需求 |
|------|----------|--------|
| 用户数据 | 账号、密码、地址 | 增加等级、标签、圈选规则 |
| 运营能力 | 无圈选入口 | 可按规则圈选并输出目标用户集 |
| 跨 BC 使用 | Promotion 只能被动读 level/tags | Promotion 可稳定消费可维护的人群数据 |

后端影响面（初判）：

| BC | 影响程度 | 说明 |
|----|----------|------|
| **User** | 🔴 重大 | 新增标签/等级维护、圈选规则与查询能力 |
| **Promotion** | 🟡 中等 | 从“直接读单用户分群”扩展为“可消费圈选结果/规则” |
| **BFF** | 🟢 轻微 | 新增用户管理相关代理路由 |

前端影响面（初判）：

| 端 | 影响程度 | 说明 |
|----|----------|------|
| **frontend/admin** | 🔴 重大 | 新增用户标签/等级维护页面与圈选预览页面 |
| **frontend/web** | ⚪ 无 | 无直接交互改动（仅受促销结果间接影响） |

### 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|---------|-----------|
| F1 | 圈选配置并生成目标用户集 | 主流程 | L3 重分析 | 运营配置圈选规则后，系统返回可用目标用户集供业务消费 |
| F2 | 单用户标签设置 | 支撑流程 | L2 中分析 | 运营为指定用户增删标签并实时生效 |
| F3 | 单用户等级调整 | 支撑流程 | L2 中分析 | 运营调整用户等级并影响后续定向判断 |
| F4 | 规则预览与命中解释 | 支撑流程 | L2 中分析 | 运营可预览规则命中人数与示例用户，避免盲配 |
| F5 | 规则失效与数据异常兜底 | 异常流程 | L2 中分析 | 标签值非法、规则冲突、无命中用户时给出可解释反馈 |

---

## 二、场景分析（事件流）

### F1：圈选配置并生成目标用户集（L3）

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|-----|---------|
| 1 | — | ⌘ CreateSegmentRule(name, conditions) | 校验规则语法与字段合法性（level/tags） | User | 🆕 新增圈选规则写入能力 |
| 2 | 🟧 SegmentRuleCreated | ⌘ SaveRule | 规则状态置为 `DRAFT`，可继续调试 | User | 🆕 规则生命周期起点 |
| 3 | — | ⌘ PreviewSegment(ruleId, sampleSize) | 规则引擎按当前用户主数据实时计算命中用户集 | User | 🆕 实时圈选预览 |
| 4 | 🟧 SegmentPreviewGenerated | ⌘ PreviewSegment | 返回命中人数、示例用户、未命中原因统计 | User | 🆕 可解释预览输出 |
| 5 | — | ⌘ ActivateSegmentRule(ruleId) | 激活前门禁：命中人数 > 0 且规则合法 | User | 🔄 新增激活门禁 |
| 6 | 🟧 SegmentRuleActivated | ⌘ ActivateSegmentRule | 对外暴露只读查询（供 Promotion/BFF） | User | 🆕 供下游消费的人群规则 |
| 7 | — | ⌘ ResolveUserSegments(userId)（Promotion 调用） | 若命中圈选条件，返回标准化 level/tags/segmentHit | User | 🔄 下游查询语义增强 |

> **决策 UM1**：第一版“圈选用户”采用**实时规则计算**，不落静态用户包，先保证一致性与可解释性。  
> **决策 UM2**：圈选能力归属 User BC，Promotion 只消费结果，不维护圈选规则副本。

#### F1 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 1 | 规则合法性校验 | 可用字段白名单（level/tags） | User 规则元数据 | ❌ 需新增 SegmentRule 模型 |
| 3 | 规则实时计算 | 用户 level/tags 全量索引能力 | User 主数据 | 🔄 当前仅单用户读取，需扩展查询能力 |
| 4 | 可解释预览 | 命中/未命中原因统计结构 | 规则引擎输出 | ❌ 需新增 PreviewResult 模型 |
| 6 | 激活门禁 | 命中人数、规则状态 | Rule + Preview snapshot | ❌ 需新增状态机与门禁 |
| 7 | 下游消费一致性 | 标准化响应（level/tags/segmentHit） | User API | 🔄 现有 `/segments` 需增强语义 |

### F2：单用户标签设置（L2）

- 运营在后台按用户维度增删标签（如 `VIP`、`NEW_USER`、`BLACKLIST`）。
- 系统做标签字典校验（是否允许自由标签在迭代中可配置），写入后立即可被圈选规则与 Promotion 消费。
- 影响：User 需要提供单用户标签更新 API，并保证更新后查询一致。

### F3：单用户等级调整（L2）

- 运营调整用户等级（如 `L1 -> L2`），并记录变更时间与操作者（审计可先简化）。
- 级别值需受枚举或配置约束，避免 Promotion 配置等级与 User 实际等级不一致。
- 影响：User 需新增等级更新 API，Promotion 无需改接口但受结果影响。

### F4：规则预览与命中解释（L2）

- 运营在激活前可反复预览：命中人数、示例用户 ID、未命中主要原因（等级不匹配/缺少标签/被排除标签命中）。
- 预览不改变线上生效规则，仅用于决策和调参。
- 影响：User 需提供 Preview API 与解释性返回；admin 需要可视化展示。

### F5：规则失效与数据异常兜底（L2）

- 标签值非法、规则冲突（如 `tagsAny` 与 `excludeTags` 同时包含同值）时，创建/激活应被拒绝并返回明确错误。
- 命中人数为 0 时允许保留 `DRAFT`，但默认不允许激活；可由运营确认后强制激活作为后续迭代能力。
- 下游调用失败时（Promotion 查询 User 超时）采用“可解释降级”：返回默认分群并记录告警。

### 查询影响

- 现有 `GET /api/users/{id}/segments` 从“仅返回 level/tags”扩展为“可携带是否命中指定圈选规则/原因”（按接口拆分决定）。
- 需要新增“按规则预览命中用户集”的查询接口，支撑 admin 圈选调试。

### 流程间耦合

- User 规则配置产出由 Promotion 实时消费，不做长期前端缓存，避免规则生效延迟。
- 标签/等级变更会立即影响圈选预览与 Promotion 定向命中结果，属于“强一致读路径”。
- 异常路径以“提交即校验 + 激活门禁 + 调用降级”兜底，不新增跨 BC 补偿链。

---

## 三、变更分析

### User（🔴 重大，🔄 需调整：从“用户账号域”扩展为“用户运营画像域”）

#### 领域模型变更

- 🔲 新增聚合：`SegmentRule`
  - `ruleId: Long`
  - `name: String`
  - `conditions: SegmentCondition`（包含 `levelsIn/tagsAny/tagsAll/excludeTags`）
  - `status: DRAFT | ACTIVE | INACTIVE`
  - `lastPreviewCount: Long?`
  - `createdAt/updatedAt`
- 🔲 新增值对象：`SegmentCondition`
  - 不变式：
    - `levelsIn/tagsAny/tagsAll/excludeTags` 允许为空集合
    - `tagsAny` 与 `excludeTags` 不允许完全冲突（同值冲突报错）
    - 至少包含一种条件（避免“空规则”）
- 🔄 扩展聚合：`User`
  - 保持 `level: String`、`tags: Set<String>`，新增可变更行为：
    - `updateLevel(newLevel)`
    - `replaceTags(newTags)` / `addTag/removeTag`
- 🔲 新增领域服务：`SegmentRuleEvaluator`
  - 输入：`SegmentCondition` + 用户集合
  - 输出：命中用户集 + 命中/未命中原因统计

#### 事件流变更

- 🔲 新增 API：
  - `PUT /api/users/{id}/level`（更新用户等级）
  - `PUT /api/users/{id}/tags`（全量覆盖用户标签）
  - `POST /api/users/segment-rules`（创建圈选规则）
  - `POST /api/users/segment-rules/{id}/preview`（预览命中）
  - `POST /api/users/segment-rules/{id}/activate`（激活规则）
  - `GET /api/users/segment-rules/{id}`（规则详情）
  - `GET /api/users/segment-rules`（规则列表）
- 🔄 调整 API：
  - `GET /api/users/{id}/segments`：保持兼容，返回 level/tags；后续可扩展命中信息字段（可选 query 参数）

#### 需求场景变更

- 🔲 新增 Feature：用户分群管理
  - 规则创建成功并进入 DRAFT
  - 规则预览返回命中人数与原因统计
  - 命中人数 > 0 方可激活
  - 非法规则创建/激活失败
- 🔲 新增 Feature：用户画像维护
  - 调整用户等级后查询可见
  - 调整用户标签后圈选预览结果变化可见

---

### Promotion（🟡 中等，✅ 可复用：保持“只消费用户分群结果”）

#### 领域模型变更

- ✅ 复用既有 `TargetingRule` 与算价命中逻辑
- 🔄 可选增强：
  - 在 `UserSegmentResolver` 请求参数中支持 `ruleId`（如后续按圈选规则直接消费）
  - 目前阶段可保持按 `level/tags` 判定，不强依赖 `ruleId`

#### 事件流变更

- 🔄 现有同步查询链路保留：Promotion -> User（同步查询）
- 🔲 可选新增查询端点消费（若采用规则 ID 直查）：
  - `GET /api/users/segment-rules/{id}/resolve?userId=...`

#### 需求场景变更

- 🔄 修改已有“定向活动命中”场景：
  - 增加“用户标签/等级更新后，命中结果实时变化”断言
  - 增加“User 查询失败时默认降级 + 可解释提示”断言

---

### BFF（🟢 轻微，🔄 需调整：新增透传路由）

#### 事件流变更

- 🔄 新增 `/api/users/segment-rules**` 路由透传
- 🔄 新增 `/api/users/{id}/level`、`/api/users/{id}/tags` 路由透传
- ✅ 保持现有 `/api/users/{id}/segments` 透传

#### 需求场景变更

- 🔲 新增路由透传契约测试（成功/失败状态码透传）

---

### 前端（frontend/admin）（🔴 重大）

#### 新增/修改页面与组件

- 🔲 新增页面：`UserSegmentationPage`
  - 用户检索与画像编辑区（等级、标签）
  - 圈选规则编辑区（levels/tags 组合）
  - 预览区（命中人数、示例用户、原因分布）
- 🔄 可能复用组件：
  - 标签输入组件（逗号分隔 -> tag chips）
  - 规则条件构建组件（与 Promotion 定向规则字段保持一致命名）

#### 数据流与状态

- 页面状态至少包含：
  - `selectedUser`
  - `userLevel`, `userTags`
  - `ruleDraft`
  - `previewResult`（count/samples/reasons）
- 数据流：
  - 用户画像更新 -> User API -> 页面本地状态回填
  - 规则预览 -> Preview API -> 预览区渲染
  - 规则激活 -> Activate API -> 列表状态刷新

#### 界面规格（粗粒度）

- 左侧：用户画像编辑（用户 ID 搜索 + 等级 + 标签）
- 右侧：圈选规则编辑与预览（条件配置、预览按钮、激活按钮）
- 底部：规则列表（状态、最近预览人数、更新时间）

#### 手工验收 checklist

- [ ] 修改用户等级后，重新预览规则，命中人数按预期变化
- [ ] 增删标签后，预览示例用户与原因统计同步变化
- [ ] 非法规则提交时前端展示后端错误 message
- [ ] 命中人数为 0 时激活按钮不可用或激活失败提示明确

---

### 前端（frontend/web）（⚪ 无变更）

- 当前需求无直接页面改动；用户仍通过既有购物路径使用促销能力。
- 间接效果由 Promotion 定向结果体现（在商品/结账页已具备可解释展示）。

---

### BC 间数据流

- `admin` -> `BFF` -> `User`：
  - 写入用户等级/标签
  - 创建/预览/激活圈选规则
- `Promotion` -> `User`：
  - 实时查询用户分群数据（level/tags，后续可扩展 ruleId 解析）
- `User` -> `admin`：
  - 返回预览统计（命中人数/样例/原因）

数据一致性约束：

- User 为分群主数据唯一来源（Single Source of Truth）
- Promotion 不持久化用户圈选副本，仅做实时消费与短期调用级缓存（如有）

---

## 四、迭代计划

### 迭代 0：User 分群底座（等级/标签 + 圈选规则） ✅ 已完成

**涉及 BC**：User  
**前置依赖**：无

**后端变更**：
- 用户等级更新 API、用户标签更新 API
- 圈选规则模型（创建/查询/预览/激活）
- 规则预览结果（命中人数 + 可解释原因）

**前端变更**：
- 无必做页面（可后置到迭代 1）

**验收标准**：
- 可通过 API 完成单用户等级/标签维护
- 可创建规则并拿到预览结果
- 命中人数为 0 时默认不可激活

---

### 迭代 1：管理端配置闭环 + BFF 路由 ✅ 已完成

**涉及 BC**：frontend/admin、BFF、User  
**前置依赖**：迭代 0

**后端变更**：
- BFF 新增用户分群相关路由透传
- User API 错误语义与返回结构稳定化（便于 UI 展示）

**前端变更**：
- admin 新增用户分群页面：用户画像编辑 + 规则预览 + 激活

**验收标准**：
- 运营可在 admin 页面完成等级/标签维护
- 运营可在 admin 页面创建规则并预览命中情况
- 错误与门禁提示可解释（规则冲突/无命中）

---

### 迭代 2：Promotion 联动与端到端一致性 ✅ 已完成

**涉及 BC**：Promotion、User、frontend/admin  
**前置依赖**：迭代 0、迭代 1

**后端变更**：
- Promotion 消费 User 分群数据保持实时一致
- 用户画像变更后，Promotion 定向命中结果即时生效

**前端变更**：
- 无新增页面，重点验证既有促销页面行为一致性

**验收标准**：
- 修改用户等级/标签后，定向促销命中结果按预期变化
- Promotion 调用 User 失败时有可解释降级

**E2E 验收**：

| 用例 | 场景概述 |
|------|---------|
| BIZ-UM-001 | 运营调整用户等级后，用户下单命中定向活动 |
| BIZ-UM-002 | 运营调整用户标签后，圈选预览与促销命中一致 |
| BIZ-UM-003 | 规则无命中时激活失败并给出可解释原因 |

---

## 一致性检查

| 维度 | 检查项 | 结果 |
|------|--------|------|
| 场景完整 | 主流程 L3、支撑流程 L2、异常流程 L2 是否覆盖 | ✅ 覆盖 F1~F5 |
| 事件完整 | 主成功路径 + 门禁/异常路径是否完整 | ✅ F1/F5 已覆盖 |
| 数据可达 | L3 中 ❌ 数据缺口是否在变更分析补齐 | ✅ SegmentRule/PreviewResult/门禁已落点 |
| 场景↔变更 | 场景影响与 BC 变更是否一一对应 | ✅ User/Promotion/BFF/admin 均有映射 |
| 变更内部 | 模型、API、场景描述是否自洽 | ✅ User 为单一主数据源 |
| 前端 | 显著前端改动是否给出规格与验收 checklist | ✅ admin 已覆盖 |
| 扩展一致性 | 与既有 Promotion 定向能力是否兼容 | ✅ Promotion 继续消费 User 结果 |

---

## Phase B 落地状态

- ✅ User BC 文档已同步（requirements/domain-model/api）
- ✅ Promotion BC 文档已同步（requirements/domain-model）
- ✅ 系统文档已同步（context-map/business-flows/project-status）
- ✅ 前端规格文档已同步（frontend/admin/ui-spec）

---

## 交付跟踪

### 迭代 0：User 分群底座（等级/标签 + 圈选规则） ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | User：等级/标签更新 API、圈选规则创建/预览/激活 | evolve-feature | — | ✅ 完成 |
| 2 | User：ATDD 场景补齐（含冲突校验、0 命中激活失败） | evolve-feature | #1 | ✅ 完成 |
| 3 | User：`mvn test -q` 验证 | deliver-requirement | #1,#2 | ✅ 完成 |

**交付日期**：2026-03-16  
**下一迭代**：迭代 1（管理端闭环）

### 迭代 1：管理端配置闭环 + BFF 路由 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | admin：新增 `UserSegmentationPage` + API 客户端 + 路由/导航 | frontend-development | 迭代0 | ✅ 完成 |
| 2 | BFF：确认 `/api/users**` 路由透传覆盖新增接口 | integration | 迭代0 | ✅ 完成 |
| 3 | admin：`BIZ-UM-002`、`BIZ-UM-003` E2E 通过 | deliver-requirement | #1,#2 | ✅ 完成 |

**交付日期**：2026-03-16  
**下一迭代**：迭代 2（Promotion 联动）

### 迭代 2：Promotion 联动与端到端一致性 ✅

| # | 工作项 | Skill | 依赖 | 状态 |
|---|--------|-------|------|------|
| 1 | web Business E2E：新增 `BIZ-UM-001`（等级变更后定向命中变化） | frontend-development | 迭代0,1 | ✅ 完成 |
| 2 | 全链路验证：web Smoke P0 回归 | deliver-requirement | #1 | ✅ 完成 |
| 3 | Promotion/User 回归测试（`mvn test -q`） | deliver-requirement | #1 | ✅ 完成 |

**交付日期**：2026-03-16  
**下一步**：本需求已全部交付（迭代 0/1/2 全部完成）

### 备注（非阻塞遗留）

- admin 全量 Business E2E 中既有镭雕用例 `BIZ-LE-002/003` 存在历史不稳定；本次需求相关用例 `BIZ-UM-001/002/003` 已全部通过，不影响本需求闭环。
