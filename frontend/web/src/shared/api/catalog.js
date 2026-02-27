/**
 * 商品限定上下文 API，与 docs/bounded-contexts/catalog/api.yaml 一致
 */
import client from './client.js'

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

/** 产品级展示图列表（管理后台用；消费者端列表/详情使用 product.coverImageUrl / product.defaultDisplayImages） */
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

/** 某实体商品的可选服务列表 */
export async function getAvailableServices(spuId) {
  const { data } = await client.get(`/products/${spuId}/available-services`)
  return data
}

/** 某规格选项的展示图列表 */
export async function getOptionImages(spuId, dimensionId, optionId) {
  const { data } = await client.get(
    `/products/${spuId}/dimensions/${dimensionId}/options/${optionId}/images`
  )
  return data
}
