/**
 * Cart 模块的 MCP tools：1 个 tool（cart_manage）。
 * 消费者端购物车管理：查看、加购、改数量、删除、结算预览。
 * userId 由 Smart Interaction 服务端注入（决策 S10），不由 LLM 自行传递。
 */
import { z } from 'zod'

const CART_API_BASE = process.env.HMALL_CART_API_BASE || 'http://localhost:8087/api'

async function cartApi(method, path, body, userId) {
  const url = `${CART_API_BASE}${path}`
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': String(userId),
    },
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
  if (res.status === 204) return null
  return text ? JSON.parse(text) : null
}

function ok(text) {
  return { content: [{ type: 'text', text }] }
}

function err(e) {
  if (e.cause?.code === 'ECONNREFUSED' || e.message?.includes('ECONNREFUSED') || e.message?.includes('fetch failed')) {
    return { content: [{ type: 'text', text: `错误：无法连接后端服务（${CART_API_BASE}），请确认服务已启动。原始错误：${e.message}` }] }
  }
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

function formatPrice(cents) {
  return `¥${(cents / 100).toFixed(2)}`
}

export function registerCartTools(server) {
  server.tool(
    'cart_manage',
    '购物车管理（按用户隔离，userId 系统自动注入）。action=list 查看购物车（返回 cartItemId、skuId、商品名、数量、单价、商品类型）；add(skuId, quantity) 加购实体商品，add(skuId, quantity, relatedSkuId) 加购服务商品（relatedSkuId 为关联的实体 SKU ID，SERVICE 商品加购时必传）；update_quantity(cartItemId, quantity) 改数量（0=删除）；remove(cartItemId) 删除；checkout_preview(cartItemIds) 结算预览（返回明细、服务分组和总价）。',
    {
      action: z.enum(['list', 'add', 'update_quantity', 'remove', 'checkout_preview'])
        .describe('list|add|update_quantity|remove|checkout_preview'),
      userId: z.number().optional().describe('用户 ID（系统自动注入，无需手动传递）'),
      skuId: z.number().optional().describe('add 时必填，SKU ID'),
      relatedSkuId: z.number().optional().describe('add 时可选：服务 SKU 关联的实体 skuId（仅 SERVICE 商品加购时需要）'),
      quantity: z.number().optional().describe('add 时必填（≥1）；update_quantity 时必填（0 = 删除）'),
      cartItemId: z.number().optional().describe('update_quantity/remove 时必填，购物车项 ID'),
      cartItemIds: z.array(z.number()).optional().describe('checkout_preview 时必填，选中的购物车项 ID 列表'),
    },
    async ({ action, userId, skuId, relatedSkuId, quantity, cartItemId, cartItemIds }) => {
      if (!userId) return err(new Error('未登录，请先登录'))
      try {
        if (action === 'list') {
          const items = await cartApi('GET', '/cart', undefined, userId)
          if (!items || !items.length) return ok('购物车为空。')
          const lines = items.map(item => {
            const name = item.skuName || `SKU ${item.skuId}`
            const price = item.skuPrice != null ? formatPrice(item.skuPrice) : '价格未知'
            const avail = item.available === false ? '（已下架）' : ''
            const type = item.productType ? ` [${item.productType}]` : ''
            const related = item.relatedSkuId ? ` (关联实体SKU:${item.relatedSkuId})` : ''
            return `cartItemId=${item.cartItemId}  skuId=${item.skuId}${type}  ${name} × ${item.quantity}  单价 ${price}${avail}${related}`
          })
          return ok(lines.join('\n'))
        }

        if (action === 'add') {
          const body = { skuId, quantity }
          if (relatedSkuId != null) body.relatedSkuId = relatedSkuId
          const item = await cartApi('POST', '/cart/items', body, userId)
          return ok(`已添加到购物车：SKU ${item.skuId}，数量 ${item.quantity}${item.relatedSkuId ? '，关联实体SKU ' + item.relatedSkuId : ''}`)
        }

        if (action === 'update_quantity') {
          if (quantity === 0) {
            await cartApi('PUT', `/cart/items/${cartItemId}`, { quantity: 0 }, userId)
            return ok(`购物车项 ${cartItemId} 已删除`)
          }
          const item = await cartApi('PUT', `/cart/items/${cartItemId}`, { quantity }, userId)
          return ok(`购物车项 ${cartItemId} 数量已更新为 ${item.quantity}`)
        }

        if (action === 'remove') {
          await cartApi('DELETE', `/cart/items/${cartItemId}`, undefined, userId)
          return ok(`购物车项 ${cartItemId} 已删除`)
        }

        if (action === 'checkout_preview') {
          const preview = await cartApi('POST', '/cart/checkout-preview', { cartItemIds }, userId)
          const lines = preview.items.map(item => {
            const type = item.productType ? ` [${item.productType}]` : ''
            return `skuId=${item.skuId}${type}  ${item.skuName} × ${item.quantity}  单价 ${formatPrice(item.price)}  小计 ${formatPrice(item.subtotal)}`
          })
          if (preview.groups && preview.groups.length) {
            lines.push('')
            lines.push('服务分组：')
            for (const g of preview.groups) {
              lines.push(`  实体商品 skuId=${g.primarySkuId} ${g.primarySkuName || ''}`)
              for (const si of (g.serviceItems || [])) {
                lines.push(`    + ${si.skuName} × ${si.quantity}  ${formatPrice(si.price)}`)
              }
              lines.push(`    分组小计：${formatPrice(g.groupSubtotal)}`)
            }
          }
          lines.push('──────')
          lines.push(`总计：${formatPrice(preview.totalPrice)}`)
          return ok(lines.join('\n'))
        }

        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
