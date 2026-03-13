import { ref, computed } from 'vue'

export function useOpsCanvas() {
  const panels = ref([])
  const phase = ref('EMPTY')
  const cachedStats = ref(null)
  let turnActive = false

  const state = computed(() => {
    if (phase.value === 'EMPTY') return { type: 'EMPTY' }
    if (phase.value === 'LOADING' && panels.value.length === 0) {
      return { type: 'EMPTY', loading: true }
    }
    return { type: 'MULTI_PANEL', panels: panels.value, loading: phase.value === 'LOADING' }
  })

  function onToolCall(toolName) {
    if (toolName === 'activity_query' || toolName === 'ops_canvas') {
      if (!turnActive) {
        turnActive = true
        panels.value = []
      }
      phase.value = 'LOADING'
    }
  }

  function parseResult(result) {
    if (typeof result === 'string') {
      try { return JSON.parse(result) } catch { return {} }
    }
    return result ?? {}
  }

  function onToolResult(toolName, result) {
    const parsed = parseResult(result)

    if (toolName === 'activity_query') {
      const raw = parsed._raw
      if (raw?.type === 'activity_stats' && raw.data) {
        cachedStats.value = raw.data
      }
      return
    }

    if (toolName !== 'ops_canvas') return

    const raw = parsed._raw
    if (!raw || raw.type !== 'canvas_command') {
      return
    }

    panels.value = [...panels.value, {
      type: raw.view.toUpperCase(),
      title: raw.title,
      data: raw.data,
    }]
    phase.value = 'READY'
  }

  function onTurnEnd() {
    turnActive = false
  }

  function reset() {
    panels.value = []
    phase.value = 'EMPTY'
    cachedStats.value = null
    turnActive = false
  }

  return { state, panels, phase, onToolCall, onToolResult, onTurnEnd, reset }
}
