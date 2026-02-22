---
name: mcp-development
description: 为 HMall 中作为入口的独立 BC 设计并实现 MCP tools：设计原则（收敛、命名、文档）、实现位置与 schema、验证与文档同步。触发词：MCP 开发、为 BC 提供 MCP、MCP tools、入口 BC 的 MCP、新增 MCP 工具。
---

# MCP 开发（为入口 BC 提供 MCP Tools）

适用于「独立 BC 作为入口、对外暴露 MCP」的场景。该 BC 可能协调其它 BC，但 MCP 的**入口**是本 BC 的聚合与能力；其它 BC 通过本 BC 的 API 或事件被协调，不直接暴露为独立 tool 集。

## 一、设计原则

### 1. Tool 收敛：一资源一 Tool，用 action 区分操作

- **不要**为同一资源的 list/get/create/update/delete 各建一个 tool（易膨胀到 30+）。
- **要**按资源收敛：一个资源一个 tool，用参数 `action`（或等价）区分 list | get | create | update | delete；必要时 list 支持多种模式（如 list + tree、list + search）。
- 读操作合并：list 与 search 合并为一个 list（可选 keyword/categoryId）；get 与 get_full 合并为一个 get（可选 detail: basic | full）。
- 参考：Catalog 收敛后为 7～8 个 tools（categories、products、dimensions、skus、upload_image、product_images、option_images），见 `hmall-mcp/docs/TOOLS.md`。

### 2. 命名约定

- 前缀：`<bc>_`，如 `catalog_`。同一 MCP Server 若暴露多 BC，前缀区分。
- 资源名：小写、复数或单数一致，如 `catalog_categories`、`catalog_products`、`catalog_skus`。
- 不按动作拆 tool 名：避免 `catalog_list_products`、`catalog_get_product` 等并立，用 `catalog_products` + `action=list|get|...`。

### 3. 只暴露已有能力，不发明新用例

- MCP 层只**调用该 BC 已有 REST API**（或 BFF/聚合 API），不在此层实现新业务规则。
- 参数与后端 API 对齐：必填/可选、类型、语义一致；返回给 AI 的文本可读、结构化（如列表一行一条、错误信息清晰）。

### 4. 面向 AI 的返回与错误

- 返回内容为**文本**（或 MCP 规定的 content 格式），便于 LLM 解析与引用。
- 错误：捕获 API 异常，返回简短、可读的错误说明（如「错误：404」或后端 message），不要抛未处理异常。

### 5. 文档与实现同步

- 每个暴露 tool 的 BC 在 **hmall-mcp** 内有一份 **Tools 说明**：列出 tool 名、用途、参数、示例。
- 收敛后的「目标设计」写在 `hmall-mcp/docs/TOOLS.md`（或按 BC 拆为 `hmall-mcp/docs/TOOLS-<bc>.md`）；实现若分步收敛，文档中注明当前实现状态（如「当前仍为多 tool，将逐步合并」）。
- README 中保留「文档」小节，指向上述 TOOLS 文档。

### 6. 入口 BC 的边界

- **入口 BC**：对外（用户、其他 Agent）提供查询与操作入口的 BC；其聚合与用例是 MCP 的直接映射对象。
- 若该 BC 会协调其它 BC（如下单时调库存、支付），协调逻辑留在**后端或 BFF**，MCP 只调本 BC（或 BFF）的接口；不在 MCP 层拼装多 BC 的多个 tool 调用顺序（除非是明确的「编排类」设计并文档化）。

---

## 二、开发与实现

### 1. 代码位置

- 所有 MCP 代码在 **hmall-mcp/** 下。
- 入口：`index.js`（stdio）、`index-http.js`（HTTP）。
- 每个 BC 的 tools：在 **hmall-mcp/tools/** 下单一文件（如 `catalog.js`），在入口中 `registerXxxTools(server)` 注册。
- 共享：API 调用、上传等可抽成 `hmall-mcp/tools/common.js` 或内联在对应 BC 的 tool 文件中。

### 2. Tool 定义方式

- 使用 MCP SDK 的 `server.tool(name, description, inputSchema, handler)`。
- **name**：符合命名约定，如 `catalog_products`。
- **description**：中文简短说明，含「何时用」与「action 可选值」，便于 AI 选 tool。
- **inputSchema**：用 **zod** 定义，SDK 会转成 JSON Schema；必填/可选与后端一致；若收敛为单 tool 多 action，则 `action` 为枚举，其余参数按 action 条件必填（在 description 中说明）。
- **handler**：async，内部调 `api(method, path, body)` 等访问后端；返回 `{ content: [{ type: 'text', text }] }`；错误用 try/catch 转成可读文本再返回。

### 3. 与后端 API 的对应

- 先确认该 BC 的 REST API（如 `docs/bounded-contexts/<bc>/api.yaml` 或已实现接口）。
- 每个 tool 的 action 映射到 HTTP method 与 path；list/get 用 GET，create 用 POST，update 用 PUT，delete 用 DELETE。
- 环境变量：`HMALL_API_BASE`（默认 `http://localhost:8080/api`），用于拼请求 URL。

---

## 三、验证方法

### 1. 本地运行与手工验证

- 启动后端（对应 BC 的 service 或 BFF）。
- 启动 MCP：`cd hmall-mcp && npm run start:http`（或 stdio 方式通过 Cursor 拉起）。
- 在 Cursor（或其它 MCP Client）中：列出 tools，确认名称与描述符合设计；对关键 action 调用 1～2 次，传必填参数，检查返回文本与错误提示。

### 2. 检查清单

- [ ] Tool 数量收敛：该 BC 是否仍是一资源一 tool、用 action 区分？
- [ ] 命名：前缀 + 资源名，无「list_/get_/create_」拆成多 tool。
- [ ] 参数：与后端 API 一致；description 中说明各 action 所需参数。
- [ ] 错误：API 4xx/5xx 被捕获并返回可读文本。
- [ ] 文档：`hmall-mcp/docs/TOOLS.md`（或 TOOLS-&lt;bc&gt;.md）已更新，包含所有 tool 的用途、参数表与示例。
- [ ] README：文档小节指向 TOOLS 文档，并注明当前实现状态（若与目标设计不一致）。

### 3. 回归与扩展

- 新增或修改 BC 的 API 时，若影响 MCP 暴露能力，同步改 `hmall-mcp/tools/<bc>.js` 与 TOOLS 文档。
- 新增「入口 BC」时：在 hmall-mcp 中新增 `tools/<bc>.js`、在入口中注册、新增或合并进 `docs/TOOLS.md`，并走一遍检查清单。

---

## 四、参考

- **设计示例**：`hmall-mcp/docs/TOOLS.md`（Catalog 收敛后的 8 个 tool 设计）。
- **实现示例**：`hmall-mcp/tools/catalog.js`（当前为多 tool 实现，可逐步改为收敛形态）。
- **运行与配置**：`hmall-mcp/README.md`。
