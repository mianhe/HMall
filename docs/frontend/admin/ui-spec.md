# 界面规格 — 管理后台（frontend/admin）

管理后台支持 **Catalog 全量 CRUD**：类目、商品、规格维度、选项、SKU 的增删改均在 Admin 完成；商品详情页支持编辑基本信息、维度/选项/SKU、展示图与服务绑定。后端需求见 `docs/bounded-contexts/catalog/requirements.md`。

---

## 一、定位与范围

| 端 | 职责 |
|----|------|
| **管理后台（frontend/admin）** | 类目树增删改、商品增删改、规格维度/选项/SKU 增删改、产品级/选项级展示图、服务绑定、镭雕图案库；全部通过 REST API 完成 |
| **MCP** | 可选补充（与 Admin 能力重叠时可二选一） |

---

## 二、页面规格

### 2.1 页面总览

| 路由 | 页面 | 功能 | 后端 API |
|------|------|------|----------|
| `/` | HomePage | 入口；文案说明；跳转「查看 Catalog」「库存管理」 | — |
| `/catalog` | CatalogPage | 类目 + 商品树形全览；**类目**：hover 显示「+ 子类目」「编辑」「删除」；**叶子类目**另有「新增商品」；**商品行** hover 显示「删除」；刷新、空态与错误提示 | `GET/POST/PUT/DELETE /api/categories`、`GET /api/products`、`POST /api/products`、`DELETE /api/products/{id}` |
| `/products/:id` | ProductDetailPage | 商品详情：**基础信息**可编辑（名称、描述）与删除；**规格维度**可新增；**选项**可新增/删除；**SKU** 可新增/编辑价格与展示名/删除；产品级与选项级展示图、服务绑定（SERVICE） | `GET/PUT/DELETE /api/products/:id`、`GET/POST /api/products/:id/dimensions`、`POST/DELETE .../options`、`GET/POST/PUT/DELETE .../skus`、图片与绑定 API |
| `/inventory` | InventoryPage | 库存管理：平铺表格展示（一级类别、二级子类别、产品、SKU 名称、可用、已占用、操作）；过滤（一级类别、二级子类别、产品名称）；库存直接修改（PUT） | `GET /api/categories`、`GET /api/products`、`GET /api/products/{id}/skus`、`GET/PUT /api/inventory/stock/{skuId}` |
| `/fulfillment` | FulfillmentPage | 履约管理：列表展示履约单（履约单ID、订单ID、状态、商品摘要、收货人、地址、承运商、物流单号、发货/签收时间、创建时间、操作）；过滤（订单ID、状态）；按状态操作（开始配货、发货、签收） | Fulfillment API |
| `/engraving-patterns` | EngravingPatternPage | 镭雕图案库：列表展示图案（ID、缩略图、名称、排序、启用状态、操作）；过滤（启用状态）；新增/编辑/删除图案；图片上传或手动输入 URL | `GET/POST /api/engraving-patterns`、`GET/PUT/DELETE /api/engraving-patterns/{id}`、`POST /api/files/upload` |
| `/activity` | ActivityPage | 活动监控仪表盘：统计卡片 + 最近事件流水 | `GET /api/activities/stats`、`GET /api/activities/recent` |
| `/activity/journey/:orderId?` | OrderJourneyPage | 订单旅程回放：按 BC 分组的事件时间线，展示交易全生命周期与 Saga 补偿路径 | `GET /api/activities?orderId={id}` |
| `/settings` | SettingsPage | 系统设置：按分类展示配置项（当前含「支付设置」） | `GET/PUT /api/payments/settings` |

### 2.2 Catalog 页

- **递归加载**：根类目 → 每类目下子类目与商品 → 每商品下维度（含选项及展示图）、SKU。
- **树形展示**（`CatalogTree` / `CatalogTreeNode`）。**类目行** hover 显示操作：「+ 子类目」（弹窗：名称、描述）、「编辑」（弹窗：名称、描述）、「删除」（confirm）；**叶子类目**另有「新增商品」按钮。
- **新增商品**：叶子类目行「新增商品」→ 弹窗（类目已定，名称、描述、商品类型、服务分类）→ 创建后跳转商品详情。API: `POST /api/products`。
- **商品行**：hover 显示「删除」，confirm 后 `DELETE /api/products/{id}` 并刷新树。
- **刷新按钮**：重新请求并渲染整棵树。
- **错误与空态**：请求失败显示错误信息；无数据时提示「暂无数据，请通过 MCP 添加类目与商品」。
- **商品类型标签**：商品名称旁显示 `productType` Badge；SERVICE 且 `serviceKind=ENGRAVING` 时显示「镭雕」Badge。

