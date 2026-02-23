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
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

function formatPrice(cents) {
  return `¥${(cents / 100).toFixed(2)}`
}

export function registerCartTools(server) {
  server.tool(
    'cart_manage',
    '购物车管理（按用户隔离，userId 系统自动注入）。action=list 查看购物车（返回 cartItemId、skuId、商品名、数量、单价）；add(skuId, quantity) 加购；update_quantity(cartItemId, quantity) 改数量（0=删除）；remove(cartItemId) 删除；checkout_preview(cartItemIds) 结算预览（返回明细和总价）。',
    {
      action: z.enum(['list', 'add', 'update_quantity', 'remove', 'checkout_preview'])
        .describe('list|add|update_quantity|remove|checkout_preview'),
      userId: z.number().optional().describe('用户 ID（系统自动注入，无需手动传递）'),
      skuId: z.number().optional().describe('add 时必填，SKU ID'),
      quantity: z.number().optional().describe('add 时必填（≥1）；update_quantity 时必填（0 = 删除）'),
      cartItemId: z.number().optional().describe('update_quantity/remove 时必填，购物车项 ID'),
      cartItemIds: z.array(z.number()).optional().describe('checkout_preview 时必填，选中的购物车项 ID 列表'),
    },
    async ({ action, userId, skuId, quantity, cartItemId, cartItemIds }) => {
      if (!userId) return err(new Error('未登录，请先登录'))
      try {
        if (action === 'list') {
          const items = await cartApi('GET', '/cart', undefined, userId)
          if (!items || !items.length) return ok('购物车为空。')
          const lines = items.map(item => {
            const name = item.skuName || `SKU ${item.skuId}`
            const price = item.skuPrice != null ? formatPrice(item.skuPrice) : '价格未知'
            const avail = item.available === false ? '（已下架）' : ''
            return `cartItemId=${item.cartItemId}  skuId=${item.skuId}  ${name} × ${item.quantity}  单价 ${price}${avail}`
          })
          return ok(lines.join('\n'))
        }

        if (action === 'add') {
          const item = await cartApi('POST', '/cart/items', { skuId, quantity }, userId)
          return ok(`已添加到购物车：SKU ${item.skuId}，数量 ${item.quantity}`)
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
          const lines = preview.items.map(item =>
            `skuId=${item.skuId}  ${item.skuName} × ${item.quantity}  单价 ${formatPrice(item.price)}  小计 ${formatPrice(item.subtotal)}`
          )
          lines.push('──────')
          lines.push(`总计：${formatPrice(preview.totalPrice)}`)
          return ok(lines.join('\n'))
        }

        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
