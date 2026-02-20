/**
 * 支付限定上下文 API，与 docs/bounded-contexts/payment/api.yaml 一致
 */
import client from './client.js'

/**
 * 按 orderId 查询支付单（含 payUrl 用于跳转支付）
 * @param {number} orderId
 * @returns {Promise<{ paymentId: number, orderId: number, amountCents: number, status: string, payUrl?: string }>}
 */
export async function getPaymentByOrderId(orderId) {
  const { data } = await client.get(`/payments/by-order/${orderId}`)
  return data
}

/**
 * 创建支付单（Order 下单时应由后端调用；前端仅在「未找到支付单」时补救调用，幂等）
 * @param {number} orderId
 * @param {number} amountCents
 * @returns {Promise<{ paymentId: number, orderId: number, amountCents: number, status: string, payUrl?: string }>}
 */
export async function createPayment(orderId, amountCents) {
  const { data } = await client.post('/payments', { orderId, amountCents })
  return data
}
