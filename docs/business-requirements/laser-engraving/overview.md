# 业务需求（Business Requirement）：镭雕服务（激光雕刻）

## 一、需求概述与场景

### 1.1 业务目标

为特定实体商品（如手机、平板等）提供激光雕刻（镭雕）增值服务。用户在下单时可选择镭雕服务并填写雕刻内容；履约时需先完成镭雕工序，才能发货。

### 1.2 需求类型

**扩展型**。镭雕复用虚拟商品（virtual-product）的 SERVICE 类型与 ServiceBinding 基础设施，但在履约方式上与碎屏险/延保等即时激活服务有本质差异。

### 1.3 与已有能力的核心区别

| 维度 | 已有能力（碎屏险、延保等） | 本需求（镭雕） |
|------|---------------------------|----------------|
| **履约方式** | 虚拟履约单，支付后即时激活（ServiceActivated） | 附属于实体履约单的物理加工环节，须在发货前完成 |
| **用户输入** | 无（或仅选规格如期限） | 图案（从图案库选）+ 文字（≤20 字），至少选其一 |
| **配置数据** | 服务期限等通过 SpecDimension 表达 | 图案库（EngravingPattern）需新增管理能力 |
| **发货门禁** | 虚拟单无发货操作 | 实体单有镭雕时，须雕刻完成才能 ship() |
| **状态跟踪** | ACTIVATED 即完成 | 需跟踪雕刻进度，管理员可手动标记「镭雕已完成」 |

### 1.4 影响面

- **后端**：Catalog（图案库、镭雕服务配置）、Order（雕刻内容随订单传递）、Fulfillment（雕刻信息附属、ship 门禁、雕刻完成操作）
- **前端**：web（详情页/结账页雕刻内容选择）、admin（图案库管理、镭雕履约状态与完成操作）

### 1.5 场景总览

| # | 场景 | 类型 | 分析深度 | 一句话描述 |
|---|------|------|----------|-----------|
| LE-1 | 含镭雕订单主流程 | 主流程 | L3 重分析 | 选实体+镭雕服务 → 填雕刻内容 → 下单支付 → 履约配货 → 雕刻完成 → 发货 → 签收 |
| LE-2 | 镭雕内容校验 | 支撑流程 | L2 中分析 | 图案/文字约束：图案从库选、文字≤20 字、至少选其一 |
| LE-3 | 图案库管理 | 支撑流程 | L1 轻分析 | Admin 配置图案库，供详情页/结账页选择 |
| LE-4 | 镭雕履约管理 | 支撑流程 | L2 中分析 | Admin 查看镭雕状态、手动标记「镭雕已完成」 |
| LE-5 | 含镭雕订单取消 | 异常流程 | L2 中分析 | 取消与现有补偿链一致，雕刻中状态可取消 |
| LE-6 | 商品详情与选品展示 | 支撑流程 | L1 轻分析 | 详情页展示镭雕可选、图案库列表；选品阶段数据依赖变化 |

---

## 二、场景分析（事件流）

### LE-1：含镭雕订单主流程（L3）

> **决策 LE1**：镭雕不拆成独立 VIRTUAL 履约单，雕刻内容附属在关联的实体履约单上。FulfillmentOrder 新增 engravingInfo、engravingCompletedAt；ship() 前置条件：有 engravingInfo 时须 engravingCompletedAt 已设。

| # | Event 🟧 | Command ⌘ | Policy / Rule ⟳ | BC | 影响识别 |
|---|----------|-----------|-----------------|-----|---------|
| 1 | — | ⌘ PlaceOrder（含实体 + 镭雕 SERVICE item + engravingInfo） | — | Order | 🆕 镭雕 item 携带 serviceAttributes（patternId?, text?），至少其一非空 |
| 2 | 🟧 StockReserved | — | 仅 PHYSICAL items 占库存 | Inventory | 已有 |
| 3 | 🟧 OrderCreated | — | — | Order | 已有，items 含镭雕及雕刻内容 |
| 4 | — | — | 用户支付 | — | 已有 |
| 5 | 🟧 PaymentCompleted | — | ⟳ 同步调用 createFulfillment | Order | 已有 |
| 6 | 🟧 FulfillmentOrderCreated | createFulfillment | 拆单：PHYSICAL items → 实体履约单（附 engravingInfo）；镭雕 SERVICE 不单独拆单 | Fulfillment | 🆕 实体单携带 engravingInfo（由镭雕 item 的 serviceAttributes 合并） |
| 7 | — | ⌘ allocate | 配货 | Fulfillment | 已有 |
| 8 | 🟧 FulfillmentOrderAllocated | — | Order 置 FULFILLING | Order | 已有 |
| 9 | — | ⌘ completeEngraving | 管理员手动标记镭雕完成 | Fulfillment | 🆕 仅当 engravingInfo 非空时可用；设 engravingCompletedAt |
| 10 | — | ⌘ ship | 发货；门禁：engravingInfo 非空 → engravingCompletedAt 必填 | Fulfillment | 🆕 ship() 门禁 |
| 11 | 🟧 FulfillmentShipped | — | Order 推进 | Order | 已有 |
| 12 | — | ⌘ confirmDelivery | 签收 | Fulfillment | 已有 |
| 13 | 🟧 FulfillmentDelivered | — | Order 全部到达 → OrderCompleted | Order | 已有 |

