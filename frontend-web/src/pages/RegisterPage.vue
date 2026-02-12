<template>
  <div class="max-w-md mx-auto px-4 py-16">
    <h1 class="text-2xl font-bold text-gray-800 mb-6">注册</h1>
    <form @submit.prevent="handleRegister" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
        <input
          v-model="username"
          type="text"
          class="w-full px-4 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-transparent"
          placeholder="请输入用户名"
        />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">密码</label>
        <input
          v-model="password"
          type="password"
          class="w-full px-4 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-transparent"
          placeholder="请输入密码"
        />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">确认密码</label>
        <input
          v-model="confirmPassword"
          type="password"
          class="w-full px-4 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-transparent"
          placeholder="请再次输入密码"
        />
      </div>
      <p v-if="errorMsg" class="text-red-600 text-sm">{{ errorMsg }}</p>
      <button
        type="submit"
        :disabled="loading"
        class="w-full py-3 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors disabled:opacity-50"
      >
        {{ loading ? '注册中...' : '注册' }}
      </button>
    </form>
    <p class="mt-4 text-sm text-vmall-gray-text">
      已有账号？
      <router-link to="/login" class="text-vmall-red hover:underline">去登录</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, login } from '../shared/api/user.js'
import { useAuth } from '../shared/auth.js'

const router = useRouter()
const { setToken } = useAuth()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')
const loading = ref(false)

async function handleRegister() {
  errorMsg.value = ''

  if (password.value !== confirmPassword.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    await register(username.value, password.value)
    const { token } = await login(username.value, password.value)
    setToken(token)
    router.push('/')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
