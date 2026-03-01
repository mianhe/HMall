async function hasServiceBindings(request, productId) {
  const resp = await request.get(`/api/products/${productId}/available-services`, {
    failOnStatusCode: false,
  })
  if (!resp.ok()) return false
  const data = await resp.json()
  if (!Array.isArray(data)) return false
  return data.some(
    (svc) => svc.productType === 'SERVICE' && Array.isArray(svc.bindings) && svc.bindings.length > 0
  )
}

async function findProductWithServiceBindings(request, preferredId) {
  if (preferredId && await hasServiceBindings(request, preferredId)) {
    return preferredId
  }
  for (let id = 1; id <= 50; id++) {
    if (await hasServiceBindings(request, id)) {
      return id
    }
  }
  return null
}

module.exports = { findProductWithServiceBindings }
