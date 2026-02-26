<template>
  <div>
    <AppHeader />
    <main class="max-w-7xl mx-auto px-4 py-8">
      <!-- Title + Time Range -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
        <h1 class="text-2xl font-bold text-gray-800">活动监控</h1>
        <div class="flex flex-wrap items-center gap-2">
          <div class="inline-flex rounded-lg border border-vmall-gray-border overflow-hidden">
            <button
              v-for="opt in periodOptions"
              :key="opt.value"
              @click="selectPeriod(opt.value)"
              :class="[
                'px-4 py-2 text-sm font-medium transition-colors',
                activePeriod === opt.value
                  ? 'bg-vmall-red text-white'
                  : 'bg-white text-gray-600 hover:bg-gray-50'
              ]"
            >
              {{ opt.label }}
            </button>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <input
              v-model="customFrom"
              type="date"
              class="px-2 py-2 rounded-lg border border-vmall-gray-border text-gray-700 text-sm"
            />
            <span class="text-gray-400">–</span>
            <input
              v-model="customTo"
              type="date"
              class="px-2 py-2 rounded-lg border border-vmall-gray-border text-gray-700 text-sm"
            />
            <button
              @click="applyCustomRange"
              :disabled="!customFrom || !customTo"
              class="px-3 py-2 rounded-lg bg-white border border-vmall-gray-border text-gray-600 hover:bg-gray-50 transition-colors disabled:opacity-40 text-sm"
            >
              查询
            </button>
          </div>
        </div>
      </div>

      <!-- Loading / Error -->
      <div v-if="statsLoading" class="text-center py-12 text-gray-400">加载中…</div>
      <div v-else-if="statsError" class="text-vmall-red mb-6">{{ statsError }}</div>

      <!-- Stats Cards -->
      <template v-if="stats && !statsLoading">
        <p class="text-sm text-gray-400 mb-6">
          统计区间：{{ stats.from }} 至 {{ stats.to }}
        </p>

        <!-- 订单概览 -->
        <section class="mb-8">
          <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">订单概览</h2>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <StatCard label="已创建订单" :value="stats.ordersCreated" color="blue" />
            <StatCard label="已取消订单" :value="stats.ordersCancelled" color="red" />
            <StatCard label="已完成订单" :value="stats.ordersCompleted" color="green" />
          </div>
        </section>

        <!-- 支付概览 -->
        <section class="mb-8">
          <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">支付概览</h2>
          <div class="grid grid-cols-2 sm:grid-cols-5 gap-4">
            <div class="bg-white rounded-lg border border-vmall-gray-border p-5 flex flex-col sm:col-span-1">
              <span class="text-3xl font-bold tabular-nums text-gray-800">{{ formatMoney(stats.paymentTotalCents) }}</span>
              <span class="mt-1 text-sm text-gray-500">成功支付金额</span>
            </div>
            <StatCard label="支付尝试" :value="stats.paymentAttempts" color="gray" />
            <StatCard label="成功支付" :value="stats.paymentSuccess" color="green" />
            <StatCard label="支付失败" :value="stats.paymentFailed" color="red" />
            <StatCard label="支付过期" :value="stats.paymentExpired" color="amber" />
          </div>
          <!-- 支付成功率进度条 -->
          <div v-if="stats.paymentAttempts > 0" class="mt-3 bg-white rounded-lg border border-vmall-gray-border p-4">
            <div class="flex items-center justify-between text-sm mb-2">
              <span class="text-gray-500">支付成功率</span>
              <span class="font-medium" :class="successRateColor">{{ successRate }}%</span>
            </div>
            <div class="w-full bg-gray-100 rounded-full h-2.5">
              <div
                class="h-2.5 rounded-full transition-all duration-500"
                :class="successRateBarColor"
                :style="{ width: successRate + '%' }"
              />
            </div>
          </div>
        </section>

        <!-- 履约概览 -->
        <section class="mb-8">
          <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">履约概览</h2>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <StatCard label="履约单创建" :value="stats.fulfillmentCreated" color="blue" />
            <StatCard label="正在配货" :value="stats.fulfillmentAllocated" color="amber" />
            <StatCard label="已发货" :value="stats.fulfillmentShipped" color="blue" />
            <StatCard label="已签收" :value="stats.fulfillmentDelivered" color="green" />
          </div>
          <!-- 履约漏斗 -->
          <div v-if="stats.fulfillmentCreated > 0" class="mt-3 bg-white rounded-lg border border-vmall-gray-border p-4">
            <div class="flex items-center gap-3 text-sm">
              <FunnelStep label="创建" :count="stats.fulfillmentCreated" color="bg-blue-500" :total="stats.fulfillmentCreated" />
              <span class="text-gray-300">→</span>
              <FunnelStep label="配货" :count="stats.fulfillmentAllocated" color="bg-amber-500" :total="stats.fulfillmentCreated" />
              <span class="text-gray-300">→</span>
              <FunnelStep label="发货" :count="stats.fulfillmentShipped" color="bg-blue-500" :total="stats.fulfillmentCreated" />
              <span class="text-gray-300">→</span>
              <FunnelStep label="签收" :count="stats.fulfillmentDelivered" color="bg-green-500" :total="stats.fulfillmentCreated" />
            </div>
          </div>
        </section>

        <!-- 库存活动 -->
        <section class="mb-8">
          <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">库存活动</h2>
          <div class="grid grid-cols-2 gap-4 max-w-lg">
            <StatCard label="库存占用" :value="stats.stockReserved" color="blue" />
            <StatCard label="库存释放" :value="stats.stockReleased" color="green" />
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import StatCard from '../shared/ui/StatCard.vue'
import FunnelStep from '../shared/ui/FunnelStep.vue'
import { getActivityStats } from '../shared/api/activity.js'

const periodOptions = [
  { value: 'today', label: '今日' },
  { value: 'last7', label: '最近 7 天' },
  { value: 'last30', label: '最近 30 天' },
]

const activePeriod = ref('today')
const customFrom = ref('')
const customTo = ref('')
const stats = ref(null)
const statsLoading = ref(false)
const statsError = ref('')

const successRate = computed(() => {
  if (!stats.value || stats.value.paymentAttempts === 0) return 0
  return Math.round((stats.value.paymentSuccess / stats.value.paymentAttempts) * 100)
})

const successRateColor = computed(() => {
  const r = successRate.value
  if (r >= 90) return 'text-green-600'
  if (r >= 70) return 'text-amber-600'
  return 'text-red-600'
})

const successRateBarColor = computed(() => {
  const r = successRate.value
  if (r >= 90) return 'bg-green-500'
  if (r >= 70) return 'bg-amber-500'
  return 'bg-red-500'
})

function formatMoney(cents) {
  if (cents == null || cents === 0) return '¥0.00'
  return '¥' + (cents / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function selectPeriod(period) {
  activePeriod.value = period
  customFrom.value = ''
  customTo.value = ''
  loadStats({ period })
}

function applyCustomRange() {
  if (!customFrom.value || !customTo.value) return
  activePeriod.value = ''
  loadStats({ from: customFrom.value, to: customTo.value })
}

async function loadStats(params) {
  statsLoading.value = true
  statsError.value = ''
  try {
    stats.value = await getActivityStats(params)
  } catch (e) {
    statsError.value = e.response?.data?.message || e.message || '加载统计失败'
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  loadStats({ period: 'today' })
})
</script>