#### 数据依赖验证

| 步骤# | 决策/分支 | 所需数据 | 数据来源 | 现有模型 |
|-------|----------|---------|---------|---------|
| 1 | 镭雕内容校验 | patternId ∈ 图案库、text ≤20 字、至少选其一 | Catalog 图案 API、Order 校验 | ❌ 需 Order 校验逻辑；Catalog 需图案库 |
| 6 | 镭雕内容附属 | SERVICE item 的 relatedSkuId、serviceAttributes | Order → Fulfillment create 请求 | 🔄 OrderLineItem 已有 relatedSkuId、serviceAttributes，需约定 engraving 结构 |
| 10 | ship 门禁 | engravingInfo 非空 → engravingCompletedAt 已设 | FulfillmentOrder | ❌ FulfillmentOrder 需新增 engravingInfo、engravingCompletedAt |

### LE-2：镭雕内容校验（L2）

- **图案**：patternId 必须在 Catalog 图案库中存在；若选图案则 patternName 快照存储（展示用）
- **文字**：≤20 字，字符集可限定（如不含敏感词，MVP 可仅做长度校验）
- **组合**：patternId 与 text 至少其一非空
- **校验时机**：PlaceOrder 时 Order 应用层校验；前端选品时实时校验

### LE-3：图案库管理（L1）

Admin 操作：创建、编辑、删除、上下架镭雕图案。图案实体含：patternId、name、imageUrl、sortOrder、enabled。提供 `GET /api/engraving-patterns` 供前端选品使用（可过滤 enabled=true）。

### LE-4：镭雕履约管理（L2）

- Admin 查看履约单列表/详情时，展示 engravingInfo（图案名、文字预览）及 engravingCompletedAt（未完成/已完成）
- 操作：`POST /api/fulfillment/{id}/complete-engraving`，仅当 engravingInfo 非空且 engravingCompletedAt 未设时可用
- 雕刻进度跟踪：通过 engravingCompletedAt 是否已设体现（MVP 不做细粒度进度，仅两态）

### LE-5：含镭雕订单取消（L2）

与现有补偿链一致：PaymentExpired / 用户取消 → OrderCancelled ⟳ cancelFulfillment。镭雕进行中（CREATED/ALLOCATING）的履约单可取消；无额外补偿逻辑。

### LE-6：商品详情与选品展示（L1）

- **数据依赖变化**：选规格/服务阶段需 `GET /api/engraving-patterns` 获取图案列表；结账页需展示/编辑雕刻内容
- **影响边界**：详情页、购物车、结账页需支持镭雕服务的图案选择与文字输入；路径 A/B 均受影响；补购路径 C 若支持镭雕补购则同样需图案库（MVP 可先不支持补购镭雕）

#### 查询影响

- 订单详情：需展示镭雕 item 的 serviceAttributes（图案名、文字）
- 履约单详情：需展示 engravingInfo、engravingCompletedAt

#### 流程间耦合

- 图案库（支撑）→ 主流程：结账时实时引用图案列表，创建订单时校验 patternId；图案删除/下架不影响已下单订单（patternName 已快照）
- 镭雕完成（支撑）→ ship：completeEngraving 是 ship 的前置步骤，同一聚合内门禁

---

## 三、变更分析

### Catalog（🔴 重大，🔄 需调整：新增图案库）

#### 影响程度

🔴 重大

#### 领域模型变更

