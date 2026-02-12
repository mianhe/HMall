/**
 * 响应式认证状态管理。
 * 从 JWT token 中解析用户名，提供 setToken / logout / isLoggedIn / username。
 */
import { reactive, computed } from 'vue'

function parseToken(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return { username: payload.sub, userId: payload.userId }
  } catch {
    return null
  }
}

const state = reactive({
  token: localStorage.getItem('token') || null,
  username: null,
  userId: null,
})

// 初始化：如果 localStorage 里已有 token，解析一下
if (state.token) {
  const info = parseToken(state.token)
  if (info) {
    state.username = info.username
    state.userId = info.userId
  } else {
    state.token = null
    localStorage.removeItem('token')
  }
}

export function useAuth() {
  const isLoggedIn = computed(() => !!state.token)
  const username = computed(() => state.username)

  function setToken(token) {
    const info = parseToken(token)
    if (!info) return
    state.token = token
    state.username = info.username
    state.userId = info.userId
    localStorage.setItem('token', token)
  }

  function logout() {
    state.token = null
    state.username = null
    state.userId = null
    localStorage.removeItem('token')
  }

  return { isLoggedIn, username, setToken, logout }
}