| 层级 | 展示内容 |
|------|----------|
| 类目 | 名称、描述；hover 显示 + 子类目 / 编辑 / 删除（叶子另有「新增商品」） |
| 商品 | 名称、Badge、描述；点击进详情；hover 显示「删除」 |
| SKU | 规格组合、价格、展示名（详情页内管理） |
| 服务绑定 | 商品详情页内、仅 SERVICE 类型管理 |

API 封装位置：`frontend/admin/src/shared/api/catalog.js`。契约：`docs/bounded-contexts/catalog/api.yaml`。

### 2.3 商品详情页（ProductDetailPage）

- **进入方式**：Catalog 页点击商品名称，跳转 `/products/:id`。
- **商品基本信息**：名称、描述可点击「编辑」进入行内编辑，保存调用 `PUT /api/products/:id`；右上角「删除商品」confirm 后 `DELETE /api/products/:id` 并跳回 Catalog。
- **规格维度**：列表展示；「+ 新增维度」展开表单（维度名称、是否必填），`POST /api/products/{spuId}/dimensions`。
- **规格选项**：每个维度下列出选项，每选项可「删除」；维度下「+ 新增选项」输入选项值，`POST .../dimensions/{dimId}/options`、`DELETE .../options/{optId}`。
- **SKU**：列表展示每 SKU 的价格（元）与展示名，行内可编辑后「保存」或「删除」；「+ 新增 SKU」展开表单（每维度选一选项、价格、展示名），`POST/PUT/DELETE /api/products/{spuId}/skus`。
- **产品级/选项级展示图**：上传（先 `POST /api/files/upload` 再创建图）、删除。
- **服务绑定**（仅 SERVICE）：按 SKU 管理绑定，新增（目标 SPU ID、价格）、删除。
- 错误时展示后端返回的 `message`。

### 2.4 库存管理页（InventoryPage）

- **平铺表格**——列：一级类别、二级子类别、产品、SKU 名称、可用、已占用、操作。不展示描述性文案，仅展示 SKU 名称。
- **过滤**（假设类别仅两层）：按一级类别 → 二级子类别 → 产品名称关键词。
- **库存修改**：表格内直接修改（输入框 + 保存），调用 `PUT /api/inventory/stock/{skuId}`；成功更新当前行，失败展示后端 message。
- 数据来源：Catalog 树数据扁平化 + 库存 `GET /api/inventory/stock/{skuId}`。API 封装：`shared/api/inventory.js`。

### 2.5 活动监控页（ActivityPage）

| 功能 | 说明 | API |
|------|------|-----|
| 统计卡片 | 订单概览（3 指标）、支付概览（4 指标）、库存活动（2 指标） | `GET /api/activities/stats?period=today\|last7\|last30` |
| 时间范围切换 | 今日 / 最近7天 / 最近30天 快捷选择 + 自定义日期 | 同上 |
| 最近活动流水 | 最近事件列表（时间、事件类型、订单 ID、Topic） | `GET /api/activities/recent?limit=20` |
| 订单旅程入口 | 输入框 + 按钮，输入 orderId 跳转旅程页 | — |

### 2.6 订单旅程回放页（OrderJourneyPage）

| 功能 | 说明 |
|------|------|
| 顶部概要 | orderId、推断状态、事件数量、时间跨度 |
| 分组时间线 | 事件按 occurredAt 正序，每节点：BC 标签（彩色 badge）、中文事件名、时间戳 |
| BC 颜色编码 | Order=蓝、Payment=绿、Inventory=琥珀、Fulfillment=靛蓝 |
| 补偿事件高亮 | OrderCancelled、StockReleased 使用红/橙色 |
| 事件详情展开 | 点击展开 payload 关键字段 |

后端需求见 `docs/bounded-contexts/activity/requirements.md` §4。

### 2.7 系统设置页（SettingsPage）

- **支付设置**：支付超时时间（分钟），整数，最小 1，默认 30。
- 页面加载时读取当前值，编辑后点「保存」提交；成功展示提示，失败展示后端 message。

### 2.8 AI 智能对话

AI Chat 以全局 Drawer 形式存在，从右侧滑出，所有路由页面共享同一个对话实例。

| 功能 | 说明 | API |
|------|------|-----|
| 呼出/关闭 | 右下角浮动按钮，或 Ctrl+K / Cmd+K | — |
| 流式对话 | SSE 流式接收 LLM 回复，逐 token 渲染 | `POST /api/ai/chat` (SSE) |
| Tool Call 展示 | 折叠卡片展示工具名、参数、状态、结果 | — |
| Markdown 渲染 | marked + DOMPurify | — |
| 结构化回复 | 区分思考过程（可折叠）、正式结论、后续建议 | — |
| 模型切换 | 可选择不同 LLM 提供商 | `GET /api/ai/models` |

