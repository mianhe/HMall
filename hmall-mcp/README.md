# HMall MCP Server

通过 AI 对话（Cursor、Claude Desktop 等任意 MCP Client）操作 HMall 商品管理系统。

## 前置条件

- Node.js 18+
- HMall 后端已启动（默认 `http://localhost:8080`）

## 安装

```bash
cd hmall-mcp
npm install
```

## 两种运行方式

| 方式 | 适用场景 | 启动方式 |
|------|----------|----------|
| **stdio** | 由 Client 按需拉起子进程（如 Cursor 用 command 启动） | `npm start` 或 `node index.js` |
| **HTTP** | 独立进程，对外提供 URL，任意 Client 均可连接 | `npm run start:http` 或 `node index-http.js` |

**若不希望依赖 Cursor 独家拉起**，建议用 **HTTP 方式**：单独用脚本或进程管理器（如 pm2、systemd）常驻运行，所有 MCP Client 通过同一 URL 连接。

### 独立运行（HTTP，推荐对外服务）

```bash
npm run start:http
# 默认监听 http://127.0.0.1:3000/mcp，可通过环境变量 PORT 修改端口
```

任意支持 MCP Streamable HTTP 的 Client 配置 **URL** 即可，例如：

- **Cursor**：MCP 里添加类型为 **URL** 的 server，地址填 `http://127.0.0.1:3000/mcp`（需先在本机启动上述命令）
- **Claude Desktop / 自研 Client**：连接 `http://<主机>:3000/mcp`（若需局域网或公网访问，可设 `PORT` 并用反向代理或防火墙放行）

### 在 Cursor 中配置（二选一）

**方式 A：URL（需先独立启动 HTTP Server）**

- **Name**: `hmall`
- **Type**: `url`
- **URL**: `http://127.0.0.1:3000/mcp`

**方式 B：Command（由 Cursor 按需拉起 stdio 进程）**

- **Name**: `hmall`
- **Type**: `command`
- **Command**: `node /绝对路径/HMall/hmall-mcp/index.js`

或在 `.cursor/mcp.json` 中：

```json
{
  "mcpServers": {
    "hmall": {
      "url": "http://127.0.0.1:3000/mcp"
    }
  }
}
```

或使用 command 方式：

```json
{
  "mcpServers": {
    "hmall": {
      "command": "node",
      "args": ["/绝对路径/HMall/hmall-mcp/index.js"]
    }
  }
}
```

## 可用 Tools

tool 的 description 和参数 schema 定义在 `tools/catalog.js` 的 `server.tool()` 调用中，MCP SDK 会自动暴露给 AI。

### 类目

| Tool | 说明 | 参数 |
|------|------|------|
| `catalog_list_categories` | 查类目列表 | `parentId?`: number — 父类目 ID，不传查根类目 |
| `catalog_create_category` | 创建类目 | `name`: string, `description?`: string, `parentId?`: number |

### 商品（SPU）

| Tool | 说明 | 参数 |
|------|------|------|
| `catalog_list_products` | 按类目查商品列表 | `categoryId`: number |
| `catalog_get_product` | 查商品详情 | `productId`: number |
| `catalog_create_product` | 创建商品 | `categoryId`: number, `name`: string, `description?`: string |

### 规格维度与选项

| Tool | 说明 | 参数 |
|------|------|------|
| `catalog_list_dimensions` | 查某 SPU 的维度及选项 | `spuId`: number |
| `catalog_add_dimension` | 为 SPU 添加维度 | `spuId`: number, `name`: string, `required`: boolean, `affectsAppearance?`: boolean |
| `catalog_add_option` | 为维度添加选项 | `spuId`: number, `dimensionId`: number, `optionValue`: string, `sortOrder?`: number, `image?`: string |

### SKU

| Tool | 说明 | 参数 |
|------|------|------|
| `catalog_list_skus` | 查某 SPU 下 SKU 列表 | `spuId`: number |
| `catalog_create_sku` | 创建 SKU | `spuId`: number, `specOptionIds`: number[], `priceCents`: number (>=0), `displayName?`: string |

> 参数后带 `?` 表示可选，不带则必填。

## 代码结构

```
hmall-mcp/
├── index.js           ← stdio 入口：由 Client 拉起的子进程
├── index-http.js      ← HTTP 入口：独立进程，监听端口，供任意 Client 通过 URL 连接
├── tools/
│   └── catalog.js     ← Catalog 模块 tools（description + schema + 调 API）
└── README.md
```

每个 tool 在 `server.tool()` 中定义了三部分：
- **name**: tool 名称（如 `catalog_create_sku`）
- **description**: 说明文本，AI 据此决定何时调用
- **inputSchema**: 用 zod 定义的参数 schema，SDK 自动转 JSON Schema 暴露给 MCP Client

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `HMALL_API_BASE` | `http://localhost:8080/api` | 后端 API 地址 |
| `PORT` | `3000` | 仅 HTTP 模式：监听端口（`index-http.js`） |
