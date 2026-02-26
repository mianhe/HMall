import axios from 'axios'

const client = axios.create({
  baseURL: '/api/ai/settings',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

export async function getSettings() {
  const { data } = await client.get('')
  return data
}

export async function updateSettings(payload) {
  const { data } = await client.put('', payload)
  return data
}

export async function resetSettings() {
  const { data } = await client.post('/reset')
  return data
}
