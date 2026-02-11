/**
 * 商品限定上下文 API（展示用，仅 GET），与 docs/bounded-contexts/catalog/api.yaml 一致
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

export async function getCategories(parentId = null) {
  const params = parentId != null ? { parentId } : {}
  const { data } = await client.get('/categories', { params })
  return data
}

export async function getProducts(categoryId) {
  const { data } = await client.get('/products', { params: { categoryId } })
  return data
}

export async function getProduct(id) {
  const { data } = await client.get(`/products/${id}`)
  return data
}

export async function getDimensions(spuId) {
  const { data } = await client.get(`/products/${spuId}/dimensions`)
  return data
}

export async function getSkus(spuId) {
  const { data } = await client.get(`/products/${spuId}/skus`)
  return data
}
