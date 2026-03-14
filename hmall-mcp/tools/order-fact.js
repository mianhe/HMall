/**
 * OrderFact 模块的 MCP tools：1 个 tool（order_fact_query）。
 * 基于 OrderFact/OrderItemFact 读模型的多维运营分析查询。
 */
import { z } from 'zod'

const ACTIVITY_API_BASE = process.env.HMALL_ACTIVITY_API_BASE || 'http://localhost:8086/api'

async function orderFactApi(method, path) {
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

function formatOrderFactStats(s) {
  const lines = []
  lines.push(`统计区间：${s.from || '—'} ~ ${s.to || '—'}`)
  lines.push('')
  lines.push('── 订单概览 ──')
  lines.push(`  总订单数：${s.totalOrders}`)
  lines.push(`  已完成：${s.completedOrders}  取消：${s.cancelledOrders}  进行中：${s.inProgressOrders}`)
  lines.push(`  取消原因 → 超时：${s.cancelByTimeout}  手动：${s.cancelByManual}`)
  lines.push('')
  lines.push('── 增值服务 ──')
  lines.push(`  含镭雕：${s.ordersWithEngraving}  含保障：${s.ordersWithWarranty}  任一VAS：${s.ordersWithAnyVas}`)
  lines.push(`  VAS渗透率：${s.totalOrders > 0 ? ((s.ordersWithAnyVas / s.totalOrders) * 100).toFixed(1) : 0}%`)
  lines.push(`  多件订单：${s.multiItemOrders}`)
  lines.push('')
  lines.push('── 金额与效率 ──')
  lines.push(`  总收入：${formatPrice(s.totalRevenueCents)}  平均客单价：${formatPrice(s.avgOrderAmountCents)}`)
  lines.push(`  VAS客单价：${formatPrice(s.avgVasOrderAmountCents)}  非VAS客单价：${formatPrice(s.avgNonVasOrderAmountCents)}`)
  lines.push(`  平均支付耗时：${s.avgPaymentDurationSec}秒  平均履约耗时：${s.avgFulfillmentDurationSec}秒`)
  lines.push('')
  lines.push('── 用户 ──')
  lines.push(`  独立买家：${s.distinctBuyerCount}  复购买家：${s.repeatBuyerCount}`)
  if (s.distinctBuyerCount > 0) {
    lines.push(`  复购率：${((s.repeatBuyerCount / s.distinctBuyerCount) * 100).toFixed(1)}%`)
  }
  return lines.join('\n')
}

export function registerOrderFactTools(server) {
  server.tool(
    'order_fact_query',
    '订单事实分析：以订单为中心的多维运营分析视图。stats 查聚合统计（VAS渗透率、客单价、取消原因、效率指标等）；stats_daily 查按日趋势；product_ranking 查商品销售排名；list 查订单列表（支持 VAS/阶段/用户筛选）。返回结果含 _raw 结构化数据。',
    {
      action: z.enum(['stats', 'stats_daily', 'product_ranking', 'list']).describe('stats|stats_daily|product_ranking|list'),
      hasEngraving: z.boolean().optional().describe('筛选含镭雕订单'),
      hasWarranty: z.boolean().optional().describe('筛选含保障服务订单'),
      currentStage: z.string().optional().describe('list 时可选，按阶段筛选：CREATED/PAID/FULFILLING/SHIPPED/DELIVERED/COMPLETED/CANCELLED'),
      spuId: z.number().optional().describe('list 时可选，按 SPU ID 筛选'),
      userId: z.number().optional().describe('list 时可选，按用户 ID 筛选'),
      rankBy: z.enum(['quantity', 'revenue', 'orderCount']).optional().describe('product_ranking 时可选，排名依据，默认 revenue'),
      groupBy: z.string().optional().describe('product_ranking 时可选，sku 按 SKU 分组，默认按 SPU'),
      period: z.string().optional().describe('快捷周期：today|last7|last30，默认 last7'),
      from: z.string().optional().describe('起始日期 YYYY-MM-DD，与 to 同时传入'),
      to: z.string().optional().describe('结束日期 YYYY-MM-DD，与 from 同时传入'),
      limit: z.number().optional().describe('list/product_ranking 时可选，返回条数'),
    },
    async ({ action, hasEngraving, hasWarranty, currentStage, spuId, userId, rankBy, groupBy, period, from, to, limit }) => {
      try {
        const dateParams = new URLSearchParams()
        if (from && to) {
          dateParams.set('from', from)
          dateParams.set('to', to)
        } else if (period) {
          dateParams.set('period', period)
        }

        if (action === 'stats') {
          const qs = dateParams.toString() ? `?${dateParams}` : ''
          const stats = await orderFactApi('GET', `/order-facts/stats${qs}`)
          const text = formatOrderFactStats(stats) +
            '\n\n若需渲染可视化，请将以下 JSON 作为 data 传入 ops_canvas：\n' +
            JSON.stringify(stats)
          return ok(text, { type: 'order_fact_stats', data: stats })
        }

        if (action === 'stats_daily') {
          const qs = dateParams.toString() ? `?${dateParams}` : ''
          const daily = await orderFactApi('GET', `/order-facts/stats/daily${qs}`)
          if (!daily.length) return ok('指定区间暂无订单事实数据。', { type: 'order_fact_stats_daily', items: [] })
          const lines = daily.map(d =>
            `${d.date}  订单:${d.totalOrders} 完成:${d.completedOrders} VAS:${d.vasOrders} 收入:${formatPrice(d.totalRevenueCents)}`
          )
          return ok(
            `按日订单统计（${daily.length} 天）：\n${lines.join('\n')}`,
            { type: 'order_fact_stats_daily', items: daily }
          )
        }

        if (action === 'product_ranking') {
          const params = new URLSearchParams(dateParams)
          if (rankBy) params.set('rankBy', rankBy)
          if (groupBy) params.set('groupBy', groupBy)
          if (hasEngraving != null) params.set('hasEngraving', String(hasEngraving))
          if (hasWarranty != null) params.set('hasWarranty', String(hasWarranty))
          if (limit != null) params.set('limit', String(limit))
          const qs = params.toString() ? `?${params}` : ''
          const ranking = await orderFactApi('GET', `/order-facts/product-ranking${qs}`)
          if (!ranking.items?.length) return ok('暂无商品排名数据。', { type: 'product_ranking', data: ranking })
          const unit = ranking.rankBy === 'quantity' ? '件' : ranking.rankBy === 'orderCount' ? '单' : ''
          const lines = ranking.items.map((item, i) => {
            const id = item.spuId != null ? `SPU#${item.spuId}` : `SKU#${item.skuId}`
            const val = ranking.rankBy === 'revenue' ? formatPrice(item.totalRevenueCents) :
              ranking.rankBy === 'quantity' ? `${item.totalQuantity}${unit}` : `${item.orderCount}${unit}`
            return `${i + 1}. ${id}  ${val}  订单:${item.orderCount}  取消:${item.cancelledOrderCount}`
          })
          return ok(
            `商品排名（按${ranking.rankBy}，${ranking.from}~${ranking.to}）：\n${lines.join('\n')}`,
            { type: 'product_ranking', data: ranking }
          )
        }

        if (action === 'list') {
          const params = new URLSearchParams(dateParams)
          if (hasEngraving != null) params.set('hasEngraving', String(hasEngraving))
          if (hasWarranty != null) params.set('hasWarranty', String(hasWarranty))
          if (currentStage) params.set('currentStage', currentStage)
          if (userId != null) params.set('userId', String(userId))
          if (limit != null) params.set('limit', String(limit))
          const qs = params.toString() ? `?${params}` : ''
          const facts = await orderFactApi('GET', `/order-facts${qs}`)
          if (!facts.length) return ok('暂无符合条件的订单事实。', { type: 'order_fact_list', items: [] })
          const lines = facts.map(f => {
            const vas = [f.hasEngraving && '镭雕', f.hasWarranty && '保障'].filter(Boolean).join('+') || '—'
            return `订单#${f.orderId} ${f.currentStage} ${formatPrice(f.totalAmountCents)} VAS:${vas}`
          })
          return ok(
            `订单事实列表（${facts.length} 条）：\n${lines.join('\n')}`,
            { type: 'order_fact_list', items: facts }
          )
        }

        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
