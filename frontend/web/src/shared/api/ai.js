/**
 * AI Chat API — SSE streaming via /api/ai/chat
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api/ai',
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

export async function getModels() {
  const { data } = await client.get('/models')
  return data.models
}

/**
 * Sends a chat request and returns a ReadableStream reader for SSE parsing.
 * @param {{ messages: Array, context: object, provider?: string }} payload
 * @param {AbortSignal} [signal]
 * @returns {Promise<ReadableStreamDefaultReader>}
 */
export async function streamChat(payload, signal) {
  const headers = { 'Content-Type': 'application/json' }
  const userId = getUserIdFromToken()
  if (userId != null) {
    headers['X-User-Id'] = String(userId)
  }

  const response = await fetch('/api/ai/chat', {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
    signal,
  })

  if (!response.ok) {
    throw new Error(`AI chat request failed: ${response.status}`)
  }

  return response.body.pipeThrough(new TextDecoderStream()).getReader()
}
