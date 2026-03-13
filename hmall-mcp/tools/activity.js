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

function ok(text, raw) {
  const result = { content: [{ type: 'text', text }] }
  if (raw) result._raw = raw
  return result
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
  lines.push(`  下单用户数（去重）：${s.distinctBuyerCount ?? 0}`)
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
    '业务活动查询与统计（面向管理后台仪表盘）。action=list 按条件查活动列表（可选 orderId/userId/skuId/spuId 筛选）；recent 查最近活动；stats 查聚合统计指标；stats_daily 查按日统计（需要 from/to 日期范围，返回每天的统计数据，适合画趋势图）。返回结果含 _raw 结构化数据。',
    {
      action: z.enum(['list', 'recent', 'stats', 'stats_daily']).describe('list|recent|stats|stats_daily'),
      orderId: z.number().optional().describe('list 时可选，按订单 ID 筛选'),
      userId: z.number().optional().describe('list 时可选，按用户 ID 筛选'),
      skuId: z.number().optional().describe('list 时可选，按 SKU ID 筛选'),
      spuId: z.number().optional().describe('list 时可选，按 SPU ID 筛选'),
      limit: z.number().optional().describe('list/recent 时可选，返回条数，默认 20'),
      period: z.string().optional().describe('stats/stats_daily 时可选，快捷周期：today|last7|last30'),
      from: z.string().optional().describe('stats/stats_daily 时可选，起始日期 YYYY-MM-DD，与 to 同时传入'),
      to: z.string().optional().describe('stats/stats_daily 时可选，结束日期 YYYY-MM-DD，与 from 同时传入'),
    },
    async ({ action, orderId, userId, skuId, spuId, limit, period, from, to }) => {
      try {
        if (action === 'list') {
          const params = new URLSearchParams()
          if (orderId != null) params.set('orderId', String(orderId))
          if (userId != null) params.set('userId', String(userId))
          if (skuId != null) params.set('skuId', String(skuId))
          if (spuId != null) params.set('spuId', String(spuId))
          if (limit != null) params.set('limit', String(limit))
          const qs = params.toString() ? `?${params}` : ''
          const activities = await activityApi('GET', `/activities${qs}`)
          if (!activities.length) return ok('暂无活动记录。', { type: 'activity_list', items: [] })
          const lines = activities.map(formatActivity)
          const dimension = userId != null ? `用户 #${userId}` : orderId != null ? `订单 #${orderId}` : skuId != null ? `SKU #${skuId}` : spuId != null ? `SPU #${spuId}` : '全部'
          return ok(
            `${dimension} 的活动（${activities.length} 条）：\n${lines.join('\n')}`,
            { type: 'activity_list', items: activities, dimension: { orderId, userId, skuId, spuId } }
          )
        }
        if (action === 'recent') {
          const qs = limit != null ? `?limit=${limit}` : ''
          const activities = await activityApi('GET', `/activities/recent${qs}`)
          if (!activities.length) return ok('暂无最近活动。', { type: 'activity_list', items: [] })
          const lines = activities.map(formatActivity)
          return ok(
            `最近活动（${activities.length} 条）：\n${lines.join('\n')}`,
            { type: 'activity_list', items: activities, dimension: {} }
          )
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
          const text = formatStats(stats) +
            '\n\n若需渲染 stat_cards，请将以下 JSON 作为 data 传入 ops_canvas：\n' +
            JSON.stringify(stats)
          return ok(text, { type: 'activity_stats', data: stats })
        }
        if (action === 'stats_daily') {
          const params = new URLSearchParams()
          if (from && to) {
            params.set('from', from)
            params.set('to', to)
          } else if (period) {
            params.set('period', period)
          }
          const qs = params.toString() ? `?${params}` : ''
          const dailyStats = await activityApi('GET', `/activities/stats/daily${qs}`)
          if (!dailyStats.length) return ok('指定区间暂无数据。', { type: 'activity_stats_daily', items: [] })
          const lines = dailyStats.map(d =>
            `${d.date}  订单:${d.ordersCreated} 支付:${d.paymentSuccess} 金额:${formatPrice(d.paymentTotalCents ?? 0)} 发货:${d.fulfillmentShipped}`
          )
          return ok(
            `按日统计（${dailyStats.length} 天）：\n${lines.join('\n')}`,
            { type: 'activity_stats_daily', items: dailyStats }
          )
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
