import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

export async function getPaymentSettings() {
  const { data } = await client.get('/payments/settings')
  return data
}

export async function updatePaymentSettings(settings) {
  const { data } = await client.put('/payments/settings', settings)
  return data
}
