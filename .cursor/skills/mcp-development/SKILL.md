---
name: mcp-development
description: 为 HMall BC 设计并实现 MCP tools：API 分类与暴露范围分析、设计原则（收敛、命名、文档）、实现位置与 schema、验证与文档同步。触发词：MCP 开发、为 BC 提供 MCP、MCP tools、新增 MCP 工具。
---

# MCP 开发（为 BC 提供 MCP Tools）

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

## 二、设计原则

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

### 4. 面向 AI 的返回与错误

- 返回内容为**文本**，便于 LLM 解析与引用。
- 错误：捕获 API 异常，返回简短可读的错误说明，不抛未处理异常。

### 5. 文档与实现同步

- 每个 BC 的 tools 说明在 `hmall-mcp/docs/TOOLS.md`（或按 BC 拆为 `TOOLS-<bc>.md`）。
- README 中保留「文档」小节指向 TOOLS 文档。

### 6. 入口 BC 的边界

- **入口 BC**：对外（用户、Agent）提供查询与操作入口的 BC。
- 协调逻辑留在**后端或 BFF**，MCP 只调本 BC 接口；不在 MCP 层拼装多 BC 调用顺序。

---

## 三、开发与实现

### 1. 代码位置

- 所有 MCP 代码在 **hmall-mcp/** 下。
- 入口：`index.js`（stdio）、`index-http.js`（HTTP）。
- 每个 BC 的 tools：`hmall-mcp/tools/<bc>.js`，在入口中 `registerXxxTools(server)` 注册。
- 共享：API 调用等抽在 `hmall-mcp/tools/common.js` 或内联。

### 2. Tool 定义方式

- 使用 MCP SDK 的 `server.tool(name, description, inputSchema, handler)`。
- **name**：符合命名约定。
- **description**：中文简短说明，含「何时用」与「action 可选值」，便于 AI 选 tool。
- **inputSchema**：用 **zod** 定义；`action` 为枚举，其余参数按 action 条件必填（在 description 中说明）。
- **handler**：async，调 `api(method, path, body)` 访问后端；返回 `{ content: [{ type: 'text', text }] }`；错误 try/catch 转可读文本。

### 3. 与 API 契约的对应

- 先确认该 BC 的 API 契约（`docs/bounded-contexts/<bc>/api.yaml`）。
- action 映射到 HTTP method + path；list/get → GET，create → POST，update → PUT，delete → DELETE。
- 环境变量：`HMALL_API_BASE`（默认 `http://localhost:8080/api`）。

---

## 四、验证方法

### 1. 本地运行与手工验证

- 启动后端 + MCP（`cd hmall-mcp && npm run start:http`）。
- 在 Cursor 中列出 tools，确认名称与描述；对关键 action 调用 1～2 次，检查返回与错误提示。

### 2. 检查清单

- [ ] **范围分析**：系统协调 API 未被暴露为 tool
- [ ] **Tool 收敛**：一资源一 tool，用 action 区分
- [ ] **命名**：前缀 + 资源名，无动作拆分
- [ ] **参数**：与后端 API 一致；description 说明各 action 所需参数
- [ ] **错误**：API 4xx/5xx 被捕获并返回可读文本
- [ ] **文档**：`hmall-mcp/docs/TOOLS.md`（或 `TOOLS-<bc>.md`）已更新

### 3. 回归与扩展

- 新增/修改 BC 的 API 时，同步改 `hmall-mcp/tools/<bc>.js` 与 TOOLS 文档。
- 新增 BC 时：范围分析 → 新增 `tools/<bc>.js` → 入口注册 → 文档 → 检查清单。

---

## 五、参考

- **设计与实现示例**：`hmall-mcp/docs/TOOLS.md` 和 `hmall-mcp/tools/` 下已有 BC 的实现。
- **运行与配置**：`hmall-mcp/README.md`。
