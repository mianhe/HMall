#!/usr/bin/env node
/**
 * 验证本地 MCP 是否可被 smart-interaction-service 使用（Host 127.0.0.1 放行 + tools/list 成功）。
 * 用法：node scripts/verify-mcp-local.mjs [MCP_BASE_URL]
 * 默认 MCP_BASE_URL=http://127.0.0.1:3000
 */
const base = (process.argv[2] || 'http://127.0.0.1:3000').replace(/\/$/, '')
const mcpUrl = base + '/mcp'

async function run() {
  console.log('验证本地 MCP（Host: 127.0.0.1）...')

  const initBody = JSON.stringify({
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: {
      protocolVersion: '2025-03-26',
      capabilities: { tools: {} },
      clientInfo: { name: 'hmall-smart-interaction', version: '1.0.0' },
    },
  })

  const initRes = await fetch(mcpUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json, text/event-stream',
      Host: new URL(mcpUrl).host,
    },
    body: initBody,
  })

  if (!initRes.ok) {
    console.error('MCP initialize 失败:', initRes.status, initRes.statusText)
    if (initRes.status === 403) {
      console.error('→ 可能原因：MCP allowedHosts 未包含 127.0.0.1，请确认 hmall-mcp/index-http.js 默认含 127.0.0.1 并重启 MCP')
    }
    process.exit(1)
  }

  const sessionId = initRes.headers.get('mcp-session-id')
  if (!sessionId) {
    console.error('MCP 未返回 mcp-session-id')
    process.exit(1)
  }

  const listBody = JSON.stringify({
    jsonrpc: '2.0',
    id: 2,
    method: 'tools/list',
    params: {},
  })

  const listRes = await fetch(mcpUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json, text/event-stream',
      'mcp-session-id': sessionId,
      Host: new URL(mcpUrl).host,
    },
    body: listBody,
  })

  if (!listRes.ok) {
    console.error('MCP tools/list 失败:', listRes.status)
    process.exit(1)
  }

  const listText = await listRes.text()
  const dataLine = listText.split('\n').find((l) => l.startsWith('data:'))
  const listJson = dataLine ? JSON.parse(dataLine.slice(5).trim()) : {}
  const tools = listJson?.result?.tools ?? []
  if (tools.length === 0) {
    console.error('MCP 返回 0 个工具，智能助手将无法查询数据')
    process.exit(1)
  }

  console.log('OK：MCP 可访问，工具数:', tools.length)
}

run().catch((e) => {
  console.error('验证失败:', e.message)
  if (e.cause?.code === 'ECONNREFUSED') {
    console.error('→ 请先启动 MCP：cd hmall-mcp && npm run start:http')
  }
  process.exit(1)
})
