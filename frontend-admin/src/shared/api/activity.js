import axios from 'axios'

export async function getActivityStats(params = {}) {
  const { data } = await axios.get('/api/activities/stats', { params })
  return data
}

export async function getRecentActivities(limit = 20) {
  const { data } = await axios.get('/api/activities/recent', { params: { limit } })
  return data
}
