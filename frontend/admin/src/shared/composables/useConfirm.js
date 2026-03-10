import { reactive } from 'vue'

export function useConfirm() {
  const state = reactive({
    visible: false,
    options: {
      title: '确认',
      message: '',
      confirmText: '确定',
      cancelText: '取消',
    },
  })
  let pendingResolve = null

  function confirm(opts = {}) {
    return new Promise((resolve) => {
      pendingResolve = resolve
      state.options = {
        title: opts.title ?? '确认',
        message: opts.message ?? '',
        confirmText: opts.confirmText ?? '确定',
        cancelText: opts.cancelText ?? '取消',
      }
      state.visible = true
    })
  }

  function onConfirm() {
    state.visible = false
    if (pendingResolve) {
      pendingResolve(true)
      pendingResolve = null
    }
  }

  function onCancel() {
    state.visible = false
    if (pendingResolve) {
      pendingResolve(false)
      pendingResolve = null
    }
  }

  return { state, confirm, onConfirm, onCancel }
}
