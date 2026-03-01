/**
 * Activity 领域知识 Resource。
 */

export const ACTIVITY_DOMAIN_URI = 'hmall://activity/domain-knowledge'

export const ACTIVITY_DOMAIN_KNOWLEDGE = `## 运营数据（Activity）领域知识

### 统计维度

- activity_query stats 返回四大维度统计：订单概览（创建/取消/完成）、支付概览（尝试/成功/失败/过期/金额）、履约概览（创建/配货/发货/签收）、库存活动（占用/释放）。
- 时间范围支持快捷周期（today/last7/last30）和自定义日期范围（from + to，格式 YYYY-MM-DD）。默认查今日。

### 事件查询

- activity_query list 可按 orderId 筛选，查看某订单的完整事件时间线（按发生时间正序）。
- activity_query recent 查最近活动，跨所有订单，按时间倒序。

### 数据格式

- 金额字段单位为分，展示时转为元（如 paymentTotalCents 599900 → ¥5999.00）。`

export function registerActivityResources(server) {
  server.resource(
    'activity-domain-knowledge',
    ACTIVITY_DOMAIN_URI,
    { description: '运营数据领域知识：四大统计维度、时间范围、事件查询。', mimeType: 'text/plain' },
    async () => ({
      contents: [{ uri: ACTIVITY_DOMAIN_URI, mimeType: 'text/plain', text: ACTIVITY_DOMAIN_KNOWLEDGE }],
    }),
  )
}