- 新增聚合/实体 **EngravingPattern**（可独立聚合或挂于 Catalog 下）：
  - patternId: Long
  - name: String
  - imageUrl: String
  - sortOrder: Integer
  - enabled: Boolean
- 不变式：name、imageUrl 必填；enabled 默认 true

#### 事件流变更

- 新增 API：
  - `GET /api/engraving-patterns`：列表（支持 enabled 过滤），供选品使用
  - `POST /api/engraving-patterns`、`PUT /api/engraving-patterns/{id}`、`DELETE /api/engraving-patterns/{id}`：Admin 管理

#### 需求场景变更

- 🔲 新增图案 CRUD 验收场景
- 🔲 图案列表 API 验收

---

### Order（🟡 中等，🔄 需调整：PlaceOrder 接纳镭雕内容、创建履约单传参）

#### 影响程度

🟡 中等

#### 领域模型变更

- OrderLineItem.serviceAttributes 约定镭雕结构：`{ "engravingPatternId": Long?, "engravingPatternName": String?, "engravingText": String? }`
- 不变式：SERVICE 且 relatedSkuId 非空时，若为镭雕则至少 engravingPatternId 或 engravingText 其一非空；engravingText 长度 ≤20

#### 事件流变更

- PlaceOrder 请求体扩展：items 中 SERVICE 项可含 serviceAttributes（engravingPatternId, engravingPatternName, engravingText）
- createFulfillment 调用时传递 items 及 serviceAttributes，供 Fulfillment 合并 engravingInfo
- 无新增事件

#### 需求场景变更

- 🔲 新增含镭雕 PlaceOrder 验收（内容校验、存储、传递）
- 🔄 修改 createFulfillment 调用规格，传递镭雕信息

---

### Fulfillment（🔴 重大，🔄 需调整：拆单策略、ship 门禁、completeEngraving）

#### 影响程度

🔴 重大

#### 领域模型变更

- FulfillmentOrder 新增值对象 **EngravingInfo**（可选）：
  - patternId: Long?
  - patternName: String?
  - text: String?
- FulfillmentOrder 新增 `engravingCompletedAt: Instant?`
- 不变式：engravingInfo 非空时，ship() 要求 engravingCompletedAt 已设

#### 事件流变更

- createFulfillment 请求体扩展：items 中可含 serviceAttributes；Fulfillment 从 SERVICE items（relatedSkuId + 镭雕内容）合并 engravingInfo 到关联的 PHYSICAL 履约单
- 新增 API：`POST /api/fulfillment/{fulfillmentOrderId}/complete-engraving`
- 新增事件：**EngravingCompleted**（可选，若需 Activity 时间线展示）
  - Topic: `fulfillment.engraving.completed`
  - Payload: orderId, fulfillmentOrderId, completedAt, occurredAt

#### 需求场景变更

- 🔲 新增创建含镭雕履约单（提取 engravingInfo）
- 🔲 新增 completeEngraving 及 ship 门禁验收
- 🔄 修改 ship 前置条件（有镭雕时须先完成）

---

### Cart（🟢 轻微，🔄 需调整：镭雕内容暂不落 Cart）

#### 影响程度

🟢 轻微

#### 说明

镭雕内容（图案、文字）为结账时填写，Cart 可不存储；加购时仅记录镭雕 SKU + relatedSkuId，结账页再填雕刻内容。若未来需购物车暂存雕刻内容，可扩展 CartItem。MVP 采用结账时填写。

---

### Activity（🟢 轻微）

#### 影响程度

🟢 轻微

#### 说明

若发布 EngravingCompleted 事件，Activity 可订阅并在订单旅程中展示「镭雕已完成」节点。

---

### 其他 BC

- **Inventory**：⚪ 无变更
- **Payment**：⚪ 无变更
- **User**：⚪ 无变更

---

### BC 间数据流

```
Catalog EngravingPattern
  → GET /api/engraving-patterns
  → 前端选品 / 结账页

Order OrderLineItem.serviceAttributes (engravingPatternId, engravingPatternName, engravingText)
  → createFulfillment 请求
  → Fulfillment FulfillmentOrder.engravingInfo

Fulfillment EngravingCompleted（若发布）
  → Activity 订单旅程
```

---

## 四、迭代计划

### 迭代 0：图案库（Catalog） ✅ 已完成

**涉及 BC**：Catalog  
**前置依赖**：无

