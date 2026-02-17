/**
 * 订单限定上下文 API，与 docs/bounded-contexts/order/api.yaml 一致
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

/**
 * 创建订单（含占用库存等，可能较慢，单独延长超时）
 * @param {Object} body - { userId, items: [{ skuId, quantity }], shippingAddress: { recipientName, phone, province, city, district, detail } }
 */
export async function createOrder(body) {
  const { data } = await client.post('/orders', body, { timeout: 30000 })
  return data
}

/**
 * 按 ID 查询订单详情
 */
export async function getOrder(orderId) {
  const { data } = await client.get(`/orders/${orderId}`)
  return data
}

/**
 * 按 userId 查询订单列表（分页）
 * @param {number} userId
 * @param {number} page - 页码，从 0 开始
 * @param {number} size - 每页条数
 */
export async function getOrders(userId, page = 0, size = 20) {
  const { data } = await client.get('/orders', { params: { userId, page, size } })
  return data
}

/**
 * 取消订单
 */
export async function cancelOrder(orderId) {
  await client.post(`/orders/${orderId}/cancel`)
}
