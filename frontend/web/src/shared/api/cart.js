/**
 * 购物车 API，与 docs/bounded-contexts/cart/api.yaml 一致。
 * 请求头 X-User-Id 由 client 拦截器自动注入。
 */
import client from './client.js'

/**
 * 查询当前用户购物车
 * @returns {Promise<Array<{ cartItemId, skuId, quantity, addedAt, skuName?, skuPrice?, skuImageUrl?, available? }>>}
 */
export async function getCart() {
  const { data } = await client.get('/cart')
  return data ?? []
}

/**
 * 添加商品到购物车
 * @param {number} skuId
 * @param {number} quantity
 * @param {number | null} relatedSkuId
 */
export async function addCartItem(skuId, quantity, relatedSkuId = null) {
  const body = relatedSkuId == null ? { skuId, quantity } : { skuId, quantity, relatedSkuId }
  const { data } = await client.post('/cart/items', body)
  return data
}

/**
 * 修改购物车项数量
 * @param {number} cartItemId
 * @param {number} quantity - 0 表示删除该项
 */
export async function updateCartItem(cartItemId, quantity) {
  const { data } = await client.put(`/cart/items/${cartItemId}`, { quantity })
  return data
}

/**
 * 删除单个购物车项
 */
export async function deleteCartItem(cartItemId) {
  await client.delete(`/cart/items/${cartItemId}`)
}

/**
 * 批量删除购物车项
 * @param {number[]} cartItemIds
 */
export async function deleteCartItems(cartItemIds) {
  await client.delete('/cart/items', { data: { cartItemIds } })
}

/**
 * 结算预览：勾选项摘要与总价
 * @param {number[]} cartItemIds
 * @returns {Promise<{ items: Array<{ cartItemId, skuId, skuName, price, quantity, subtotal }>, totalPrice }>}
 */
export async function checkoutPreview(cartItemIds) {
  const { data } = await client.post('/cart/checkout-preview', { cartItemIds })
  return data
}
