<template>
  <header class="bg-vmall-red text-white">
    <div class="max-w-4xl mx-auto px-4 flex items-center justify-between h-14">
      <router-link to="/" class="text-lg font-bold hover:opacity-90 transition-opacity">
        HMall 商城
      </router-link>
      <div class="flex items-center gap-3">
        <template v-if="isLoggedIn">
          <span class="text-sm">{{ username }}</span>
          <button
            @click="handleLogout"
            class="px-3 py-1.5 text-sm rounded border border-white/50 hover:bg-white/10 transition-colors"
          >
            退出
          </button>
        </template>
        <template v-else>
          <router-link
            to="/login"
            class="px-3 py-1.5 text-sm rounded bg-white text-vmall-red font-medium hover:bg-gray-100 transition-colors"
          >
            登录
          </router-link>
          <router-link
            to="/register"
            class="px-3 py-1.5 text-sm rounded border border-white/50 hover:bg-white/10 transition-colors"
          >
            注册
          </router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '../auth.js'

const router = useRouter()
const { isLoggedIn, username, logout } = useAuth()

function handleLogout() {
  logout()
  router.push('/')
}
</script>
