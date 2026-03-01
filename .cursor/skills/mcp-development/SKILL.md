---
name: mcp-development
description: 为 HMall BC 设计并实现 MCP tools 与 resources：API 分类与暴露范围分析、设计原则（收敛、命名、文档）、领域知识 Resource 编写、实现位置与 schema、验证与文档同步。触发词：MCP 开发、为 BC 提供 MCP、MCP tools、新增 MCP 工具、MCP resource。
---

# MCP 开发（为 BC 提供 MCP Tools & Resources）

## 知识分层架构

MCP 开发涉及两类产出物，承载不同层次的知识：

| 层次 | 载体 | 职责 | 写在哪里 |
|------|------|------|----------|
| **Tool Schema** | `server.tool()` 的 name + description + inputSchema | 告诉 LLM"这个工具是什么、何时用、参数怎么传" | `hmall-mcp/tools/<bc>.js` |
| **Resource** | `server.resource()` 暴露的领域知识文档 | 告诉 LLM"这个域的业务模型、规则和跨工具操作指南" | `hmall-mcp/resources/<bc>-domain.js` |

> **原则：Tool description 只描述工具本身，不嵌入领域知识。领域模型、业务规则、跨工具操作流程等知识放到 Resource 中，由 MCP Client 按需注入对话上下文。**

Skill Prompt（Smart Interaction 的 `systemPrompt`）只保留 few-shot 示例和操作策略，不重复 Resource 已有的领域知识。

---

## 一、Tool 范围分析（实现前必做）

在写代码之前，先分析该 BC 的 API 表面，决定哪些暴露、哪些不暴露。

### 步骤

1. **列出该 BC 的全部 API 契约**（读 `docs/bounded-contexts/<bc>/api.yaml`）。
2. **逐个分类**：

| 分类 | 判断标准 | MCP 暴露？ | 示例 |
|------|----------|-----------|------|
| **用户操作入口** | 前端管理后台或消费者端直接调用；是用户/AI 的自然操作意图 | **是** | 查询库存、设置库存、查询订单列表 |
| **系统协调 API** | 仅由其它 BC 在 Saga/流程中同步调用；直接调用会绕过协调方的一致性保证 | **否** | Saga 步骤中的占用/释放类 API |
| **内部/技术 API** | 健康检查、metrics、内部回调 | **否** | `/actuator/health` |

3. **输出「暴露清单」**：只包含「用户操作入口」类 API。

### 为什么不暴露系统协调 API？

- **一致性风险**：协调 API（如占用库存）是 Saga 步骤的一部分，只有从协调方（如 Order）发起才能保证跨 BC 数据一致。AI 直接调用会产生「库存被占但无对应订单」等脏数据。
- **不是自然操作意图**：用户的意图是「下单」而非「占用库存」；库存变动是下单的副作用，不是独立操作。
- **前端验证**：如果前端没有对应操作入口，说明这不是面向用户的能力。

---

## 二、Tool 设计原则

### 1. Tool 收敛：一资源一 Tool，用 action 区分操作

- **不要**为同一资源的 list/get/create/update/delete 各建一个 tool（易膨胀到 30+）。
- **要**按资源收敛：一个资源一个 tool，用参数 `action` 区分 list | get | create | update | delete。
- 读操作合并：list 与 search 合并为一个 list（可选 keyword/categoryId）；get 与 get_full 合并为一个 get（可选 detail: basic | full）。
- 参考：`hmall-mcp/docs/TOOLS.md` 中的已有 tool 设计。

### 2. 命名约定

- 前缀：`<bc>_`，如 `catalog_`、`inventory_`。同一 MCP Server 内前缀区分 BC。
- 资源名：小写，如 `catalog_categories`、`inventory_stock`。
- 不按动作拆 tool 名：用 `catalog_products` + `action=list|get|...`，而非 `catalog_list_products`。

### 3. 只暴露已有能力，不发明新用例

- MCP 层只**调用该 BC 已有 API 契约**，不在此层实现新业务规则。
- 参数与后端 API 对齐：必填/可选、类型、语义一致。

### 4. description 的边界

