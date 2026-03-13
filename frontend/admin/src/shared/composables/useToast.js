import { ref } from 'vue'

const MAX_TOASTS = 3
const SUCCESS_DURATION = 2500
const ERROR_DURATION = 5000

let toastId = 0

export function useToast() {
  const items = ref([])
  const timers = new Map()

  function remove(id) {
    items.value = items.value.filter((i) => i.id !== id)
    const t = timers.get(id)
    if (t) {
      clearTimeout(t)
      timers.delete(id)
    }
  }

  function showToast(message, type = 'success') {
    const msg = message != null ? String(message).trim() : ''
    if (!msg) return
    const id = ++toastId
    const item = { id, message: msg, type }
    const next = [...items.value, item].slice(-MAX_TOASTS)
    items.value.forEach((i) => {
      if (!next.some((n) => n.id === i.id)) {
        const t = timers.get(i.id)
        if (t) clearTimeout(t)
        timers.delete(i.id)
      }
    })
    items.value = next
    const duration = type === 'success' ? SUCCESS_DURATION : ERROR_DURATION
    const t = setTimeout(() => remove(id), duration)
    timers.set(id, t)
    return id
  }

  return { showToast, items, remove }
}
