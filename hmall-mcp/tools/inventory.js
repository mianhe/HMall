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

async function fetchSkuInfo(skuId) {
  try {
    const sku = await catalogApi('GET', `/skus/${skuId}`)
    const spec = (sku.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join('/')
    const spuName = sku.spuName || sku.displayName || ''
    return {
      spuId: sku.spuId ?? null,
      spuName,
      productType: sku.productType || 'PHYSICAL',
      spec,
      label: spec ? `${spuName} (${spec})` : spuName,
    }
  } catch {
    return { spuId: null, spuName: '', productType: 'PHYSICAL', spec: '', label: '' }
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
    '库存水位查询与管理，按 SKU 粒度。每个 SKU 有 available（可用）和 reserved（已占用，由订单流程自动维护，不可手动改）。\n\n⚠️ 仅 PHYSICAL 类型商品需要库存。SERVICE 类型商品（碎屏险、延保等虚拟服务）不需要库存管理——下单时系统会自动跳过服务商品的库存占用。不要为 SERVICE SKU 设置库存。\n\naction=health 商品级库存健康检查（1 次调用交叉比对商品目录与库存，分为全部有货 / 部分缺货 / 全部缺货 / 未初始化四档）；list 查全部库存明细（按商品分组）；get(skuId) 查单个；update(skuId, available) 设置可用库存（不存在则自动创建）。',
    {
      action: z.enum(['health', 'list', 'get', 'update']).describe('health|list|get|update'),
      skuId: z.number().optional().describe('get/update 时必填，SKU ID'),
      available: z.number().min(0).optional().describe('update 时必填，可用数量（≥0）'),
    },
    async ({ action, skuId, available }) => {
      try {
        if (action === 'health') {
          const [products, stocks] = await Promise.all([
            catalogApi('GET', '/products/search?keyword=&include=skus'),
            inventoryApi('GET', '/inventory/stock'),
          ])
          const stockBySkuId = new Map(stocks.map(s => [s.skuId, s]))
          const physical = products.filter(p => (p.productType || 'PHYSICAL') === 'PHYSICAL')

          const fullyStocked = []
          const partiallyOut = []
          const allOut = []
          const uninitialized = []

          for (const p of physical) {
            const skus = p.skus || []
            if (!skus.length) continue
            let inStock = 0, zeroStock = 0, noRecord = 0
            for (const sku of skus) {
              const s = stockBySkuId.get(sku.id)
              if (!s) noRecord++
              else if (s.available > 0) inStock++
              else zeroStock++
            }
            if (inStock === skus.length) {
              fullyStocked.push(p.name)
            } else if (inStock > 0) {
              partiallyOut.push({ name: p.name, total: skus.length, out: zeroStock + noRecord })
            } else if (noRecord === skus.length) {
              uninitialized.push({ name: p.name, count: skus.length })
            } else {
              allOut.push({ name: p.name, count: skus.length })
            }
          }

          const lines = [`库存健康检查（仅 PHYSICAL 商品，共 ${physical.length} 个）`]
          if (fullyStocked.length) {
            lines.push(`\n✅ 全部有货（${fullyStocked.length}）：${fullyStocked.join('、')}`)
          }
          if (partiallyOut.length) {
            lines.push(`\n⚠️ 部分缺货（${partiallyOut.length}）：`)
            for (const item of partiallyOut) {
              lines.push(`  - ${item.name}：${item.out}/${item.total} 个 SKU 缺货或未初始化`)
            }
          }
          if (allOut.length) {
            lines.push(`\n❌ 全部缺货（${allOut.length}）：`)
            for (const item of allOut) {
              lines.push(`  - ${item.name}（${item.count} 个 SKU 均为 0）`)
            }
          }
          if (uninitialized.length) {
            lines.push(`\n🔕 未初始化库存（${uninitialized.length}）：`)
            for (const item of uninitialized) {
              lines.push(`  - ${item.name}（${item.count} 个 SKU 无库存记录）`)
            }
          }
          if (!partiallyOut.length && !allOut.length && !uninitialized.length) {
            lines.push('\n所有商品库存正常。')
          }
          return ok(lines.join('\n'))
        }
        if (action === 'list') {
          const stocks = await inventoryApi('GET', '/inventory/stock')
          if (!stocks.length) return ok('当前无库存记录。')
          const infos = await Promise.all(stocks.map(s => fetchSkuInfo(s.skuId)))

          const groupOrder = []
          const groups = new Map()
          stocks.forEach((s, i) => {
            const info = infos[i]
            const key = info.spuId != null ? String(info.spuId) : `_unknown_${s.skuId}`
            if (!groups.has(key)) {
              groups.set(key, { spuName: info.spuName, productType: info.productType, skus: [] })
              groupOrder.push(key)
            }
            groups.get(key).skus.push({ skuId: s.skuId, available: s.available, reserved: s.reserved, spec: info.spec })
          })

          const lines = []
          const allOutOfStock = []
          for (const key of groupOrder) {
            const g = groups.get(key)
            if (g.productType === 'SERVICE') continue
            const heading = g.spuName || '未知商品'
            lines.push(`\n📦 ${heading}`)
            for (const sku of g.skus) {
              const specLabel = sku.spec ? ` (${sku.spec})` : ''
              lines.push(`  SKU ${sku.skuId}${specLabel}：可用 ${sku.available}，已占用 ${sku.reserved}`)
            }
            if (g.skus.every(sku => sku.available === 0)) {
              allOutOfStock.push({ name: heading, count: g.skus.length })
            }
          }

          const totalAvailable = stocks.reduce((sum, s) => sum + s.available, 0)
          const totalReserved = stocks.reduce((sum, s) => sum + s.reserved, 0)
          lines.push('\n──────')
          lines.push(`合计 ${stocks.length} 个 SKU：可用 ${totalAvailable}，已占用 ${totalReserved}`)

          if (allOutOfStock.length) {
            lines.push('\n⚠️ 以下商品所有 SKU 均缺货（available=0）：')
            for (const item of allOutOfStock) {
              lines.push(`  - ${item.name}（${item.count} 个 SKU）`)
            }
          }

          return ok(lines.join('\n'))
        }
        if (action === 'get') {
          const s = await inventoryApi('GET', `/inventory/stock/${skuId}`)
          return ok(`SKU ${s.skuId} 库存：可用 ${s.available}，已占用 ${s.reserved}`)
        }
        if (action === 'update') {
          const s = await inventoryApi('PUT', `/inventory/stock/${skuId}`, { available })
          const info = await fetchSkuInfo(skuId)
          const label = info.label || `SKU ${s.skuId}`
          return ok(`${label}（SKU ${s.skuId}）库存已更新：可用 ${s.available}，已占用 ${s.reserved}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