- **写什么**：工具用途（一句话）、action 可选值、各 action 所需参数、关键约束（如"priceCents 单位为分"）。
- **不写什么**：领域模型解释、业务流程、跨工具操作指南 — 这些属于 Resource 的职责。

### 5. 面向 AI 的返回与错误

- 返回内容为**文本**，便于 LLM 解析与引用。
- 错误：捕获 API 异常，返回简短可读的错误说明，不抛未处理异常。特别处理 `ECONNREFUSED`，返回"后端服务未启动"等友好提示。

### 6. 入口 BC 的边界

- **入口 BC**：对外（用户、Agent）提供查询与操作入口的 BC。
- 协调逻辑留在**后端或 BFF**，MCP 只调本 BC 接口；不在 MCP 层拼装多 BC 调用顺序。

---

## 三、Resource 设计原则

### 1. 定位与职责

Resource 是 MCP 协议提供的只读数据暴露机制。在 HMall 中，用它承载**领域知识文档**：

- 数据模型（实体、关系、字段语义）
- 业务规则（如"SERVICE 商品不需要库存"）
- 跨工具操作指南（如"修改绑定价格的步骤"）
- 常见操作流程（如"新建实体商品流程"）

### 2. 粒度：一域一 Resource

- 按业务域拆分，每个 Resource 覆盖一个或若干紧密相关的 BC。
- URI 命名：`hmall://<domain>/domain-knowledge`，如 `hmall://catalog/domain-knowledge`。
- 如果两个 BC 的 Tools 总是一起使用（如 Cart 和 Order），可合并为一个 Resource（`hmall://cart-order/domain-knowledge`）。

### 3. 内容编写原则

- **Markdown 格式**，结构化、分小节，便于 LLM 快速定位。
- **只写事实和规则**，不写操作步骤的"请"/"建议"等语气词 — Resource 是给程序/LLM 读的参考文档。
- **包含⚠️ 警告**标注容易犯错的点（如"不要为 SERVICE SKU 设置库存"）。
- **保持精炼**，每个 Resource 控制在 1000 字以内。过长会浪费 Token 预算。

### 4. 与 Tool description 的协作

- Tool description 中**可以引用**已有 Resource 中的术语（如"SKU"、"ServiceBinding"），但**不要解释**这些术语的含义。
- Resource 的跨工具操作指南应明确写出用到的 Tool 名和 action，形成完整的操作路径。

---

## 四、开发与实现

### 1. 代码位置

```
hmall-mcp/
├── index.js              # stdio 入口
├── index-http.js         # HTTP 入口
├── tools/
│   ├── common.js         # 共享工具（api 调用等）
│   └── <bc>.js           # 各 BC 的 Tools 注册
└── resources/
    └── <bc>-domain.js    # 各域的 Resource 注册
```

- 入口文件中 import 并调用 `registerXxxTools(server)` 和 `registerXxxResources(server)`。
- **两个入口文件（`index.js` 和 `index-http.js`）必须同步注册**，保持一致。

### 2. Tool 定义方式

- 使用 MCP SDK 的 `server.tool(name, description, inputSchema, handler)`。
- **name**：符合命名约定。
- **description**：中文简短说明，含「何时用」与「action 可选值」，便于 AI 选 tool。不嵌入领域知识。
- **inputSchema**：用 **zod** 定义；`action` 为枚举，其余参数按 action 条件必填（在 description 中说明）。
- **handler**：async，调 `api(method, path, body)` 访问后端；返回 `{ content: [{ type: 'text', text }] }`；错误 try/catch 转可读文本。

### 3. Resource 定义方式

Resource 文件结构（以 `hmall-mcp/resources/inventory-domain.js` 为例）：

```javascript
export const INVENTORY_DOMAIN_URI = 'hmall://inventory/domain-knowledge'

export const INVENTORY_DOMAIN_KNOWLEDGE = `## 库存（Inventory）领域知识
...领域模型、规则、操作指南...`

export function registerInventoryResources(server) {
  server.resource(
    'inventory-domain-knowledge',        // 资源标识名
    INVENTORY_DOMAIN_URI,                // URI
    { description: '...简短描述...', mimeType: 'text/plain' },
    async () => ({
      contents: [{ uri: INVENTORY_DOMAIN_URI, mimeType: 'text/plain', text: INVENTORY_DOMAIN_KNOWLEDGE }],
    }),
  )
}
```

