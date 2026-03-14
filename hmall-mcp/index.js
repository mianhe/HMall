#!/usr/bin/env node
/**
 * HMall MCP Server 入口
 * 通过 stdio 与 MCP Client（如 Cursor）通信，调用后端 REST API。
 */
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js'
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js'
import { registerCatalogTools } from './tools/catalog.js'
import { registerInventoryTools } from './tools/inventory.js'
import { registerCartTools } from './tools/cart.js'
import { registerOrderTools } from './tools/order.js'
import { registerFulfillmentTools } from './tools/fulfillment.js'
import { registerUserTools } from './tools/user.js'
import { registerActivityTools } from './tools/activity.js'
import { registerOrderFactTools } from './tools/order-fact.js'
import { registerCatalogResources } from './resources/catalog-domain.js'
import { registerInventoryResources } from './resources/inventory-domain.js'
import { registerCartOrderResources } from './resources/cart-order-domain.js'
import { registerFulfillmentResources } from './resources/fulfillment-domain.js'
import { registerActivityResources } from './resources/activity-domain.js'
import { registerOntologyResources } from './resources/ontology.js'

const server = new McpServer({
  name: 'hmall-mcp',
  version: '0.1.0',
})

// 注册各模块 tools
registerCatalogTools(server)
registerInventoryTools(server)
registerCartTools(server)
registerOrderTools(server)
registerFulfillmentTools(server)
registerUserTools(server)
registerActivityTools(server)
registerOrderFactTools(server)

// 注册领域知识 resources
registerCatalogResources(server)
registerInventoryResources(server)
registerCartOrderResources(server)
registerFulfillmentResources(server)
registerActivityResources(server)
registerOntologyResources(server)

// 启动
async function main() {
  const transport = new StdioServerTransport()
  await server.connect(transport)
  console.error('HMall MCP Server running on stdio')
}

main().catch((error) => {
  console.error('Fatal error:', error)
  process.exit(1)
})
