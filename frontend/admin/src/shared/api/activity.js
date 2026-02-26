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

export async function getEventMetadata() {
  const { data } = await axios.get('/api/activities/event-metadata')
  return data
}
