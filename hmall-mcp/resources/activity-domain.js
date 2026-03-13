/**
 * Activity 领域知识 Resource。
 * 基础查询知识，供非运营场景（如"运营数据助手" Skill）使用。
 * 完整的运营分析知识见 hmall://intelligent-ops/domain-knowledge。
 */

export const ACTIVITY_DOMAIN_URI = 'hmall://activity/domain-knowledge'

export const ACTIVITY_DOMAIN_KNOWLEDGE = `## 运营数据（Activity）领域知识

### 统计维度

- activity_query stats 返回四大维度统计：订单概览（创建/取消/完成）、支付概览（尝试/成功/失败/过期/金额）、履约概览（创建/配货/发货/签收）、库存活动（占用/释放）。
- activity_query stats_daily 返回按日拆分的统计数据，适合画趋势图。
- 时间范围支持快捷周期（today/last7/last30）和自定义日期范围（from + to，格式 YYYY-MM-DD）。默认查今日。

### 事件查询

- activity_query list 可按 orderId/userId/skuId/spuId 筛选，查看事件时间线（按发生时间正序）。
- activity_query recent 查最近活动，跨所有订单，按时间倒序。

### 数据格式

- 金额字段 paymentTotalCents 单位为分，展示时转为元（如 599900 → ¥5999.00）。`

export function registerActivityResources(server) {
  server.resource(
    'activity-domain-knowledge',
    ACTIVITY_DOMAIN_URI,
    { description: '运营数据领域知识：统计维度、时间范围、事件查询。', mimeType: 'text/plain' },
    async () => ({
      contents: [{ uri: ACTIVITY_DOMAIN_URI, mimeType: 'text/plain', text: ACTIVITY_DOMAIN_KNOWLEDGE }],
    }),
  )
}
