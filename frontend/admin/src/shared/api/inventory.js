/**
 * 库存限定上下文 API，与 docs/bounded-contexts/inventory/api.yaml 一致
 * 开发环境经 Vite 代理到 BFF，BFF 再代理到 inventory-service。
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

/**
 * 查询单个 SKU 库存
 * @param {number} skuId - SKU ID
 * @returns {Promise<{ skuId: number, available: number, reserved: number }>}
 */
export async function getStock(skuId) {
  const { data } = await client.get(`/inventory/stock/${skuId}`)
  return data
}

/**
 * 批量查询库存（仅返回已有记录的 SKU）
 * @param {number[]} skuIds - SKU ID 列表
 * @returns {Promise<Array<{ skuId: number, available: number, reserved: number }>>}
 */
export async function batchGetStock(skuIds) {
  const { data } = await client.post('/inventory/stock/batch', skuIds)
  return data
}

/**
 * 初始化或更新可用库存
 * @param {number} skuId - SKU ID
 * @param {number} available - 可用数量（≥0）
 * @returns {Promise<{ skuId: number, available: number, reserved: number }>}
 */
export async function setStock(skuId, available) {
  const { data } = await client.put(`/inventory/stock/${skuId}`, { available })
  return data
}
