<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">我的订单</span>
    </nav>

    <h1 class="text-xl font-bold text-gray-800 mb-6">{{ statusFilterTitle }}订单</h1>

    <div v-if="loading && !orders?.length" class="text-vmall-gray-text py-12">加载中…</div>
    <div v-else-if="listError" class="text-vmall-red py-4">{{ listError }}</div>
    <div v-else-if="!filteredOrders.length" class="text-vmall-gray-text py-12 text-center">
      <p>{{ statusFilterTitle ? '暂无该状态订单' : '暂无订单' }}</p>
      <router-link to="/" class="mt-4 inline-block text-vmall-red hover:underline">去逛逛</router-link>
    </div>

    <div v-else class="space-y-4">
      <router-link
        v-for="o in filteredOrders"
        :key="o.orderId"
        :to="`/orders/${o.orderId}`"
        class="block bg-white rounded-lg border border-vmall-gray-border p-4 hover:border-vmall-red/50 transition-colors"
      >
        <div class="flex justify-between items-start mb-2">
          <span class="font-mono text-sm text-vmall-gray-text">订单 {{ o.orderId }}</span>
          <span class="px-2 py-0.5 rounded text-xs font-medium" :class="statusClass(o.status)">
            {{ statusMap[o.status] ?? o.status }}
          </span>
        </div>
        <div class="text-gray-800">
          <p v-for="item in (o.items || []).slice(0, 2)" :key="item.lineItemId ?? item.skuId" class="text-sm truncate">
            {{ item.displayName || '商品' }} × {{ item.quantity }}
          </p>
        </div>
        <p class="text-right mt-2 text-vmall-red font-medium">¥ {{ (o.totalAmountCents || 0) / 100 }}</p>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrders } from '../shared/api/order.js'
import { useAuth } from '../shared/auth.js'

const route = useRoute()
const router = useRouter()
const { userId, isLoggedIn } = useAuth()

const orders = ref([])
const loading = ref(true)
const listError = ref('')

const statusFilter = computed(() => route.query.status || '')

const statusFilterTitle = computed(() => {
  const s = statusFilter.value
  if (s === 'PENDING_PAYMENT') return '待付款'
  if (s === 'PAID') return '待收货'
  if (s === 'DELIVERED') return '待评价'
  return '我的'
})

const filteredOrders = computed(() => {
  const list = orders.value || []
  const s = statusFilter.value
  if (!s) return list
  if (s === 'PENDING_PAYMENT') return list.filter((o) => o.status === 'PENDING_PAYMENT')
  if (s === 'PAID') return list.filter((o) => ['PAID', 'FULFILLING', 'SHIPPED'].includes(o.status))
  if (s === 'DELIVERED') return list.filter((o) => ['DELIVERED', 'COMPLETED'].includes(o.status))
  return list
})

const statusMap = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  FULFILLING: '履约中',
  SHIPPED: '已发货',
  DELIVERED: '已送达',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

function statusClass(s) {
  if (s === 'PENDING_PAYMENT') return 'bg-amber-100 text-amber-800'
  if (s === 'CANCELLED') return 'bg-gray-100 text-gray-600'
  if (['PAID', 'FULFILLING', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(s)) return 'bg-green-100 text-green-800'
  return 'bg-vmall-gray-bg text-vmall-gray-text'
}

async function load() {
  if (!userId.value) return
  loading.value = true
  listError.value = ''
  try {
    const page = await getOrders(userId.value, 0, 20)
    orders.value = page?.content ?? []
  } catch (e) {
    listError.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!isLoggedIn.value || !userId.value) {
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  load()
})
watch(() => route.query.status, () => { /* filteredOrders 依赖 orders + query，无需重载 */ })
</script>
