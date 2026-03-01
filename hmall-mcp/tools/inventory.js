/**
 * Inventory 模块的 MCP tools：1 个 tool（inventory_stock）。
 * 仅暴露库存水位的查询与管理（用户操作入口），不暴露占用/释放（系统协调 API）。
 * 设计见 hmall-mcp/docs/TOOLS.md。
 */
import { z } from 'zod'

const CATALOG_API_BASE = process.env.HMALL_API_BASE || 'http://localhost:8080/api'
const INVENTORY_API_BASE = process.env.HMALL_INVENTORY_API_BASE || 'http://localhost:8083/api'

async function callApi(baseUrl, method, path, body) {
  const url = `${baseUrl}${path}`
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body !== undefined) {
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(url, opts)
  const text = await res.text()
  if (!res.ok) {
    let msg = `${res.status}`
    try { msg = JSON.parse(text).message || msg } catch {}
    throw new Error(msg)
  }
  return text ? JSON.parse(text) : null
}

const inventoryApi = (method, path, body) => callApi(INVENTORY_API_BASE, method, path, body)
const catalogApi = (method, path) => callApi(CATALOG_API_BASE, method, path)

async function fetchSkuName(skuId) {
  try {
    const sku = await catalogApi('GET', `/skus/${skuId}`)
    const spec = (sku.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join('/')
    const name = sku.spuName || sku.displayName || ''
    return spec ? `${name} (${spec})` : name
  } catch {
    return ''
  }
}

function ok(text) {
  return { content: [{ type: 'text', text }] }
}

function err(e) {
  if (e.cause?.code === 'ECONNREFUSED' || e.message?.includes('ECONNREFUSED') || e.message?.includes('fetch failed')) {
    return { content: [{ type: 'text', text: `错误：无法连接后端服务，请确认服务已启动。原始错误：${e.message}` }] }
  }
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

export function registerInventoryTools(server) {
  server.tool(
    'inventory_stock',
    '库存水位查询与管理，按 SKU 粒度。每个 SKU 有 available（可用）和 reserved（已占用，由订单流程自动维护，不可手动改）。\n\n⚠️ 仅 PHYSICAL 类型商品需要库存。SERVICE 类型商品（碎屏险、延保等虚拟服务）不需要库存管理——下单时系统会自动跳过服务商品的库存占用。不要为 SERVICE SKU 设置库存。\n\naction=list 查全部库存（含 SKU 名称和汇总）；get(skuId) 查单个；update(skuId, available) 设置可用库存（不存在则自动创建）。',
    {
      action: z.enum(['list', 'get', 'update']).describe('list|get|update'),
      skuId: z.number().optional().describe('get/update 时必填，SKU ID'),
      available: z.number().min(0).optional().describe('update 时必填，可用数量（≥0）'),
    },
    async ({ action, skuId, available }) => {
      try {
        if (action === 'list') {
          const stocks = await inventoryApi('GET', '/inventory/stock')
          if (!stocks.length) return ok('当前无库存记录。')
          const names = await Promise.all(stocks.map(s => fetchSkuName(s.skuId)))
          const lines = stocks.map((s, i) => {
            const label = names[i] ? `${names[i]}` : `SKU ${s.skuId}`
            return `SKU ${s.skuId} ${label}：可用 ${s.available}，已占用 ${s.reserved}`
          })
          const totalAvailable = stocks.reduce((sum, s) => sum + s.available, 0)
          const totalReserved = stocks.reduce((sum, s) => sum + s.reserved, 0)
          lines.push('──────')
          lines.push(`合计 ${stocks.length} 个 SKU：可用 ${totalAvailable}，已占用 ${totalReserved}`)
          return ok(lines.join('\n'))
        }
        if (action === 'get') {
          const s = await inventoryApi('GET', `/inventory/stock/${skuId}`)
          return ok(`SKU ${s.skuId} 库存：可用 ${s.available}，已占用 ${s.reserved}`)
        }
        if (action === 'update') {
          const s = await inventoryApi('PUT', `/inventory/stock/${skuId}`, { available })
          const name = await fetchSkuName(skuId)
          const label = name ? `${name}` : `SKU ${s.skuId}`
          return ok(`${label}（SKU ${s.skuId}）库存已更新：可用 ${s.available}，已占用 ${s.reserved}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
