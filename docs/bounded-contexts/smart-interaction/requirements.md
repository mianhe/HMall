# Smart Interaction 限界上下文 - 需求列表

每个功能对应一个 .feature 文件，场景对应 Gherkin Scenario。Feature 目录：`services/smart-interaction-service/src/test/resources/features/smart-interaction/`。技术架构见 [architecture.md](./architecture.md)，领域模型见 [domain-model.md](./domain-model.md)。

### 状态图例

- ✅ 已实现
- 🔲 待实现

---

## 一、职责说明

Smart Interaction 提供智能交互能力：管理员和消费者通过自然语言与系统对话，借助 Skill 定义的上下文和工具范围，由 LLM + MCP Tool Calling 编排执行业务操作。

- **管理后台（frontend-admin）**：运营人员管理商品、库存、订单、履约，已接入。
- **消费者前台（frontend-web）**：用户对话式购物、查询订单、追踪物流、管理账户，规划中。

**核心设计决策**：

| # | 决策 | 说明 |
|---|------|------|
| S1 | Smart Interaction 是独立 BC | 拥有 Skill、Conversation、Settings 领域模型，独立持久化 |
| S2 | Skill 存储在 Client 端（Smart Interaction），不在 MCP Server | 灵活可运营，管理员可随时 CRUD，无需改代码或重启 MCP Server |
| S3 | Tool 过滤在 Client 端执行 | MCP Server 暴露全量 tools，Smart Interaction 按 Skill.allowedTools 过滤后传给 LLM |
| S4 | 单一 MCP Server，按模块组织 tools | 物理简单，逻辑隔离由 Skill 的 allowedTools 完成 |
| S5 | 通配符匹配 allowedTools | `inventory_*` 匹配所有 `inventory_` 前缀的工具；`*` 或空列表表示不过滤 |
| S6 | Settings 单例 | 系统级配置全局一条记录，暂不按用户区分 |
| S7 | 对话历史持久化 | Conversation + Message 存数据库，支持历史回顾 |
| S8 | Skill 自动匹配 | 用户未指定 Skill 时，系统用 LLM 路由匹配 0-N 个相关 Skill，合并 systemPrompt 注入领域知识。工具范围：管理端不传 clientType 时全量可用（向后兼容）；指定 clientType 时按匹配到的 Skill 的 allowedTools 并集过滤，0 匹配则无工具（安全降级）。手动指定 Skill 时仍按其 allowedTools 过滤 |
| S9 | 前后台共用 Smart Interaction Service | 同一个服务实例，通过 Skill 隔离能力范围；前台经 BFF 代理或直连 |
| S13 | Skill Audience 隔离 | Skill 有 `audience` 属性（`admin`/`consumer`/`all`），自动匹配时按请求中的 `clientType` 过滤候选 Skill，防止后台 Skill 出现在消费者端路由中。不传 clientType 时不过滤（向后兼容） |
| S10 | userId 由服务端注入 | Smart Interaction 从认证上下文（JWT）提取 userId，自动注入 MCP tool 调用参数，前端不传 userId（防篡改） |
| S11 | 消费者端默认自动匹配 | 前台用户不手动选 Skill，完全依赖自动匹配（更自然的消费者体验） |
| S12 | MCP Tool 不区分前后台 | 工具本身是通用的，权限隔离在 userId 注入层完成 |

---

## 1. Skill 管理

`skill-management.feature`

- ✅ 1.1 创建 Skill 时传入名称、描述、systemPrompt、allowedTools，应创建成功并返回 Skill 详情
- ✅ 1.2 创建 Skill 时名称为空应返回 400 错误
- ✅ 1.3 查询 Skill 列表应返回所有 Skill（按 createdAt 降序）
- ✅ 1.4 按 ID 查询 Skill 详情应返回完整信息（含 allowedTools）
- ✅ 1.5 更新 Skill 时应修改对应字段并更新 updatedAt
- ✅ 1.6 删除 Skill 应成功；删除不存在的 Skill 应返回 404
- ✅ 1.7 将某 Skill 设为默认时，应自动取消原默认 Skill 的 isDefault 标记
- ✅ 1.8 删除默认 Skill 后，系统应无默认 Skill（不自动指定替代）
- ✅ 1.9 创建 Skill 时可指定 audience（admin/consumer/all），不指定时默认为 all（决策 S13）

---

## 2. AI 流式对话

`ai-chat.feature`

