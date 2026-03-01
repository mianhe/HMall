/**
 * Activity 模块的 MCP tools：1 个 tool（activity_query）。
 * 纯只读查询：活动列表、最近活动、统计仪表盘。
 */
import { z } from 'zod'

const ACTIVITY_API_BASE = process.env.HMALL_ACTIVITY_API_BASE || 'http://localhost:8086/api'

async function activityApi(method, path) {
  const url = `${ACTIVITY_API_BASE}${path}`
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
  })
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
  if (e.cause?.code === 'ECONNREFUSED' || e.message?.includes('ECONNREFUSED') || e.message?.includes('fetch failed')) {
    return { content: [{ type: 'text', text: `错误：无法连接后端服务（${ACTIVITY_API_BASE}），请确认服务已启动。原始错误：${e.message}` }] }
  }
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

function formatPrice(cents) {
  return `¥${(cents / 100).toFixed(2)}`
}

function formatActivity(a) {
  const order = a.orderId ? ` 订单:${a.orderId}` : ''
  return `[${a.id}] ${a.eventType}${order}  ${a.occurredAt || ''}`
}

function formatStats(s) {
  const lines = []
  lines.push(`统计区间：${s.from || '—'} ~ ${s.to || '—'}`)
  lines.push('')
  lines.push('── 订单概览 ──')
  lines.push(`  已开出订单：${s.ordersCreated ?? 0}`)
  lines.push(`  已取消订单：${s.ordersCancelled ?? 0}`)
  lines.push(`  已完成订单：${s.ordersCompleted ?? 0}`)
  lines.push('')
  lines.push('── 支付概览 ──')
  lines.push(`  支付尝试总数：${s.paymentAttempts ?? 0}`)
  lines.push(`  成功支付：${s.paymentSuccess ?? 0}`)
  lines.push(`  支付失败：${s.paymentFailed ?? 0}`)
  lines.push(`  支付过期：${s.paymentExpired ?? 0}`)
  lines.push(`  成功支付总额：${formatPrice(s.paymentTotalCents ?? 0)}`)
  lines.push('')
  lines.push('── 履约概览 ──')
  lines.push(`  履约单创建：${s.fulfillmentCreated ?? 0}`)
  lines.push(`  开始配货：${s.fulfillmentAllocated ?? 0}`)
  lines.push(`  已发货：${s.fulfillmentShipped ?? 0}`)
  lines.push(`  已签收：${s.fulfillmentDelivered ?? 0}`)
  lines.push('')
  lines.push('── 库存活动 ──')
  lines.push(`  库存占用：${s.stockReserved ?? 0}`)
  lines.push(`  库存释放：${s.stockReleased ?? 0}`)
  return lines.join('\n')
}

export function registerActivityTools(server) {
  server.tool(
    'activity_query',
    '业务活动查询与统计（面向管理后台仪表盘）。action=list 按条件查活动列表（可选 orderId 筛选）；recent 查最近活动；stats 查统计指标（订单、支付、履约、库存概览，支持 period 快捷周期或 from/to 自定义日期范围）。',
    {
      action: z.enum(['list', 'recent', 'stats']).describe('list|recent|stats'),
      orderId: z.number().optional().describe('list 时可选，按订单 ID 筛选'),
      limit: z.number().optional().describe('list/recent 时可选，返回条数，默认 20'),
      period: z.string().optional().describe('stats 时可选，快捷周期：today|last7|last30，默认 today'),
      from: z.string().optional().describe('stats 时可选，起始日期 YYYY-MM-DD，与 to 同时传入'),
      to: z.string().optional().describe('stats 时可选，结束日期 YYYY-MM-DD，与 from 同时传入'),
    },
    async ({ action, orderId, limit, period, from, to }) => {
      try {
        if (action === 'list') {
          const params = new URLSearchParams()
          if (orderId != null) params.set('orderId', String(orderId))
          if (limit != null) params.set('limit', String(limit))
          const qs = params.toString() ? `?${params}` : ''
          const activities = await activityApi('GET', `/activities${qs}`)
          if (!activities.length) return ok('暂无活动记录。')
          const lines = activities.map(formatActivity)
          return ok(lines.join('\n'))
        }
        if (action === 'recent') {
          const qs = limit != null ? `?limit=${limit}` : ''
          const activities = await activityApi('GET', `/activities/recent${qs}`)
          if (!activities.length) return ok('暂无最近活动。')
          const lines = activities.map(formatActivity)
          return ok(`最近活动（${activities.length} 条）：\n${lines.join('\n')}`)
        }
        if (action === 'stats') {
          const params = new URLSearchParams()
          if (from && to) {
            params.set('from', from)
            params.set('to', to)
          } else if (period) {
            params.set('period', period)
          }
          const qs = params.toString() ? `?${params}` : ''
          const stats = await activityApi('GET', `/activities/stats${qs}`)
          return ok(formatStats(stats))
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
