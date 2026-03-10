/**
 * 商品限定上下文 API，与 docs/bounded-contexts/catalog/api.yaml 一致
 * 开发环境经 Vite 代理到 BFF（见 vite.config.js），生产环境使用 /api。
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ---------- 类目（Category） ----------

export async function getCategories(parentId = null) {
  const params = parentId != null ? { parentId } : {}
  const { data } = await client.get('/categories', { params })
  return data
}

export async function getCategoryTree() {
  const { data } = await client.get('/categories/tree')
  return data
}

export async function createCategory(body) {
  const { data } = await client.post('/categories', body)
  return data
}

export async function updateCategory(id, body) {
  const { data } = await client.put(`/categories/${id}`, body)
  return data
}

export async function deleteCategory(id) {
  await client.delete(`/categories/${id}`)
}

// ---------- 商品、维度、SKU（读） ----------

export async function getProducts(categoryId, { include } = {}) {
  const params = { categoryId }
  if (include) params.include = include
  const { data } = await client.get('/products', { params })
  return data
}

export async function getProduct(id) {
  const { data } = await client.get(`/products/${id}`)
  return data
}

export async function searchProducts(keyword = '') {
  const params = keyword ? { keyword } : {}
  const { data } = await client.get('/products/search', { params })
  return data
}

export async function getDimensions(spuId) {
  const { data } = await client.get(`/products/${spuId}/dimensions`)
  return data
}

export async function createDimension(spuId, body) {
  const { data } = await client.post(`/products/${spuId}/dimensions`, body)
  return data
}

export async function createOption(spuId, dimensionId, body) {
  const { data } = await client.post(
    `/products/${spuId}/dimensions/${dimensionId}/options`,
    body
  )
  return data
}

export async function deleteOption(spuId, dimensionId, optionId) {
  await client.delete(
    `/products/${spuId}/dimensions/${dimensionId}/options/${optionId}`
  )
}

export async function getSkus(spuId) {
  const { data } = await client.get(`/products/${spuId}/skus`)
  return data
}

export async function createSku(spuId, body) {
  const { data } = await client.post(`/products/${spuId}/skus`, body)
  return data
}

export async function updateSku(spuId, skuId, body) {
  const { data } = await client.put(`/products/${spuId}/skus/${skuId}`, body)
  return data
}

export async function deleteSku(spuId, skuId) {
  await client.delete(`/products/${spuId}/skus/${skuId}`)
}

// ---------- 文件上传（返回 { url }） ----------

export async function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await client.post('/files/upload', formData, {
    headers: { 'Content-Type': undefined }, // 不设 Content-Type，由 axios 为 FormData 自动加 multipart boundary
  })
  return data
}

// ---------- 产品级展示图 ----------

export async function getProductImages(spuId) {
  const { data } = await client.get(`/products/${spuId}/images`)
  return data
}

export async function addProductImage(spuId, body) {
  const { data } = await client.post(`/products/${spuId}/images`, body)
  return data
}

export async function deleteProductImage(spuId, imageId) {
  await client.delete(`/products/${spuId}/images/${imageId}`)
}

// ---------- 服务绑定 ----------

export async function getAvailableServices(spuId) {
  const { data } = await client.get(`/products/${spuId}/available-services`)
  return data
}

export async function getServiceBindings(skuId) {
  const { data } = await client.get(`/skus/${skuId}/service-bindings`)
  return data
}

export async function createServiceBinding(skuId, body) {
  const { data } = await client.post(`/skus/${skuId}/service-bindings`, body)
  return data
}

export async function deleteServiceBinding(skuId, bindingId) {
  await client.delete(`/skus/${skuId}/service-bindings/${bindingId}`)
}

// ---------- 选项级展示图 ----------

export async function addOptionImage(spuId, dimensionId, optionId, body) {
  const { data } = await client.post(
    `/products/${spuId}/dimensions/${dimensionId}/options/${optionId}/images`,
    body
  )
  return data
}

export async function deleteOptionImage(spuId, dimensionId, optionId, imageId) {
  await client.delete(
    `/products/${spuId}/dimensions/${dimensionId}/options/${optionId}/images/${imageId}`
  )
}

// ---------- 商品（SPU）写操作 ----------

export async function createProduct(body) {
  const { data } = await client.post('/products', body)
  return data
}

export async function updateProduct(id, body) {
  const { data } = await client.put(`/products/${id}`, body)
  return data
}

export async function deleteProduct(id) {
  await client.delete(`/products/${id}`)
}

// ---------- 镭雕图案库（EngravingPattern） ----------

export async function getEngravingPatterns(enabled = null) {
  const params = enabled != null ? { enabled } : {}
  const { data } = await client.get('/engraving-patterns', { params })
  return data
}

export async function getEngravingPattern(id) {
  const { data } = await client.get(`/engraving-patterns/${id}`)
  return data
}

export async function createEngravingPattern(body) {
  const { data } = await client.post('/engraving-patterns', body)
  return data
}

export async function updateEngravingPattern(id, body) {
  const { data } = await client.put(`/engraving-patterns/${id}`, body)
  return data
}

export async function deleteEngravingPattern(id) {
  await client.delete(`/engraving-patterns/${id}`)
}
