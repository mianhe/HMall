#!/usr/bin/env node
/**
 * HMall MCP Server — HTTP/Streamable 入口
 * 独立进程监听端口，供任意 MCP Client（Cursor、Claude Desktop、自研等）通过 URL 连接。
 * 不依赖 Cursor 拉起的 stdio 子进程。
 */
import { randomUUID } from 'node:crypto'
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js'
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js'
import { createMcpExpressApp } from '@modelcontextprotocol/sdk/server/express.js'
import { isInitializeRequest } from '@modelcontextprotocol/sdk/types.js'
import { registerCatalogTools } from './tools/catalog.js'
import { registerInventoryTools } from './tools/inventory.js'
import { registerCartTools } from './tools/cart.js'
import { registerOrderTools } from './tools/order.js'
import { registerFulfillmentTools } from './tools/fulfillment.js'
import { registerUserTools } from './tools/user.js'
import { registerActivityTools } from './tools/activity.js'

function createServer() {
  const server = new McpServer({
    name: 'hmall-mcp',
    version: '0.1.0',
  })
  registerCatalogTools(server)
  registerInventoryTools(server)
  registerCartTools(server)
  registerOrderTools(server)
  registerFulfillmentTools(server)
  registerUserTools(server)
  registerActivityTools(server)
  return server
}

const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000
const transports = {}

const app = createMcpExpressApp()

const mcpPostHandler = async (req, res) => {
  const sessionId = req.headers['mcp-session-id']
  try {
    let transport
    if (sessionId && transports[sessionId]) {
      transport = transports[sessionId]
    } else if (!sessionId && isInitializeRequest(req.body)) {
      transport = new StreamableHTTPServerTransport({
        sessionIdGenerator: () => randomUUID(),
        onsessioninitialized: (sid) => {
          transports[sid] = transport
        },
      })
      transport.onclose = () => {
        const sid = transport.sessionId
        if (sid && transports[sid]) {
          delete transports[sid]
        }
      }
      const server = createServer()
      await server.connect(transport)
      await transport.handleRequest(req, res, req.body)
      return
    } else {
      res.status(400).json({
        jsonrpc: '2.0',
        error: { code: -32000, message: 'Bad Request: No valid session ID provided' },
        id: null,
      })
      return
    }
    await transport.handleRequest(req, res, req.body)
  } catch (err) {
    console.error('MCP request error:', err)
    if (!res.headersSent) {
      res.status(500).json({
        jsonrpc: '2.0',
        error: { code: -32603, message: 'Internal server error' },
        id: null,
      })
    }
  }
}

const mcpGetHandler = async (req, res) => {
  const sessionId = req.headers['mcp-session-id']
  if (!sessionId || !transports[sessionId]) {
    res.status(400).send('Invalid or missing session ID')
    return
  }
  await transports[sessionId].handleRequest(req, res)
}

const mcpDeleteHandler = async (req, res) => {
  const sessionId = req.headers['mcp-session-id']
  if (!sessionId || !transports[sessionId]) {
    res.status(400).send('Invalid or missing session ID')
    return
  }
  try {
    await transports[sessionId].handleRequest(req, res)
  } catch (err) {
    console.error('Session termination error:', err)
    if (!res.headersSent) res.status(500).send('Error processing session termination')
  }
}

app.post('/mcp', mcpPostHandler)
app.get('/mcp', mcpGetHandler)
app.delete('/mcp', mcpDeleteHandler)

app.listen(PORT, (err) => {
  if (err) {
    console.error('Failed to start server:', err)
    process.exit(1)
  }
  console.log(`HMall MCP Server (HTTP) listening on http://127.0.0.1:${PORT}/mcp`)
})

process.on('SIGINT', async () => {
  for (const sid of Object.keys(transports)) {
    try {
      await transports[sid].close()
    } catch (e) {
      console.error('Error closing transport:', e)
    }
  }
  process.exit(0)
})
