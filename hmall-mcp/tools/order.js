/**
 * Order 模块的 MCP tools：2 个 tool（order_query、order_create）。
 * userId 由 Smart Interaction 服务端注入（决策 S10）。
 */
import { z } from 'zod'

const ORDER_API_BASE = process.env.HMALL_ORDER_API_BASE || 'http://localhost:8081/api'

async function orderApi(method, path, body) {
  const url = `${ORDER_API_BASE}${path}`
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

function ok(text) {
  return { content: [{ type: 'text', text }] }
}

function err(e) {
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

function formatPrice(cents) {
  return `¥${(cents / 100).toFixed(2)}`
}

const STATUS_LABELS = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  FULFILLING: '配货中',
  SHIPPED: '已发货',
  DELIVERED: '已签收',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

function formatOrder(order) {
  const status = STATUS_LABELS[order.status] || order.status
  const lines = [
    `订单号：${order.orderId}  状态：${status}  总金额：${formatPrice(order.totalAmountCents)}  下单时间：${order.createdAt || ''}`,
  ]
  if (order.items && order.items.length) {
    lines.push('商品明细：')
    for (const item of order.items) {
      const name = item.displayName || `SKU ${item.skuId}`
      lines.push(`  - ${name} × ${item.quantity}  单价 ${formatPrice(item.unitPriceCents)}  小计 ${formatPrice(item.totalPriceCents)}`)
    }
  }
  if (order.shippingAddress) {
    const addr = order.shippingAddress
    lines.push(`收货地址：${addr.recipientName} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detail}`)
  }
  return lines.join('\n')
}

export function registerOrderTools(server) {
  server.tool(
    'order_query',
    '订单查询与管理（userId 系统自动注入）。action=get(orderId) 查单个订单详情（含商品明细、状态、收货地址）；list 查当前用户订单列表（支持分页）；cancel(orderId) 取消订单（仅待付款/已付款/配货中状态可取消）。',
    {
      action: z.enum(['get', 'list', 'cancel']).describe('get|list|cancel'),
      userId: z.number().optional().describe('用户 ID（系统自动注入，无需手动传递）'),
      orderId: z.number().optional().describe('get/cancel 时必填，订单 ID'),
      page: z.number().optional().describe('list 时分页页码，从 0 开始，默认 0'),
      size: z.number().optional().describe('list 时每页条数，默认 20'),
    },
    async ({ action, userId, orderId, page, size }) => {
      try {
        if (action === 'get') {
          const order = await orderApi('GET', `/orders/${orderId}`)
          return ok(formatOrder(order))
        }
        if (action === 'list') {
          if (!userId) return err(new Error('未登录，请先登录'))
          const params = new URLSearchParams({ userId: String(userId) })
          if (page != null) params.set('page', String(page))
          if (size != null) params.set('size', String(size))
          const result = await orderApi('GET', `/orders?${params}`)
          if (!result.content || !result.content.length) return ok('暂无订单记录。')
          const lines = result.content.map(o => {
            const status = STATUS_LABELS[o.status] || o.status
            return `[${o.orderId}] ${status}  ${formatPrice(o.totalAmountCents)}  ${o.createdAt || ''}`
          })
          lines.push(`──────\n共 ${result.totalElements} 条订单`)
          return ok(lines.join('\n'))
        }
        if (action === 'cancel') {
          if (!orderId) return err(new Error('cancel 需要提供 orderId'))
          await orderApi('POST', `/orders/${orderId}/cancel`)
          return ok(`订单 ${orderId} 已成功取消。`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'order_create',
    '创建订单（userId 系统自动注入）。需提供 items（skuId + quantity 列表）和 shippingAddress（收件人、电话、省、市、区、详细地址六要素）。从购物车下单时，skuId 和 quantity 必须使用购物车返回的数据。',
    {
      userId: z.number().optional().describe('用户 ID（系统自动注入，无需手动传递）'),
      items: z.array(z.object({
        skuId: z.number().describe('SKU ID'),
        quantity: z.number().min(1).describe('数量'),
      })).min(1).describe('购买商品列表'),
      shippingAddress: z.object({
        recipientName: z.string().describe('收件人姓名'),
        phone: z.string().describe('联系电话'),
        province: z.string().describe('省份'),
        city: z.string().describe('城市'),
        district: z.string().describe('区/县'),
        detail: z.string().describe('详细地址'),
      }).describe('收货地址'),
    },
    async ({ userId, items, shippingAddress }) => {
      if (!userId) return err(new Error('未登录，请先登录'))
      try {
        const order = await orderApi('POST', '/orders', {
          userId,
          items,
          shippingAddress,
        })
        return ok(`下单成功！\n${formatOrder(order)}`)
      } catch (e) { return err(e) }
    }
  )
}
