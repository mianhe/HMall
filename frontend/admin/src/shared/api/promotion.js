import axios from 'axios'

const client = axios.create({
  baseURL: '/api/promotion',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

export async function getCouponTemplates(page = 0, size = 20) {
  const { data } = await client.get('/coupon-templates', { params: { page, size } })
  return data
}

export async function getCouponTemplate(id) {
  const { data } = await client.get(`/coupon-templates/${id}`)
  return data
}

export async function createCouponTemplate(payload) {
  const { data } = await client.post('/coupon-templates', payload)
  return data
}

export async function deactivateCouponTemplate(id) {
  const { data } = await client.post(`/coupon-templates/${id}/deactivate`)
  return data
}

export async function issueCoupons(templateId, userId, quantity) {
  const { data } = await client.post(`/coupon-templates/${templateId}/issue`, { userId, quantity })
  return data
}

export async function getPromotionActivities(page = 0, size = 20) {
  const { data } = await client.get('/activities', { params: { page, size } })
  return data
}

export async function createPromotionActivity(payload) {
  const { data } = await client.post('/activities', payload)
  return data
}

export async function activatePromotionActivity(id) {
  const { data } = await client.post(`/activities/${id}/activate`)
  return data
}

export async function deactivatePromotionActivity(id) {
  const { data } = await client.post(`/activities/${id}/deactivate`)
  return data
}
