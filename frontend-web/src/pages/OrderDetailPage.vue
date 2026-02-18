<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <router-link to="/orders" class="hover:text-vmall-red">我的订单</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">订单 {{ orderId }}</span>
    </nav>

    <div v-if="loading && !order" class="text-vmall-gray-text py-12">加载中…</div>
    <div v-else-if="error" class="text-vmall-red py-4">{{ error }}</div>

    <template v-else-if="order">
      <!-- 订单状态 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-4 mb-6">
        <div class="flex justify-between items-center">
          <p class="text-gray-800">
            订单号：<span class="font-mono">{{ order.orderId }}</span>
          </p>
          <span class="px-3 py-1 rounded text-sm font-medium" :class="statusClass">
            {{ statusText }}
          </span>
        </div>
      </div>

      <!-- 商品明细 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-4 mb-6">
        <h2 class="text-lg font-medium text-gray-800 mb-3">商品明细</h2>
        <div v-for="item in order.items" :key="item.lineItemId ?? item.skuId" class="flex gap-4 py-3 border-b border-vmall-gray-border last:border-0">
          <div class="w-16 h-16 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center text-xl text-vmall-gray-text">📦</div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-gray-800">{{ item.displayName || '商品' }}</p>
            <p class="text-sm text-vmall-gray-text">¥ {{ (item.unitPriceCents || 0) / 100 }} × {{ item.quantity }}</p>
          </div>
          <p class="text-vmall-red font-medium shrink-0">¥ {{ (item.totalPriceCents || item.unitPriceCents * item.quantity || 0) / 100 }}</p>
        </div>
      </div>

      <!-- 收货地址 -->
      <div v-if="order.shippingAddress" class="bg-white rounded-lg border border-vmall-gray-border p-4 mb-6">
        <h2 class="text-lg font-medium text-gray-800 mb-3">收货地址</h2>
        <p class="text-gray-700">{{ order.shippingAddress.recipientName }} {{ order.shippingAddress.phone }}</p>
        <p class="text-vmall-gray-text text-sm">{{ fullAddress }}</p>
      </div>

      <!-- 合计 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-4 mb-6">
        <p class="text-right text-gray-800">
          合计：<span class="text-xl font-bold text-vmall-red">¥ {{ (order.totalAmountCents || 0) / 100 }}</span>
        </p>
      </div>

      <!-- 操作 -->
      <div class="flex gap-3">
        <template v-if="order.status === 'PENDING_PAYMENT'">
          <button
            :disabled="payLoading"
            @click="handlePay"
            class="px-6 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-60 transition-colors"
          >
            {{ payLoading ? '跳转中…' : '去支付' }}
          </button>
          <button
            :disabled="cancelling"
            @click="handleCancel"
            class="px-6 py-2 rounded-lg border border-vmall-gray-border text-gray-700 hover:bg-gray-50 disabled:opacity-60 transition-colors"
          >
            {{ cancelling ? '取消中…' : '取消订单' }}
          </button>
        </template>
        <router-link to="/orders" class="px-6 py-2 rounded-lg border border-vmall-gray-border text-gray-700 hover:bg-gray-50 transition-colors inline-block">
          返回订单列表
        </router-link>
      </div>

      <p v-if="actionError" class="mt-4 text-vmall-red text-sm">{{ actionError }}</p>
      <p v-if="toast" class="mt-4 text-green-600 text-sm">{{ toast }}</p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder, cancelOrder } from '../shared/api/order.js'
import { getPaymentByOrderId, createPayment } from '../shared/api/payment.js'
import { useAuth } from '../shared/auth.js'

const route = useRoute()
const router = useRouter()
const { userId, isLoggedIn } = useAuth()

const orderId = computed(() => Number(route.params.id))
const order = ref(null)
const loading = ref(true)
const error = ref('')
const cancelling = ref(false)
const payLoading = ref(false)
const actionError = ref('')
const toast = ref('')

const statusMap = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  FULFILLING: '履约中',
  SHIPPED: '已发货',
  DELIVERED: '已送达',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const statusText = computed(() => statusMap[order.value?.status] ?? order.value?.status ?? '')

const statusClass = computed(() => {
  const s = order.value?.status
  if (s === 'PENDING_PAYMENT') return 'bg-amber-100 text-amber-800'
  if (s === 'CANCELLED') return 'bg-gray-100 text-gray-600'
  if (['PAID', 'FULFILLING', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(s)) return 'bg-green-100 text-green-800'
  return 'bg-vmall-gray-bg text-vmall-gray-text'
})

const fullAddress = computed(() => {
  const addr = order.value?.shippingAddress
  if (!addr) return ''
  return [addr.province, addr.city, addr.district, addr.detail].filter(Boolean).join(' ')
})

async function load() {
  if (!orderId.value) return
  loading.value = true
  error.value = ''
  try {
    order.value = await getOrder(orderId.value)
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  actionError.value = ''
  cancelling.value = true
  try {
    await cancelOrder(orderId.value)
    await load()
    toast.value = '订单已取消'
    setTimeout(() => { toast.value = '' }, 2000)
  } catch (e) {
    actionError.value = e.response?.data?.message || e.message || '取消失败'
  } finally {
    cancelling.value = false
  }
}

async function handlePay() {
  actionError.value = ''
  toast.value = ''
  payLoading.value = true
  try {
    let payment
    try {
      payment = await getPaymentByOrderId(orderId.value)
    } catch (e) {
      if (e.response?.status === 404 && order.value?.totalAmountCents != null) {
        // 下单时未创建支付单（如当时未配置 payment 服务），补救：按订单金额创建支付单（幂等）
        payment = await createPayment(orderId.value, order.value.totalAmountCents)
      } else {
        throw e
      }
    }
    const url = payment?.payUrl
    if (url) {
      const returnUrl = window.location.origin + '/orders/' + orderId.value
      const sep = url.includes('?') ? '&' : '?'
      window.location.href = url + sep + 'returnUrl=' + encodeURIComponent(returnUrl)
      return
    }
    actionError.value = '支付链接暂不可用，请稍后重试'
  } catch (e) {
    if (e.response?.status === 404) {
      actionError.value = '未找到支付单，请刷新页面或联系客服'
    } else {
      actionError.value = e.response?.data?.message || e.message || '获取支付链接失败'
    }
  } finally {
    payLoading.value = false
  }
}

onMounted(() => {
  if (!isLoggedIn.value || !userId.value) {
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  load()
})
watch(() => route.params.id, load)
</script>
