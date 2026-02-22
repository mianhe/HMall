import axios from 'axios'

const client = axios.create({
  baseURL: '/api/ai/skills',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

export async function getSkills() {
  const { data } = await client.get('')
  return data
}

export async function getSkill(id) {
  const { data } = await client.get(`/${id}`)
  return data
}

export async function createSkill(payload) {
  const { data } = await client.post('', payload)
  return data
}

export async function updateSkill(id, payload) {
  const { data } = await client.put(`/${id}`, payload)
  return data
}

export async function deleteSkill(id) {
  await client.delete(`/${id}`)
}

export async function setDefaultSkill(id) {
  const { data } = await client.put(`/${id}/default`)
  return data
}
