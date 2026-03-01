<template>
  <div class="max-w-4xl mx-auto px-4 py-6">
    <!-- 面包屑 -->
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">&gt;</span>
      <router-link to="/orders" class="hover:text-vmall-red">我的订单</router-link>
      <span class="mx-1">&gt;</span>
      <span class="text-gray-800">订单详情</span>
    </nav>

    <div v-if="loading && !order" class="text-vmall-gray-text py-12 text-center">加载中…</div>
    <div v-else-if="error" class="text-vmall-red py-4">{{ error }}</div>

    <template v-else-if="order">
      <!-- 顶部：订单号 + 操作按钮 -->
      <div class="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h1 class="text-2xl font-bold text-gray-900">
          订单号：<span class="font-mono">{{ order.orderId }}</span>
        </h1>
        <div class="flex gap-3">
          <template v-if="order.status === 'PENDING_PAYMENT'">
            <button
              @click="handleCancel"
              :disabled="cancelling"
              class="px-5 py-2 rounded border border-vmall-gray-border text-gray-700 hover:bg-gray-50 disabled:opacity-60 transition-colors"
            >{{ cancelling ? '取消中…' : '取消订单' }}</button>
            <button
              @click="handlePay"
              :disabled="payLoading"
              class="px-5 py-2 rounded bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-60 transition-colors font-medium"
            >{{ payLoading ? '跳转中…' : '立即支付' }}</button>
          </template>
        </div>
      </div>

      <!-- 支付倒计时 -->
      <div
        v-if="order.status === 'PENDING_PAYMENT' && countdown"
        class="bg-amber-50 border border-amber-200 rounded-lg px-5 py-3 mb-6 flex items-center gap-2"
      >
        <span class="text-amber-600 text-lg">⚠</span>
        <span class="text-amber-800 font-medium">
          请您在 <span class="text-vmall-red font-bold tabular-nums">{{ countdown }}</span> 内完成支付。
        </span>
      </div>

      <!-- 订单旅程进度条 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-6 mb-6">
        <div class="flex items-start justify-between relative">
          <div
            v-for="(step, idx) in journeySteps"
            :key="step.key"
            class="flex flex-col items-center flex-1 relative z-10"
          >
            <!-- 连接线 -->
            <div v-if="idx > 0" class="absolute top-3 right-1/2 w-full h-0.5" :class="step.reached ? 'bg-vmall-red' : 'bg-gray-200'" />
            <!-- 圆点 -->
            <div
              class="w-6 h-6 rounded-full border-2 flex items-center justify-center relative z-10"
              :class="step.reached
                ? 'bg-vmall-red border-vmall-red'
                : 'bg-white border-gray-300'"
            >
              <div v-if="step.reached" class="w-2 h-2 rounded-full bg-white" />
            </div>
            <!-- 步骤名 -->
            <p class="mt-2 text-xs font-medium" :class="step.reached ? 'text-gray-900' : 'text-gray-400'">
              {{ step.label }}
            </p>
            <!-- 时间 -->
            <p v-if="step.time" class="text-xs text-vmall-gray-text mt-0.5 tabular-nums">
              {{ formatShortTime(step.time) }}
            </p>
          </div>
        </div>
      </div>

      <!-- 订单处理信息（事件时间线） -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-6 mb-6">
        <h2 class="text-base font-bold text-vmall-red mb-4">订单处理信息</h2>
        <div v-if="timeline.length === 0" class="text-vmall-gray-text text-sm py-4 text-center">暂无处理记录</div>
        <div v-else class="relative pl-6">
          <div class="absolute left-2.5 top-1 bottom-1 w-px bg-gray-200" />
          <div
            v-for="(ev, idx) in timeline"
            :key="ev.eventId"
            class="relative pb-5 last:pb-0"
          >
            <div
              class="absolute -left-3.5 top-0.5 w-3 h-3 rounded-full border-2"
              :class="idx === 0 ? 'bg-vmall-red border-vmall-red' : 'bg-white border-gray-300'"
            />
            <div class="flex items-baseline gap-4">
              <span class="text-sm text-vmall-red font-medium tabular-nums whitespace-nowrap">
                {{ formatFullTime(ev.occurredAt) }}
              </span>
              <span class="text-sm" :class="idx === 0 ? 'text-vmall-red font-medium' : 'text-gray-700'">
                {{ ev.description }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品明细 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-6 mb-6">
        <h2 class="text-base font-bold text-gray-900 mb-4">商品明细</h2>
        <div
          v-for="item in order.items"
          :key="item.lineItemId ?? item.skuId"
          class="flex gap-4 py-3 border-b border-vmall-gray-border last:border-0"
          :class="item.itemType === 'SERVICE' ? 'pl-8 bg-gray-50/50' : ''"
        >
          <div v-if="item.itemType === 'SERVICE'" class="w-12 h-12 shrink-0 bg-blue-50 rounded flex items-center justify-center text-blue-500">🛡</div>
          <div v-else class="w-16 h-16 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center text-xl text-vmall-gray-text">📦</div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span v-if="item.itemType === 'SERVICE'" class="text-xs px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 font-medium">服务</span>
              <p class="font-medium text-gray-800">{{ item.displayName || '商品' }}</p>
            </div>
            <p class="text-sm text-vmall-gray-text">
              ¥{{ formatPrice(item.unitPriceCents) }} × {{ item.quantity }}
            </p>
            <p v-if="item.itemType === 'SERVICE'" class="text-xs mt-0.5" :class="order.serviceActivated ? 'text-green-600' : 'text-amber-500'">
              {{ order.serviceActivated ? '已激活' : '待激活' }}
            </p>
          </div>
          <p class="text-vmall-red font-medium shrink-0">
            ¥{{ formatPrice(item.totalPriceCents || item.unitPriceCents * item.quantity) }}
          </p>
        </div>
        <div class="pt-4 text-right">
          合计：<span class="text-xl font-bold text-vmall-red">¥{{ formatPrice(order.totalAmountCents) }}</span>
        </div>
      </div>

      <!-- 收货信息 -->
      <div v-if="order.shippingAddress" class="bg-white rounded-lg border border-vmall-gray-border p-6 mb-6">
        <h2 class="text-base font-bold text-gray-900 mb-4">收货信息</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <h3 class="text-sm font-medium text-gray-500 mb-2">基本信息</h3>
            <p class="text-gray-800">姓名：{{ order.shippingAddress.recipientName }}</p>
            <p class="text-gray-800 text-sm mt-1">收货地址：{{ fullAddress }}</p>
            <p class="text-gray-800 text-sm mt-1">联系电话：{{ order.shippingAddress.phone }}</p>
          </div>
          <div>
            <h3 class="text-sm font-medium text-gray-500 mb-2">发票信息</h3>
            <p class="text-gray-800 text-sm">发票类型：数电普通发票</p>
            <p class="text-gray-800 text-sm mt-1">发票抬头：个人</p>
          </div>
          <div>
            <h3 class="text-sm font-medium text-gray-500 mb-2">配送信息</h3>
            <p class="text-gray-800 text-sm">配送方式：标准配送</p>
          </div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="flex gap-3">
        <router-link
          to="/orders"
          class="px-6 py-2 rounded border border-vmall-gray-border text-gray-700 hover:bg-gray-50 transition-colors inline-block"
        >
          返回订单列表
        </router-link>
      </div>

      <p v-if="actionError" class="mt-4 text-vmall-red text-sm">{{ actionError }}</p>
      <p v-if="toast" class="mt-4 text-green-600 text-sm">{{ toast }}</p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder, cancelOrder } from '../shared/api/order.js'
