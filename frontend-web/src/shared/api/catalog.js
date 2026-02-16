/**
 * 商品限定上下文 API，与 docs/bounded-contexts/catalog/api.yaml 一致
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

/** 根类目不传 parentId，子类目传 parentId */
export async function getCategories(parentId = null) {
  const params = parentId != null ? { parentId } : {}
  const { data } = await client.get('/categories', { params })
  return data
}

/** 按类目查商品列表 */
export async function getProducts(categoryId) {
  const { data } = await client.get('/products', { params: { categoryId } })
  return data
}

/** 按 ID 查商品详情 */
export async function getProduct(id) {
  const { data } = await client.get(`/products/${id}`)
  return data
}

/** 产品级展示图（用于商品卡片主图、详情图廊） */
export async function getProductImages(spuId) {
  const { data } = await client.get(`/products/${spuId}/images`)
  return data
}

/** 某 SPU 下所有规格维度及选项（用于详情页选规格） */
export async function getDimensions(spuId) {
  const { data } = await client.get(`/products/${spuId}/dimensions`)
  return data
}

/** 某 SPU 下所有 SKU（价格、规格展示值） */
export async function getSkus(spuId) {
  const { data } = await client.get(`/products/${spuId}/skus`)
  return data
}
