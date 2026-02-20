import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

function getUserIdFromToken() {
  try {
    const token = localStorage.getItem('token')
    if (!token) return null
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.userId ?? null
  } catch {
    return null
  }
}

client.interceptors.request.use((config) => {
  if (config.url?.startsWith('/cart') || config.url?.startsWith('cart')) {
    const userId = getUserIdFromToken()
    if (userId != null) {
      config.headers['X-User-Id'] = String(userId)
    }
  }
  return config
})

export default client
