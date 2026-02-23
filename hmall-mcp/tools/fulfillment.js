/**
 * Fulfillment 模块的 MCP tools：1 个 tool（fulfillment_orders）。
 * 暴露查询与操作推进（get/list/allocate/ship/deliver），
 * 不暴露 create/cancel（系统协调 API，由 Order BC 在 Saga 中调用）。
 */
import { z } from 'zod'

const FULFILLMENT_API_BASE = process.env.HMALL_FULFILLMENT_API_BASE || 'http://localhost:8088/api'

async function fulfillmentApi(method, path, body) {
  const url = `${FULFILLMENT_API_BASE}${path}`
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

const STATUS_LABELS = {
  CREATED: '待配货',
  ALLOCATING: '配货中',
  SHIPPED: '已发货',
  DELIVERED: '已签收',
  CANCELLED: '已取消',
}

function formatFulfillmentOrder(order) {
  const status = STATUS_LABELS[order.status] || order.status
  const lines = [
    `履约单号：${order.fulfillmentOrderId}  订单号：${order.orderId}  状态：${status}`,
  ]
  if (order.items && order.items.length) {
    lines.push('商品明细：')
    for (const item of order.items) {
      lines.push(`  - SKU ${item.skuId} × ${item.quantity}`)
    }
  }
  if (order.shippingAddress) {
    const addr = order.shippingAddress
    lines.push(`收货地址：${addr.recipientName} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detail}`)
  }
  if (order.shippingInfo) {
    const info = order.shippingInfo
    const parts = [`物流：${info.carrier || ''} ${info.trackingNumber || ''}`]
    if (info.shippedAt) parts.push(`发货时间：${info.shippedAt}`)
    if (info.deliveredAt) parts.push(`签收时间：${info.deliveredAt}`)
    lines.push(parts.join('  '))
  }
  if (order.createdAt) {
    lines.push(`创建时间：${order.createdAt}`)
  }
  return lines.join('\n')
}

export function registerFulfillmentTools(server) {
  server.tool(
    'fulfillment_orders',
    '履约单查询与操作推进。action=get(fulfillmentOrderId) 查详情；list 按 orderId 或 status 筛选列表；allocate(fulfillmentOrderId) 配货；ship(fulfillmentOrderId,carrier,trackingNumber) 发货；deliver(fulfillmentOrderId) 签收确认。不支持创建和取消（由订单流程自动触发）。',
    {
      action: z.enum(['get', 'list', 'allocate', 'ship', 'deliver']).describe('get|list|allocate|ship|deliver'),
      fulfillmentOrderId: z.number().optional().describe('get/allocate/ship/deliver 时必填，履约单 ID'),
      orderId: z.number().optional().describe('list 时可选，按订单 ID 筛选'),
      status: z.enum(['CREATED', 'ALLOCATING', 'SHIPPED', 'DELIVERED', 'CANCELLED']).optional().describe('list 时可选，按状态筛选'),
      carrier: z.string().optional().describe('ship 时必填，物流公司名称'),
      trackingNumber: z.string().optional().describe('ship 时必填，物流单号'),
    },
    async ({ action, fulfillmentOrderId, orderId, status, carrier, trackingNumber }) => {
      try {
        if (action === 'get') {
          if (!fulfillmentOrderId) return err(new Error('请提供 fulfillmentOrderId'))
          const order = await fulfillmentApi('GET', `/fulfillment/${fulfillmentOrderId}`)
          return ok(formatFulfillmentOrder(order))
        }

        if (action === 'list') {
          const params = new URLSearchParams()
          if (orderId != null) params.set('orderId', String(orderId))
          if (status) params.set('status', status)
          const qs = params.toString()
          const orders = await fulfillmentApi('GET', `/fulfillment${qs ? '?' + qs : ''}`)
          if (!orders.length) return ok('暂无履约单记录。')
          const lines = orders.map(o => {
            const s = STATUS_LABELS[o.status] || o.status
            return `[${o.fulfillmentOrderId}] 订单 ${o.orderId}  ${s}  ${o.createdAt || ''}`
          })
          lines.push(`──────\n共 ${orders.length} 条履约单`)
          return ok(lines.join('\n'))
        }

        if (action === 'allocate') {
          if (!fulfillmentOrderId) return err(new Error('请提供 fulfillmentOrderId'))
          await fulfillmentApi('POST', `/fulfillment/${fulfillmentOrderId}/allocate`)
          return ok(`履约单 ${fulfillmentOrderId} 已配货。`)
        }

        if (action === 'ship') {
          if (!fulfillmentOrderId) return err(new Error('请提供 fulfillmentOrderId'))
          if (!carrier) return err(new Error('请提供 carrier（物流公司）'))
          if (!trackingNumber) return err(new Error('请提供 trackingNumber（物流单号）'))
          await fulfillmentApi('POST', `/fulfillment/${fulfillmentOrderId}/ship`, { carrier, trackingNumber })
          return ok(`履约单 ${fulfillmentOrderId} 已发货。物流：${carrier} ${trackingNumber}`)
        }

        if (action === 'deliver') {
          if (!fulfillmentOrderId) return err(new Error('请提供 fulfillmentOrderId'))
          await fulfillmentApi('POST', `/fulfillment/${fulfillmentOrderId}/deliver`)
          return ok(`履约单 ${fulfillmentOrderId} 已签收。`)
        }

        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
