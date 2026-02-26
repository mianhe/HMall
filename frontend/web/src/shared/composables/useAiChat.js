import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { streamChat, getModels } from '../api/ai.js'

let idCounter = 0
function genId() {
  return `msg_${Date.now()}_${++idCounter}`
}

/**
 * Consumer-facing AI Chat composable.
 * Always uses auto-match mode (no manual skill selection).
 */
export function useAiChat() {
  const messages = ref([])
  const isStreaming = ref(false)
  const lastMatchedSkills = ref([])
  const route = useRoute()

  const models = ref([])
  const selectedProvider = ref(null)
  let abortController = null

  async function loadModels() {
    try {
      models.value = await getModels()
      const defaultModel = models.value.find(m => m.default)
      if (defaultModel && !selectedProvider.value) {
        selectedProvider.value = defaultModel.id
      }
    } catch (e) {
      console.warn('Failed to load AI models:', e)
    }
  }

  async function sendMessage(content) {
    if (!content.trim() || isStreaming.value) return

    messages.value.push({
      id: genId(),
      role: 'user',
      content: content.trim(),
      toolCalls: [],
      loading: false,
    })

    messages.value.push({
      id: genId(),
      role: 'assistant',
      content: '',
      toolCalls: [],
      loading: true,
    })
    const assistantMsg = messages.value[messages.value.length - 1]

    isStreaming.value = true
    abortController = new AbortController()

    try {
      const chatMessages = messages.value
        .filter(m => m.role === 'user' || (m.role === 'assistant' && !m.loading))
        .map(m => ({ role: m.role, content: m.content }))

      const payload = {
        messages: chatMessages,
        context: { page: route.path },
        provider: selectedProvider.value,
        clientType: 'consumer',
      }
      lastMatchedSkills.value = []
      const reader = await streamChat(payload, abortController.signal)

      await parseSseStream(reader, assistantMsg)
    } catch (e) {
      if (e.name !== 'AbortError') {
        assistantMsg.content += '\n\n[连接失败: ' + e.message + ']'
      }
    } finally {
      assistantMsg.loading = false
      isStreaming.value = false
      abortController = null
    }
  }

  function stopStreaming() {
    if (abortController) {
      abortController.abort()
    }
  }

  async function parseSseStream(reader, assistantMsg) {
    let buffer = ''

    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += value
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        let currentEvent = null

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const dataStr = line.slice(5).trim()
            if (!dataStr) continue
            try {
              const data = JSON.parse(dataStr)
              handleSseEvent(currentEvent || data.type, data, assistantMsg)
            } catch {
              // skip unparseable lines
            }
          }
        }
      }
    } finally {
      try { reader.cancel() } catch {}
    }
  }

  function handleSseEvent(eventType, data, assistantMsg) {
    switch (eventType) {
      case 'delta':
        assistantMsg.content += data.content || ''
        break

      case 'tool_call':
        assistantMsg.toolCalls.push({
          id: data.id,
          name: data.name,
          arguments: data.arguments,
          result: null,
          status: 'calling',
        })
        break

      case 'tool_result': {
        const tc = assistantMsg.toolCalls.find(t => t.id === data.id)
        if (tc) {
          tc.result = data.result
          tc.status = 'success'
        }
        break
      }

      case 'skill_matched':
        try {
          lastMatchedSkills.value = JSON.parse(data.content)
        } catch {
          lastMatchedSkills.value = []
        }
        break

      case 'error':
        assistantMsg.content += '\n\n[错误: ' + (data.message || '未知错误') + ']'
        break

      case 'done':
        assistantMsg.loading = false
        break
    }
  }

  function clearMessages() {
    stopStreaming()
    messages.value = []
  }

  return {
    messages,
    isStreaming,
    lastMatchedSkills,
    models,
    selectedProvider,
    loadModels,
    sendMessage,
    stopStreaming,
    clearMessages,
  }
}