- ✅ 2.1 纯文本对话应通过 SSE 流式返回（delta 事件逐 token 推送，done 事件结束）
- ✅ 2.2 单轮 Tool Call 应调用 MCP 工具并返回结果（LLM 返回 tool_calls → 执行 MCP → 回填结果 → LLM 生成最终回复）
- ✅ 2.3 多轮 Tool Call 应逐轮执行并最终返回结果（LLM 多次返回 tool_calls 时逐轮执行，直到产出纯文本）
- ✅ 2.4 查询可用模型列表时应返回已配置的模型（GET /api/ai/models）
- ✅ 2.5 指定 skillId 时，对话应加载该 Skill 的 systemPrompt 并按 allowedTools 过滤工具；system prompt 始终包含 base prompt + Skill 领域知识（不再替换）
- ✅ 2.6 未指定 skillId 时，应使用默认 Skill；无默认 Skill 时使用基础 system prompt + 全量工具
- ✅ 2.7 Tool Call 轮次超过限制时应终止循环并返回错误提示
- 🔲 2.8 LLM 返回空 id/name 的无效 tool call 时应过滤并正常完成对话，不死循环
- 🔲 2.9 MCP Server 不可用时对话应返回错误提示而非挂起
- 🔲 2.10 LLM API Key 缺失或无效时应返回明确错误提示

---

## 3. 对话历史

`conversation-history.feature`

- 🔲 3.1 对话完成后应自动持久化 Conversation 及其 Messages
- 🔲 3.2 查询对话列表应返回所有 Conversation（按 updatedAt 降序），包含 title、skillId、provider、时间
- 🔲 3.3 按 ID 查询对话详情应返回 Conversation 及其所有 Messages（含 toolCalls）
- 🔲 3.4 删除对话应级联删除其所有 Messages
- 🔲 3.5 删除不存在的对话应返回 404

---

## 4. 系统设置

`settings.feature`

- ✅ 4.1 获取系统设置应返回当前配置（adminBasePrompt、consumerBasePrompt）
- ✅ 4.2 首次获取时若无记录应自动创建并返回默认值（prompt 字段为 null，运行时使用代码内默认提示词）
- ✅ 4.3 更新系统设置应修改对应字段并更新 updatedAt
- ✅ 4.4 更新 adminBasePrompt / consumerBasePrompt 为空字符串时视为清空（恢复使用默认提示词）
- ✅ 4.5 AiChatService 根据 clientType 选择对应 base prompt：有自定义则用自定义，否则用代码内默认值

---

## 5. MCP 工具扩展

`mcp-tools.feature`

- ✅ 5.1 Inventory 模块工具应可通过 MCP 调用（查询库存、设置库存）
- 🔄 5.2 Order 模块工具应可通过 MCP 调用（✅ 查询订单详情、✅ 按用户查询订单列表、✅ 创建订单、🔲 取消订单）
- 🔲 5.3 Fulfillment 模块工具应可通过 MCP 调用（按 orderId 查询履约单状态；管理端额外支持：触发配货/发货/签收）
- 🔲 5.4 Activity 模块工具应可通过 MCP 调用（查询统计、查询事件列表）
- ✅ 5.5 Cart 模块工具应可通过 MCP 调用（购物车查询、添加、修改数量、删除、结算预览）
- 🔲 5.6 Address 模块工具应可通过 MCP 调用（收货地址查询、新增、修改、删除）

---

## 7. Skill 自动匹配

`skill-auto-matching.feature`

用户未手动指定 Skill 时，系统根据用户首条消息自动匹配相关 Skill，合并 systemPrompt 注入领域知识，但不限制工具范围。设计方案详见 [architecture.md § 3.5](./architecture.md)。

- ✅ 7.1 用户未指定 skillId 发送消息时，系统应根据消息内容和所有 Skill 的 name + description 自动匹配 0-N 个相关 Skill
- ✅ 7.2 匹配到 1 个或多个 Skill 时，应合并它们的 systemPrompt 注入对话，但工具不过滤（全量工具可用）
- ✅ 7.3 无 Skill 匹配时，应使用基础 system prompt + 全量工具（与无 Skill 模式一致）
- ✅ 7.4 用户手动指定 skillId 时，应跳过自动匹配，使用指定 Skill 的 systemPrompt 并按其 allowedTools 过滤工具
- ✅ 7.5 自动匹配结果应通过 SSE 事件通知前端（skill_matched 事件，含匹配到的 Skill 列表）
- 🔲 7.6 用户发送 `skillMode: "none"` 时，应跳过自动匹配和默认 Skill，使用基础 system prompt + 全量工具
- ✅ 7.7 请求携带 `clientType` 时，自动匹配仅在 audience 匹配的 Skill 中路由（决策 S13）
- ✅ 7.8 请求携带 `clientType` 时，工具范围按匹配到的 Skill 的 allowedTools 并集过滤；0 个匹配则不提供任何工具（安全降级，防止越权）
- ✅ 7.9 tools 为空时，system prompt 应注入无工具拒答指令（明确告知 LLM 无工具可用，禁止编造数据）；有工具时不注入

