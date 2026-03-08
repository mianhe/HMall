/**
 * 库存 API，与 docs/bounded-contexts/inventory/api.yaml 一致。
 * 经 BFF 代理到 inventory-service。
 */
import client from './client.js'

/**
 * 批量查询库存
 * @param {number[]} skuIds - SKU ID 列表
 * @returns {Promise<Array<{ skuId: number, available: number, reserved: number }>>}
 */
export async function batchGetStock(skuIds) {
  const { data } = await client.post('/inventory/stock/batch', skuIds)
  return data
}
