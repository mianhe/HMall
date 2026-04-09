<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">›</span>
      <span class="text-gray-800">我的</span>
    </nav>

    <div v-if="!isLoggedIn" class="bg-white rounded-lg border border-vmall-gray-border p-8 text-center">
      <p class="text-vmall-gray-text mb-4">登录后查看我的订单、收货地址等</p>
      <router-link
        to="/login"
        class="inline-block px-6 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover"
      >
        去登录
      </router-link>
    </div>

    <div v-else class="space-y-4">
      <UserProfileCard :username="username" @logout="handleLogout" />
      <MyOrdersCard />
      <MyCouponsCard />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '../shared/auth.js'
import UserProfileCard from '../shared/ui/organisms/UserProfileCard.vue'
import MyOrdersCard from '../shared/ui/organisms/MyOrdersCard.vue'
import MyCouponsCard from '../shared/ui/organisms/MyCouponsCard.vue'

const router = useRouter()
const { isLoggedIn, username, logout } = useAuth()

function handleLogout() {
  logout()
  router.push('/')
}
</script>
