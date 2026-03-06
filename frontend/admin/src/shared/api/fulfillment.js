/**
 * 履约限界上下文 API，与 docs/bounded-contexts/fulfillment/event-flow.md 契约一致。
 * 开发环境经 Vite 代理到 BFF，BFF 再代理到 fulfillment-service。
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

/**
 * 列表查询（管理端）
 * @param {{ orderId?: number, status?: string }} params - 可选：orderId、状态（CREATED/ALLOCATING/SHIPPED/DELIVERED/CANCELLED）
 * @returns {Promise<Array<FulfillmentOrderDto>>}
 */
export async function listFulfillmentOrders(params = {}) {
  const { data } = await client.get('/fulfillment', { params })
  return data
}

/**
 * 按 ID 查询履约单
 * @param {number} fulfillmentOrderId
 * @returns {Promise<FulfillmentOrderDto>}
 */
export async function getFulfillmentOrder(fulfillmentOrderId) {
  const { data } = await client.get(`/fulfillment/${fulfillmentOrderId}`)
  return data
}

/**
 * 开始配货（CREATED → ALLOCATING）
 * @param {number} fulfillmentOrderId
 */
export async function allocateFulfillmentOrder(fulfillmentOrderId) {
  await client.post(`/fulfillment/${fulfillmentOrderId}/allocate`)
}

/**
 * 完成镭雕（有 engravingInfo 且未完成时可调用）
 * @param {number} fulfillmentOrderId
 */
export async function completeEngravingFulfillmentOrder(fulfillmentOrderId) {
  await client.post(`/fulfillment/${fulfillmentOrderId}/complete-engraving`)
}

/**
 * 发货（ALLOCATING → SHIPPED）
 * @param {number} fulfillmentOrderId
 * @param {{ carrier: string, trackingNumber: string }} body
 */
export async function shipFulfillmentOrder(fulfillmentOrderId, body) {
  await client.post(`/fulfillment/${fulfillmentOrderId}/ship`, body)
}

/**
 * 签收确认（SHIPPED → DELIVERED）
 * @param {number} fulfillmentOrderId
 */
export async function deliverFulfillmentOrder(fulfillmentOrderId) {
  await client.post(`/fulfillment/${fulfillmentOrderId}/deliver`)
}
