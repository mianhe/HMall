import client from './client.js'

function getUserId() {
  try {
    const token = localStorage.getItem('token')
    if (!token) return null
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.userId ?? null
  } catch {
    return null
  }
}

export async function getClaimableTemplates() {
  const userId = getUserId()
  if (!userId) throw new Error('请先登录')
  const { data } = await client.get('/promotion/coupon-templates/claimable', {
    params: { userId },
  })
  return data
}

export async function claimCoupon(templateId) {
  const userId = getUserId()
  if (!userId) throw new Error('请先登录')
  const { data } = await client.post(
    `/promotion/coupon-templates/${templateId}/claim`,
    null,
    { params: { userId } },
  )
  return data
}

export async function getMyCoupons(status) {
  const userId = getUserId()
  if (!userId) throw new Error('请先登录')
  const params = { userId }
  if (status) params.status = status
  const { data } = await client.get('/promotion/coupons/my', { params })
  return data
}

export async function getAvailableCoupons(orderAmountCents) {
  const userId = getUserId()
  if (!userId) throw new Error('请先登录')
  const { data } = await client.get('/promotion/coupons/available', {
    params: { userId, orderAmountCents },
  })
  return data
}

export async function calculatePrice(items, couponId) {
  const userId = getUserId()
  if (!userId) throw new Error('请先登录')
  const payload = {
    items,
    userId,
  }
  if (couponId != null) payload.couponId = couponId
  const { data } = await client.post('/promotion/calculate-price', payload)
  return data
}

export async function previewSkuPrices(items) {
  const payload = { items }
  const userId = getUserId()
  if (userId) payload.userId = userId
  const { data } = await client.post('/promotion/preview-sku-prices', payload)
  return data?.items || []
}