- **第一参数**（资源标识名）：`<domain>-domain-knowledge`。
- **URI**：`hmall://<domain>/domain-knowledge`。
- **description**：一句话说明包含哪些知识主题，用于 Client 端匹配与过滤。
- **内容**：导出为常量 `const XXX_DOMAIN_KNOWLEDGE`，便于测试和复用。

### 4. 与 API 契约的对应

- 先确认该 BC 的 API 契约（`docs/bounded-contexts/<bc>/api.yaml`）。
- action 映射到 HTTP method + path；list/get → GET，create → POST，update → PUT，delete → DELETE。
- 环境变量：`HMALL_API_BASE`（默认 `http://localhost:8080/api`）。

---

## 五、Client 端集成（Smart Interaction）

MCP Resource 由 Smart Interaction 的 `AiChatService` 自动注入对话上下文：

1. `McpToolBridge` 启动时调用 `resources/list` 发现所有 Resource，缓存 URI 列表。
2. 对话构建时，根据匹配到的 Skill 的 `allowedTools` 前缀（如 `catalog_*` → `catalog`）推导关联域。
3. 用推导出的域匹配 Resource URI（`hmall://catalog/...`），调用 `resources/read` 获取内容。
4. 注入到系统 Prompt 的「领域知识」段，位于 Tool Schema 之后、Skill Prompt 之前。

**开发者不需要手动维护 Client 端的注入逻辑**，只需在 MCP Server 中正确注册 Resource，命名符合 `hmall://<domain>/domain-knowledge` 约定，Client 端会自动发现和注入。

---

## 六、验证方法

### 1. 本地运行与手工验证

- 启动后端 + MCP（`cd hmall-mcp && npm run start:http`）。
- 在 Cursor 中列出 tools，确认名称与描述；对关键 action 调用 1～2 次，检查返回与错误提示。
- 在 Cursor 中列出 resources，确认 Resource URI 和描述正确；读取内容检查格式与完整性。

### 2. 检查清单

**Tools：**

- [ ] **范围分析**：系统协调 API 未被暴露为 tool
- [ ] **Tool 收敛**：一资源一 tool，用 action 区分
- [ ] **命名**：前缀 + 资源名，无动作拆分
- [ ] **参数**：与后端 API 一致；description 说明各 action 所需参数
- [ ] **description 边界**：不含领域模型解释或跨工具操作流程
- [ ] **错误**：API 4xx/5xx 和 ECONNREFUSED 被捕获并返回可读文本

**Resources：**

- [ ] **覆盖完整**：该域的数据模型、业务规则、跨工具操作指南均已包含
- [ ] **URI 规范**：`hmall://<domain>/domain-knowledge`
- [ ] **内容精炼**：控制在 1000 字以内，Markdown 分节清晰
- [ ] **入口注册**：`index.js` 和 `index-http.js` 都已注册

**文档：**

- [ ] `hmall-mcp/docs/TOOLS.md` 已更新
- [ ] `hmall-mcp/README.md` 已更新（如有新 BC）

### 3. 回归与扩展

- 新增/修改 BC 的 API 时，同步改 `hmall-mcp/tools/<bc>.js`、`hmall-mcp/resources/<bc>-domain.js` 与 TOOLS 文档。
- 新增 BC 时：范围分析 → 新增 `tools/<bc>.js` + `resources/<bc>-domain.js` → 入口注册 → 文档 → 检查清单。
- 领域模型变更时，更新对应 Resource 内容，不需要改 Client 端代码。

---

## 七、参考

- **已有 Tool 实现**：`hmall-mcp/tools/` 下各 BC 文件。
- **已有 Resource 实现**：`hmall-mcp/resources/` 下各域文件。
- **Tool 文档**：`hmall-mcp/docs/TOOLS.md`。
- **运行与配置**：`hmall-mcp/README.md`。
- **知识分层架构**：`docs/bounded-contexts/smart-interaction/architecture.md`。
- **Skill Prompt 设计**：`docs/bounded-contexts/smart-interaction/skills-reference.md`。