import { getPaymentByOrderId, createPayment } from '../shared/api/payment.js'
import { getActivitiesByOrderId } from '../shared/api/activity.js'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => Number(route.params.id))
const order = ref(null)
const activities = ref([])
const loading = ref(true)
const error = ref('')
const cancelling = ref(false)
const payLoading = ref(false)
const actionError = ref('')
const toast = ref('')
const countdown = ref('')
const paymentExpiredAt = ref(null)
let countdownTimer = null

const fullAddress = computed(() => {
  const addr = order.value?.shippingAddress
  if (!addr) return ''
  return [addr.province, addr.city, addr.district, addr.detail].filter(Boolean).join('')
})

const JOURNEY_STEPS = [
  { key: 'submitted', label: '提交订单', eventTypes: ['OrderCreated'] },
  { key: 'paid',      label: '付款成功', eventTypes: ['PaymentCompleted'] },
  { key: 'fulfilling', label: '正在配货', eventTypes: ['FulfillmentOrderAllocated'] },
  { key: 'shipped',   label: '等待收货', eventTypes: ['FulfillmentShipped'] },
  { key: 'completed', label: '已完成',   eventTypes: ['OrderCompleted'] },
]

const STATUS_STEP_INDEX = {
  PENDING_PAYMENT: 0,
  PAID: 1,
  FULFILLING: 2,
  SHIPPED: 3,
  DELIVERED: 4,
  COMPLETED: 4,
}

