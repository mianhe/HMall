import axios from 'axios'

export async function getActivityStats(params = {}) {
  const { data } = await axios.get('/api/activities/stats', { params })
  return data
}

export async function getRecentActivities(limit = 20) {
  const { data } = await axios.get('/api/activities/recent', { params: { limit } })
  return data
}

export async function getActivitiesByOrderId(orderId) {
  const { data } = await axios.get('/api/activities', { params: { orderId } })
  return data
}

/**
 * 多维查询活动（orderId / userId / skuId / spuId 任一或组合，智能运营 Step 1）
 * @param {Object} params - { orderId?, userId?, skuId?, spuId?, limit? }
 */
export async function getActivities(params = {}) {
  const q = { limit: params.limit ?? 20 }
  if (params.orderId != null) q.orderId = params.orderId
  if (params.userId != null) q.userId = params.userId
  if (params.skuId != null) q.skuId = params.skuId
  if (params.spuId != null) q.spuId = params.spuId
  const { data } = await axios.get('/api/activities', { params: q })
  return data
}

export async function getEventMetadata() {
  const { data } = await axios.get('/api/activities/event-metadata')
  return data
}

export async function seedActivities({ days = 30, ordersPerDay = 5 } = {}) {
  const { data } = await axios.post('/api/activities/seed', null, {
    params: { days, ordersPerDay },
  })
  return data
}
