#!/usr/bin/env node
/**
 * 通过 MCP 协议调用 HMall MCP 的 tool（仅用 MCP，不直接调 REST）。
 * 用法: node scripts/call-mcp-tool.mjs catalog_products '{"action":"list","keyword":"pad"}'
 */
import { pathToFileURL } from 'node:url'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = join(__dirname, '..')

const toolName = process.argv[2]
const argsJson = process.argv[3] || '{}'
if (!toolName) {
  console.error('用法: node scripts/call-mcp-tool.mjs <toolName> [<argsJson>]')
  process.exit(1)
}
let args
try {
  args = JSON.parse(argsJson)
} catch (e) {
  console.error('无效的 argsJson:', e.message)
  process.exit(1)
}

const transport = new StdioClientTransport({
  command: process.execPath,
  args: [join(root, 'index.js')],
  cwd: root,
  env: { ...process.env },
})

const client = new Client(
  { name: 'hmall-mcp-caller', version: '0.0.1' },
  {}
)

async function main() {
  await client.connect(transport)
  const result = await client.callTool({ name: toolName, arguments: args })
  if (result.isError) {
    console.error('Tool 报错:', result.content?.[0]?.text ?? result)
    process.exit(1)
  }
  const text = result.content?.[0]?.text ?? JSON.stringify(result)
  console.log(text)
  await transport.close()
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
