import axios from 'axios'

const client = axios.create({
  baseURL: '/api/users',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

export async function getUserById(userId) {
  const { data } = await client.get(`/${userId}`)
  return data
}

export async function getUserSegments(userId) {
  const { data } = await client.get(`/${userId}/segments`)
  return data
}

export async function updateUserLevel(userId, level) {
  const { data } = await client.put(`/${userId}/level`, { level })
  return data
}

export async function updateUserTags(userId, tags) {
  const { data } = await client.put(`/${userId}/tags`, { tags })
  return data
}

export async function createSegmentRule(payload) {
  const { data } = await client.post('/segment-rules', payload)
  return data
}

export async function listSegmentRules() {
  const { data } = await client.get('/segment-rules')
  return data
}

export async function previewSegmentRule(ruleId, sampleSize = 20) {
  const { data } = await client.post(`/segment-rules/${ruleId}/preview`, { sampleSize })
  return data
}

export async function activateSegmentRule(ruleId) {
  const { data } = await client.post(`/segment-rules/${ruleId}/activate`)
  return data
}