---

## 8. 前端交互（frontend-admin，手工验收）

以下为前端 UI 需求，通过手工验收：

### 已实现

- ✅ 8.1 全局对话窗（Drawer）可通过右下角浮动按钮或 Ctrl+K / Cmd+K 快捷键呼出
- ✅ 8.2 SSE 流式接收，逐 token 更新 assistant 消息内容
- ✅ 8.3 Tool Call 可视化：展示工具名、参数、执行状态和结果（可折叠卡片）
- ✅ 8.4 Markdown 渲染：支持表格、列表等格式
- ✅ 8.5 回复结构化展示：区分思考过程（可折叠）、正式结论、后续建议
- ✅ 8.6 流式中断：清空对话时中断正在进行的 SSE 流
- ✅ 8.7 模型切换：可选择不同 LLM 提供商

### 待实现

- ✅ 8.8 Skill 选择器：对话顶部可选择/切换 Skill，下拉显示所有 Skill 及默认标记
- ✅ 8.9 Skill 管理面板：在 Drawer 内进行 Skill 的创建、编辑、删除、设为默认
- 🔲 8.10 对话历史列表：在 Drawer 内查看历史对话，点击可恢复
- ✅ 8.11 系统设置面板：在 Drawer 内配置 admin/consumer base prompt
- 🔲 8.12 Skill 自动匹配提示：自动匹配 Skill 后，在消息区显示匹配到的 Skill 名称
- 🔲 8.13 Skill 选择器三态：选择器提供「自动匹配」（默认）、「无 Skill」、指定具体 Skill 三种模式；自动匹配时不发 skillId，无 Skill 时发 `skillMode: "none"`，指定时发 `skillId`

---

## 二、消费者端扩展

Smart Interaction 从管理后台延伸到消费者前台（frontend-web），提供对话式购物、订单查询、物流追踪、账户管理能力。

### 迭代规划

| 迭代 | 主题 | 用户可见成果 | 涉及章节 |
|------|------|------------|---------|
| **I-1** | 基础打通 + 商品发现 | 前台出现 AI 聊天入口，能搜商品、浏览类目、查 SKU 价格和规格 | §11、§12（购物助手 v1） |
| **I-2** | 订单查询 + 物流追踪 | "我的订单到哪了"——查订单状态、看物流进展 | §9、§5.2、§5.3、§12（订单助手 v1） |
| **I-3** | 购物车 + 下单闭环 | "帮我下单"——搜索 → 加购 → 下单的完整对话式购物流程 | §5.5、§5.2（create）、§12（购物助手 v2） |
| **I-4** | 售后 + 地址 + 账户 | "取消订单""加个地址"——账户管理闭环 | §5.2（cancel）、§5.6、§12（订单助手 v2 + 账户助手） |

### 迭代依赖

```
I-1（商品发现）──→ I-2（订单+物流）──→ I-3（购物车+下单）──→ I-4（售后+账户）
 │                  │                    │
 └ 前端接入链路      └ userId 注入（后续基础） └ 写操作（加购/下单）
```

I-1 → I-2 强依赖（userId 注入是 I-2 的前提，也是 I-3、I-4 的基础）；I-3 与 I-4 之间相对独立，可调整优先级。

---

## 9. 用户上下文注入（I-2）

`user-context.feature`

消费者端所有操作必须限定在当前登录用户范围内。Smart Interaction 从认证信息中提取 userId，自动注入到 MCP tool 调用参数中（决策 S10）。

- ✅ 9.1 消费者端对话请求应携带认证信息（JWT），Smart Interaction 从中提取 userId
- ✅ 9.2 调用需要用户隔离的 MCP tool 时（如 order_query、cart_manage），应自动将 userId 注入 tool 调用参数
- ✅ 9.3 未认证用户发起对话应返回 401 错误
- ✅ 9.4 管理后台对话不注入 userId（保持现有行为，管理员可查询任意用户数据）

---

## 10. 前端交互（frontend-web，手工验收）

以下为消费者端前端 UI 需求，通过手工验收。

### I-1：基础接入

- 🔲 10.1 AI Chat 入口：页面右下角浮动按钮，点击打开对话面板
- 🔲 10.2 对话面板：SSE 流式接收，逐 token 更新；Markdown 渲染；Tool Call 执行状态（可精简展示）
- 🔲 10.3 自动匹配模式：消费者端默认使用自动匹配，不显示 Skill 选择器（决策 S11）
- 🔲 10.4 认证联动：未登录时提示登录；已登录时对话请求自动携带 JWT

### 后续迭代

