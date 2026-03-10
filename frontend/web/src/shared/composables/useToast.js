import { ref } from 'vue'

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
    const id = ++toastId
    const item = { id, message, type }
    items.value = [...items.value, item]
    if (type === 'success') {
      const t = setTimeout(() => {
        remove(id)
      }, 2500)
      timers.set(id, t)
    }
    return id
  }

  return { showToast, items, remove }
}
