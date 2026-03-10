/**
 * AI Chat API — SSE streaming 调用 BFF /api/ai/chat
 */
import axios from 'axios'

const client = axios.create({
  baseURL: '/api/ai',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

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
  const doFetch = () =>
    fetch('/api/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal,
    })

  let response
  try {
    response = await doFetch()
  } catch (err) {
    if (err.name === 'AbortError') throw err
    await new Promise(r => setTimeout(r, 500))
    response = await doFetch()
  }

  if (!response.ok) {
    throw new Error(`AI chat request failed: ${response.status}`)
  }

  return response.body.pipeThrough(new TextDecoderStream()).getReader()
}
