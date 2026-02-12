<template>
  <div class="max-w-md mx-auto px-4 py-16">
    <h1 class="text-2xl font-bold text-gray-800 mb-6">登录</h1>
    <form @submit.prevent="handleLogin" class="space-y-4">
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
      <p v-if="errorMsg" class="text-red-600 text-sm">{{ errorMsg }}</p>
      <button
        type="submit"
        class="w-full py-3 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors"
      >
        登录
      </button>
    </form>
    <p class="mt-4 text-sm text-vmall-gray-text">
      没有账号？
      <router-link to="/register" class="text-vmall-red hover:underline">去注册</router-link>
    </p>
    <router-link to="/" class="block mt-2 text-sm text-vmall-gray-text hover:text-vmall-red">返回首页</router-link>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../shared/api/user.js'
import { useAuth } from '../shared/auth.js'

const router = useRouter()
const { setToken } = useAuth()
const username = ref('')
const password = ref('')
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  try {
    const { token } = await login(username.value, password.value)
    setToken(token)
    router.push('/')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '登录失败'
  }
}
</script>