const journeySteps = computed(() => {
  const status = order.value?.status
  if (status === 'CANCELLED') {
    return JOURNEY_STEPS.map((s, idx) => ({
      ...s,
      reached: idx === 0,
      time: idx === 0 ? findEventTime(['OrderCreated']) : null,
    }))
  }

  const currentIdx = STATUS_STEP_INDEX[status] ?? 0
  return JOURNEY_STEPS.map((s, idx) => ({
    ...s,
    reached: idx <= currentIdx,
    time: findEventTime(s.eventTypes),
  }))
})

const EVENT_DESCRIPTIONS = {
  OrderCreated:               '您提交了订单，请等待系统确认',
  StockReserved:              '商品已确认，库存已锁定',
  PaymentCompleted:           '支付成功',
  PaymentFailed:              '支付失败，请重新尝试支付',
  PaymentExpired:             '支付超时，订单将自动取消',
  OrderCancelled:             '订单已取消',
  OrderCompleted:             '订单已完成',
  StockReleased:              '库存已释放',
  FulfillmentOrderCreated:    '履约单已创建，等待配货',
  FulfillmentOrderAllocated:  '商品正在配货中',
  FulfillmentShipped:         '商品已发货，等待签收',
  FulfillmentDelivered:       '商品已签收',
  ServiceActivated:           '服务已激活',
}

const timeline = computed(() =>
  [...activities.value]
    .sort((a, b) => new Date(b.occurredAt) - new Date(a.occurredAt))
    .map(ev => ({
      ...ev,
      description: EVENT_DESCRIPTIONS[ev.eventType] || ev.eventType,
    }))
)

function findEventTime(eventTypes) {
  if (!eventTypes?.length) return null
  const ev = activities.value.find(a => eventTypes.includes(a.eventType))
  return ev?.occurredAt ?? null
}

function formatPrice(cents) {
  return ((cents || 0) / 100).toFixed(2)
}

function formatShortTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd} ${hh}:${mi}`
}

function formatFullTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd} ${hh}:${mi}:${ss}`
}

function startCountdown() {
  stopCountdown()
  tick()
  countdownTimer = setInterval(tick, 1000)
}

function stopCountdown() {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  countdown.value = ''
}

function tick() {
  if (!paymentExpiredAt.value) { stopCountdown(); return }
  const remaining = new Date(paymentExpiredAt.value).getTime() - Date.now()
  if (remaining <= 0) {
    countdown.value = ''
    stopCountdown()
    load()
    return
  }
  const h = Math.floor(remaining / 3600000)
  const m = Math.floor((remaining % 3600000) / 60000)
  const s = Math.floor((remaining % 60000) / 1000)
  countdown.value = `${String(h).padStart(2, '0')}小时${String(m).padStart(2, '0')}分${String(s).padStart(2, '0')}秒`
}

async function load() {
  if (!orderId.value) return
  loading.value = true
  error.value = ''
  try {
    const [orderData, activityData] = await Promise.all([
      getOrder(orderId.value),
      getActivitiesByOrderId(orderId.value).catch(() => []),
    ])
    order.value = orderData
    activities.value = activityData

    if (orderData.status === 'PENDING_PAYMENT') {
      try {
        const payment = await getPaymentByOrderId(orderId.value)
        paymentExpiredAt.value = payment.expiredAt ?? null
        if (paymentExpiredAt.value) startCountdown()
      } catch {
        paymentExpiredAt.value = null
      }
    } else {
      stopCountdown()
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

let pollRetries = 0
async function loadWithPaymentPoll() {
  await load()
  if (order.value?.status === 'PENDING_PAYMENT' && pollRetries < 5) {
    pollRetries++
    setTimeout(loadWithPaymentPoll, 1500)
  } else {
    pollRetries = 0
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
  const fromPay = document.referrer && document.referrer.includes('mock-pay')
  if (fromPay) {
    loadWithPaymentPoll()
  } else {
    load()
  }
})
onUnmounted(() => stopCountdown())
watch(() => route.params.id, load)
</script>