后端需求见 `docs/bounded-contexts/smart-interaction/requirements.md`。技术设计见 `docs/bounded-contexts/smart-interaction/architecture.md`。

---

## 三、组件结构

### 3.1 原子（Atoms）

| 组件 | 说明 |
|------|------|
| Spinner | 加载中 |
| Badge | 小徽标（价格、状态等） |

### 3.2 分子（Molecules）

| 组件 | 说明 |
|------|------|
| TreeNode | 树形行：可展开/折叠，缩进表示层级 |
| TreeNodeRow | 单行内容：类目名、商品名+维度、SKU 规格与价格 |

### 3.3 有机体（Organisms）

| 组件 | 说明 |
|------|------|
| AppHeader | 顶栏：Logo/标题 + 导航链接 |
| CatalogTree | 完整 Catalog 树：类目 → 商品（含 dimension）→ SKU，支持展开/折叠、刷新 |

### 3.4 页面结构

| 页面 | 结构 |
|------|------|
| 首页 | AppHeader + 简短说明 + 链接（去 Catalog、库存、监控、设置） |
| Catalog | AppHeader + CatalogTree（树形分层展示 + 刷新） |
| 商品详情 | AppHeader + 面包屑 + 基础信息区 + 维度与选项区（含展示图）+ 产品级展示图区 |
| 库存管理 | AppHeader + 过滤栏（类别级联 + 关键词）+ 平铺表格（名称、可用、已占用、操作） |
| 履约管理 | AppHeader + 过滤栏 + 履约单表格 + 操作按钮 |
| 镭雕图案库 | AppHeader + 过滤栏（启用状态）+ 图案表格（缩略图、名称、排序、状态、操作）+ 新增/编辑弹窗 |
| 活动监控 | AppHeader + 时间范围选择器（pill tabs + 日期）+ 统计卡片组 + 最近活动表格 + 订单旅程入口 |
| 订单旅程 | AppHeader + 面包屑 + 顶部概要卡片 + 分组垂直时间线（BC 彩色 badge、事件详情可展开） |
| 系统设置 | AppHeader + 分类卡片（支付设置：输入框 + 保存按钮） |

### 3.5 AI Chat 组件

```
src/shared/ui/ai-chat/
├── AiChatPanel.vue          # Drawer 主面板
├── AiChatButton.vue         # 右下角浮动按钮
├── AiMessageList.vue        # 消息列表
├── AiMessageBubble.vue      # 单条消息
├── AiToolCallCard.vue       # Tool Call 卡片
└── AiChatInput.vue          # 输入框 + 发送按钮

src/shared/composables/
└── useAiChat.js             # SSE 连接、消息管理、状态

src/shared/api/
└── ai.js                    # AI API 封装
```

---

## 四、视觉规范

参考 [华为商城 VMALL](https://www.vmall.com/)：简洁、干净、现代，以白色与浅灰为主，红色作强调。

| 元素 | 规范 |
|------|------|
| **主色** | 华为红 `#C7000B`，用于顶栏、按钮、链接、强调 |
| **主色 hover** | `#A00009` 深红 |
| **背景** | 主区域白色 `#FFFFFF`；页面底 `#F5F5F5` |
| **文字** | 标题深灰 `#333333`；正文 `#666666`；辅助 `#999999` |
| **边框** | `#E0E0E0` |
| **布局** | 居中、max-width 限制；留白充足；卡片圆角 `rounded-lg` |
| **顶栏** | 红底白字；Logo/标题左；导航右 |
| **按钮** | 主按钮红底白字；次要为白底灰框 |

Tailwind 已配置 `vmall-red`、`vmall-red-hover`、`vmall-gray-bg` 等，实现时优先使用。

---

## 五、技术栈

- **框架**：Vue 3 + Vite + Vue Router
- **样式**：Tailwind CSS（含 `@tailwindcss/typography`）
- **请求**：axios，开发时代理到后端
  - `/api/ai` → smart-interaction-service `http://127.0.0.1:8089`
  - `/api` → BFF `http://127.0.0.1:8085`
- **与后端对齐**：请求/响应结构与各 BC 的 `api.yaml` 一致
- **端口**：开发环境 `5173`

---

## 六、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/catalog/requirements.md` | Catalog 后端需求与 .feature |
| `docs/bounded-contexts/catalog/api.yaml` | Catalog REST 契约 |
| `docs/bounded-contexts/inventory/api.yaml` | Inventory REST 契约 |
| `docs/bounded-contexts/activity/api.yaml` | Activity BC API 契约 |
| `docs/bounded-contexts/smart-interaction/requirements.md` | Smart Interaction 需求（AI Chat 后端） |
| `docs/bounded-contexts/smart-interaction/architecture.md` | AI Chat 技术设计（LLM + MCP） |
| `docs/frontend/web/ui-spec.md` | 消费者端界面规格 |