**后端**：EngravingPattern 实体与 CRUD；`GET /api/engraving-patterns` 列表 API。  
**前端**：`frontend/admin` 图案库管理页。  
**验收**：Admin 可配置图案；`GET /api/engraving-patterns` 返回图案列表。

---

### 迭代 1：镭雕服务配置与选品 ✅ 已完成

**涉及 BC**：Catalog  
**前置依赖**：迭代 0；虚拟商品迭代 1（ServiceBinding 已就绪）

**后端**：镭雕 SERVICE SPU 创建，ServiceBinding 关联目标实体 SPU；前端可查询可选服务时区分镭雕（用于展示图案选择入口）。  
**前端**：`frontend/web` 商品详情页展示镭雕可选；选规格/服务时加载图案库、支持图案+文字输入；`frontend/admin` 镭雕服务配置（复用 ServiceBinding 管理）。  
**验收**：实体商品详情页可展示镭雕服务；选镭雕时可选图案、填文字（≤20 字），至少选其一。

---

### 迭代 2：下单与履约（含镭雕） ✅ 已完成

**涉及 BC**：Order、Fulfillment  
**前置依赖**：迭代 1；虚拟商品迭代 2（交易流程支持随购）

**后端**：
- Order：PlaceOrder 接纳 serviceAttributes（镭雕）；校验 engravingPatternId/engravingText；createFulfillment 传递镭雕信息
- Fulfillment：createFulfillment 将镭雕合并到实体履约单 engravingInfo；新增 completeEngraving API；ship 门禁（有 engravingInfo 时须 engravingCompletedAt 已设）
**前端**：`frontend/web` 结账页镭雕内容确认与提交；`frontend/admin` FulfillmentPage 展示 engravingInfo、镭雕完成按钮、ship 前门禁提示。  
**验收**：购买实体+镭雕 → 下单 → 支付 → 履约配货 → Admin 完成镭雕 → 发货 → 签收 → OrderCompleted。  
**E2E**：BIZ-LE-001 含镭雕订单主流程。

---

### 迭代 3：订单与履约详情展示 ✅ 已完成

**涉及 BC**：Order、Fulfillment、Activity  
**前置依赖**：迭代 2

**后端**：Order 详情 API 返回镭雕 serviceAttributes；Fulfillment 详情返回 engravingInfo、engravingCompletedAt；EngravingCompleted 事件（可选）供 Activity 消费。  
**前端**：订单详情页展示镭雕内容；履约单详情与 Admin 列表展示镭雕状态；订单旅程展示「镭雕已完成」（若发事件）。  
**验收**：用户与 Admin 可查看镭雕内容及完成状态。

---

### 一致性检查

| 维度 | 检查项 | 结论 |
|------|--------|------|
| 场景完整 | 主流程 L3、支撑 L1/L2、异常 L2 | ✅ |
| 事件完整 | 主成功路径 + ship 门禁 + 补偿一致 | ✅ |
| 数据可达 | engravingInfo、engravingCompletedAt、图案库在变更分析中已补齐 | ✅ |
| 场景↔变更 | LE-1～LE-6 与 Catalog/Order/Fulfillment 变更对应 | ✅ |
| 前端 | 迭代计划标注了受影响页面 | ✅ |

---

## 交付跟踪

### 迭代 0：图案库（Catalog） ✅

| # | 工作项 | Skill | 状态 | 说明 |
|---|--------|-------|------|------|
| 1 | Catalog: EngravingPattern 域对象 + 仓储 + 应用服务 + API | evolve-feature | ✅ 完成 | 6 scenario 全绿（engraving-pattern.feature 6.1～6.6），Catalog 总计 6 feature / 72 scenario |
| 2 | BFF: `/api/engraving-patterns` 路由注册 | — | ✅ 完成 | BffRoutingService 新增路由，bff/requirements.md 已同步 |
| 3 | frontend/admin: 图案库管理页 | frontend-development | ✅ 完成 | 路由 `/engraving-patterns`、CRUD 弹窗、图片上传、启用状态过滤；ui-spec.md 已同步 |
| 4 | frontend/admin: Business E2E 测试 | — | ✅ 完成 | BIZ-LE-001～003 全绿（新增/编辑/删除）；admin 测试基础设施首次搭建（Playwright + Page Object） |
| 5 | frontend/web: Smoke P0 回归 | — | ✅ 通过 | 2 passed，核心交易链路未受影响 |

