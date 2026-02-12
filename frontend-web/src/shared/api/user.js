/**
 * 用户限定上下文 API，与 docs/bounded-contexts/user/api.yaml 一致
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

export async function register(username, password) {
  const { data } = await client.post('/users', { username, password })
  return data
}

export async function login(username, password) {
  const { data } = await client.post('/login', { username, password })
  return data
}
