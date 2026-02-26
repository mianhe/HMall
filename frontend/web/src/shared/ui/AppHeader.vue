<template>
  <header class="bg-vmall-red text-white">
    <div class="max-w-4xl mx-auto px-4 flex items-center justify-between h-14">
      <router-link to="/" class="text-lg font-bold hover:opacity-90 transition-opacity">
        HMall 商城
      </router-link>
      <div class="flex items-center gap-3">
        <template v-if="isLoggedIn">
          <router-link
            to="/cart"
            class="relative text-sm hover:underline flex items-center gap-1"
          >
            购物车
            <span v-if="cartCount > 0" class="min-w-[1.25rem] h-5 px-1.5 rounded-full bg-white/90 text-vmall-red text-xs font-medium flex items-center justify-center">
              {{ cartCount > 99 ? '99+' : cartCount }}
            </span>
          </router-link>
          <router-link
            to="/my"
            class="text-sm hover:underline"
          >
            我的
          </router-link>
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
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../auth.js'
import { getCart } from '../api/cart.js'

const router = useRouter()
const { isLoggedIn, username, logout } = useAuth()
const cartCount = ref(0)

async function fetchCartCount() {
  if (!isLoggedIn.value) {
    cartCount.value = 0
    return
  }
  try {
    const list = await getCart()
    cartCount.value = Array.isArray(list) ? list.length : 0
  } catch {
    cartCount.value = 0
  }
}

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) fetchCartCount()
  else cartCount.value = 0
}, { immediate: true })

function onCartUpdated() {
  fetchCartCount()
}

onMounted(() => {
  window.addEventListener('cart-updated', onCartUpdated)
})
onUnmounted(() => {
  window.removeEventListener('cart-updated', onCartUpdated)
})

function handleLogout() {
  logout()
  cartCount.value = 0
  router.push('/')
}
</script>
