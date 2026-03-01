# HMall MCP Server

通过 AI 对话（Cursor、Claude Desktop 等任意 MCP Client）操作 HMall 商品管理系统。

## 前置条件

- Node.js 18+
- HMall catalog-service 已启动（默认 `http://localhost:8080`）

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

## 文档

| 文档 | 说明 |
|------|------|
| [docs/TOOLS.md](docs/TOOLS.md) | **全部 MCP Tools 说明**：18 个 tool 的用途、参数与示例 |
| 本 README | 安装、运行方式、配置、环境变量 |

tool 的 description 和参数 schema 定义在 `tools/*.js` 的 `server.tool()` 调用中，MCP SDK 会自动暴露给 AI。

## 可用 Tools（18 个，覆盖 7 个 BC）

### Catalog（10 个）

| Tool | 说明 |
|------|------|
| `catalog_categories` | 类目：list / tree / get / create / update / delete |
| `catalog_products` | 商品(SPU)：list / get / create(含 productType) / update / delete |
| `catalog_dimensions` | 规格维度与选项：list / add_dimension / add_option / delete_option |
| `catalog_skus` | SKU：list / get / get_by_id(不需 spuId) / create / update / delete |
| `catalog_service_bindings` | 服务绑定：list / create / update(改价格) / delete |
| `catalog_available_services` | 查某实体商品的可选服务列表（从实体商品角度） |
| `catalog_upload_image` | 上传本地图片，返回 URL |
| `catalog_product_images` | 产品级展示图：list / add / delete |
| `catalog_option_images` | 选项级展示图：list / add / delete |

### Inventory / Cart / Order / Fulfillment / User / Activity（8 个）

| Tool | 说明 |
|------|------|
| `inventory_stock` | 库存水位：list / get / update（仅 PHYSICAL 商品需要） |
| `cart_manage` | 购物车：list / add(含服务加购) / update_quantity / remove / checkout_preview |
| `order_query` | 订单查询：get / list / cancel |
| `order_create` | 创建订单 |
| `fulfillment_orders` | 履约单：get / list / allocate / ship / deliver（含实体与虚拟） |
| `user_manage` | 用户管理：list / get / create |
| `user_addresses` | 收货地址：list / get / create / update / delete |
| `activity_query` | 业务活动：list / recent / stats |

各 tool 的详细参数见 [docs/TOOLS.md](docs/TOOLS.md)。

## 代码结构

```
hmall-mcp/
├── index.js           ← stdio 入口
├── index-http.js      ← HTTP 入口
├── tools/
│   ├── catalog.js     ← Catalog（10 tools）
│   ├── inventory.js   ← Inventory（1 tool）
│   ├── cart.js        ← Cart（1 tool）
│   ├── order.js       ← Order（2 tools）
│   ├── fulfillment.js ← Fulfillment（1 tool）
│   ├── user.js        ← User（2 tools）
│   └── activity.js    ← Activity（1 tool）
├── docs/
│   └── TOOLS.md       ← 全部 18 个 tool 的说明
└── README.md
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `HMALL_API_BASE` | `http://localhost:8080/api` | 后端 API 地址 |
| `PORT` | `3000` | 仅 HTTP 模式：监听端口（`index-http.js`） |
