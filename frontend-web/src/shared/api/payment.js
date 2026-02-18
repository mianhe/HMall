/**
 * 支付限定上下文 API，与 docs/bounded-contexts/payment/api.yaml 一致
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

/**
 * 按 orderId 查询支付单（含 payUrl 用于跳转支付）
 * @param {number} orderId
 * @returns {Promise<{ paymentId: number, orderId: number, amountCents: number, status: string, payUrl?: string }>}
 */
export async function getPaymentByOrderId(orderId) {
  const { data } = await client.get(`/payments/by-order/${orderId}`)
  return data
}
