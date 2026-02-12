<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <p v-if="isLoggedIn" class="text-gray-700">欢迎回来，<span class="font-semibold text-vmall-red">{{ username }}</span>！</p>
    <p v-else class="text-vmall-gray-text">消费者端，欢迎光临。请先 <router-link to="/login" class="text-vmall-red hover:underline">登录</router-link> 或 <router-link to="/register" class="text-vmall-red hover:underline">注册</router-link>。</p>
  </div>
</template>

<script setup>
import { useAuth } from '../shared/auth.js'

const { isLoggedIn, username } = useAuth()
</script>
