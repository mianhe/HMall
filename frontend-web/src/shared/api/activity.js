import client from './client.js'

/**
 * 按 orderId 查询该订单关联的业务活动（occurredAt 正序）
 */
export async function getActivitiesByOrderId(orderId) {
  const { data } = await client.get('/activities', { params: { orderId } })
  return data
}
