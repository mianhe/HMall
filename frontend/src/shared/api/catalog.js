/**
 * 商品限定上下文 API，与 backend/docs/catalog/catalog-api.yaml 一致
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// 类别
export async function getCategories(parentId = null) {
  const params = parentId != null ? { parentId } : {}
  const { data } = await client.get('/categories', { params })
  return data
}

export async function createCategory(body) {
  const { data } = await client.post('/categories', body)
  return data
}

// 商品
export async function getProducts(categoryId) {
  const { data } = await client.get('/products', { params: { categoryId } })
  return data
}

export async function getProduct(id) {
  const { data } = await client.get(`/products/${id}`)
  return data
}

export async function createProduct(body) {
  const { data } = await client.post('/products', body)
  return data
}

// 规格维度与选项（SPU 配置）
export async function getDimensions(spuId) {
  const { data } = await client.get(`/products/${spuId}/dimensions`)
  return data
}

export async function createDimension(spuId, body) {
  const { data } = await client.post(`/products/${spuId}/dimensions`, body)
  return data
}

export async function createOption(spuId, dimensionId, body) {
  const { data } = await client.post(`/products/${spuId}/dimensions/${dimensionId}/options`, body)
  return data
}

// SKU
export async function getSkus(spuId) {
  const { data } = await client.get(`/products/${spuId}/skus`)
  return data
}

export async function createSku(spuId, body) {
  const { data } = await client.post(`/products/${spuId}/skus`, body)
  return data
}