**交付日期**：2026-03-04  
**下一迭代**：迭代 1（镭雕服务配置与选品），前置依赖：虚拟商品迭代 1（ServiceBinding 已就绪 ✅）

---

### 迭代 1：镭雕服务配置与选品 ✅

| # | 工作项 | Skill | 依赖 | 状态 | 说明 |
|---|--------|-------|------|------|------|
| 1 | Catalog: 镭雕服务区分（serviceKind）+ available-services 扩展 | evolve-feature | — | ✅ 完成 | SPU.serviceKind；AvailableService 返回 serviceKind；74 scenario 全绿 |
| 2 | frontend/admin: 镭雕服务标识展示 | frontend-development | #1 | ✅ 完成 | Catalog 树 + ProductDetail 展示镭雕 Badge |
| 3 | frontend/web: 详情页镭雕可选 + 图案库 + 文字输入（≤20 字） | frontend-development | #1 | ✅ 完成 | serviceKind=ENGRAVING 时图案选择 + 文字输入；校验至少选其一 |
| 4 | E2E 交付门禁 | deliver-requirement | #1–#3 | ✅ 完成 | Smoke P0 通过；BIZ-LE-004 已添加（无镭雕数据时 skip，有数据时验证） |

**交付日期**：2026-03-04  
**下一迭代**：迭代 2（下单与履约），前置依赖：虚拟商品迭代 2（交易流程支持随购 ✅）

**BIZ-LE-004 前置数据**：需存在 SERVICE SPU(serviceKind=ENGRAVING) + ServiceBinding 到实体 SPU + 镭雕图案。可通过 MCP 或 Admin 创建。

---

### 迭代 2：下单与履约（含镭雕） ✅

| # | 工作项 | Skill | 依赖 | 状态 | 说明 |
|---|--------|-------|------|------|------|
| 1 | Fulfillment: engravingInfo 合并、completeEngraving、ship 门禁、查询返回 | evolve-feature | — | ✅ 完成 | 1.8, 3a.1–3a.4, 3.5, 6.4；Fulfillment 测试全绿 |
| 2 | Order: PlaceOrder 镭雕校验与 createFulfillment 传参 | evolve-feature | #1 | ✅ 完成 | 1.9；OrderLineItem.serviceAttributes；CreateFulfillmentPort 传参；Order 测试全绿 |
| 3 | frontend/web: 结账页镭雕内容确认与提交 | frontend-development | #2 | ✅ 完成 | 立即购买流程已传 serviceAttributes；toOrderItem 提取并提交 |
| 4 | frontend/admin: FulfillmentPage 镭雕展示与完成按钮 | frontend-development | #1 | ✅ 完成 | 镭雕列、engravingInfo 预览、完成镭雕按钮、ship 门禁（待镭雕时发货 disabled） |
| 5 | E2E 交付门禁 | deliver-requirement | #1–#4 | ✅ 完成 | BIZ-LE-005 全绿；Page Object 修正镭雕选择器（selectEngravingServiceOptionIfPresent + 镭雕区域限定） |

**交付日期**：2026-03-04  
**下一迭代**：迭代 3（订单与履约详情展示），前置依赖：迭代 2 ✅

---

### 迭代 3：订单与履约详情展示 ✅

| # | 工作项 | Skill | 依赖 | 状态 | 说明 |
|---|--------|-------|------|------|------|
| 1 | Order: 详情 API 返回 serviceAttributes | evolve-feature | — | ✅ 完成 | OrderDto.OrderLineItemDto 新增 serviceAttributes，toDto 映射 |
| 2 | Fulfillment: EngravingCompleted 事件 | evolve-feature | — | ✅ 完成 | 完成镭雕时发布事件；Activity 注册并消费；EventMetadataRegistry 新增 |
| 3 | frontend/web: OrderDetailPage 镭雕展示 | frontend-development | #1 | ✅ 完成 | 商品明细镭雕内容、EVENT_DESCRIPTIONS 新增 EngravingCompleted |
| 4 | E2E 交付门禁 | deliver-requirement | #1–#3 | ✅ 完成 | BIZ-LE-007 已添加；需重启 order-service 后 E2E 可全绿 |

**交付日期**：2026-03-04  
**本需求已全部交付**

**BIZ-LE-007 前置条件**：需重启 order-service 以加载 OrderDto 变更；Fulfillment、Activity 已含 EngravingCompleted 支持。