- 🔲 10.5 商品卡片：AI 返回商品信息时以卡片形式展示（图片、名称、价格），可点击跳转商品详情页
- 🔲 10.6 订单卡片：AI 返回订单信息时以卡片形式展示（订单号、状态、金额），可点击跳转订单详情页
- 🔲 10.7 确认交互：下单、取消订单等写操作前，AI 给出确认提示，用户明确同意后执行
- 🔲 10.8 快捷提问：对话面板提供常用问题入口（"查看我的订单"、"推荐商品"等）

---

## 11. 消费者端 Skill

定义三个面向消费者的 Skill，由管理员在 Skill 管理界面创建。消费者端依赖自动匹配路由到正确的 Skill（决策 S11）。

### 11.1 购物助手

| 字段 | 值 |
|------|-----|
| **名称** | 购物助手 |
| **描述** | 帮助用户搜索商品、浏览类目、查看规格和价格，管理购物车，完成下单。 |
| **allowedTools** | `catalog_categories`, `catalog_products`, `catalog_skus`, `catalog_dimensions`, `cart_manage`, `order_create` |

**迭代演进**：

- v1（I-1）：仅含 `catalog_categories`、`catalog_products`、`catalog_skus`、`catalog_dimensions`（只读浏览）✅
- v2（I-3）：加入 `cart_manage`、`order_create`（购物闭环）✅

**System Prompt 要点**：

- 角色：HMall 购物助手，帮助用户发现和购买商品
- 价格展示转换为元（分 → 元）
- 商品推荐时列出关键规格和价格，方便用户对比
- 加购前确认 SKU 和数量；下单前确认商品清单和收货地址
- 不执行管理操作（不创建/修改/删除商品和类目）

### 11.2 订单助手

| 字段 | 值 |
|------|-----|
| **名称** | 订单助手 |
| **描述** | 帮助用户查询订单状态、追踪物流、取消订单。 |
| **allowedTools** | `order_query`, `order_cancel`, `fulfillment_query` |

**迭代演进**：

- v1（I-2）：`order_query` + `fulfillment_query`（查询和追踪）
- v2（I-4）：加入 `order_cancel`（售后操作）

**System Prompt 要点**：

- 角色：HMall 订单助手，帮助用户查询和管理订单
- 订单状态用自然语言翻译（PENDING_PAYMENT → 待付款，PAID → 已付款待配货，FULFILLING → 配货中，SHIPPED → 已发货，DELIVERED → 已签收，CANCELLED → 已取消）
- 物流状态翻译（CREATED → 订单已创建、ALLOCATING → 正在配货、SHIPPED → 已发货运输中、DELIVERED → 已签收）
- 取消订单前必须告知用户当前状态和取消后果（已发货不可取消，需走退货流程）
- 用户只能查看和操作自己的订单

### 11.3 账户助手

| 字段 | 值 |
|------|-----|
| **名称** | 账户助手 |
| **描述** | 帮助用户管理收货地址、查看消费记录。 |
| **allowedTools** | `address_manage`, `order_query` |

**迭代时点**：I-4

**System Prompt 要点**：

- 角色：HMall 账户助手，帮助用户管理个人信息和地址
- 新增地址时引导用户提供完整信息（收件人、电话、省市区、详细地址）
- 删除地址前确认
- 消费记录查询时汇总展示（总金额、订单数），按时间倒序

---

## 功能与 feature 对应

| 功能 | .feature 文件 | 场景数 | 状态 |
|------|----------------|--------|------|
| 1. Skill 管理 | skill-management.feature | 1.1～1.9 | ✅ 已实现 |
| 2. AI 流式对话 | ai-chat.feature | 2.1～2.7 ✅；2.8～2.10 🔲 | 部分完成 |
| 3. 对话历史 | conversation-history.feature | 3.1～3.5 | 🔲 待实现 |
| 4. 系统设置 | settings.feature | 4.1～4.5 | ✅ 已实现 |
| 5. MCP 工具扩展 | mcp-tools.feature | 5.1 ✅；5.2 🔄；5.5 ✅；5.3/5.4/5.6 🔲 | 部分完成 |
| 7. Skill 自动匹配 | skill-auto-matching.feature | 7.1～7.5 ✅；7.6 🔲；7.7～7.9 ✅ | 部分完成 |
| 8. 前端交互（admin） | （手工验收） | 8.1～8.9 ✅；8.10～8.13 🔲 | 部分完成 |
| 9. 用户上下文注入 | user-context.feature | 9.1～9.4 | ✅ 已实现 |
| 10. 前端交互（web） | （手工验收） | 10.1～10.8 | 🔲 I-1 起 |
| 11. 消费者端 Skill | （运营配置） | 11.1～11.3 | 🔲 I-1 起 |
