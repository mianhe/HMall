<template>
  <div>
    <AppHeader />
    <main class="max-w-6xl mx-auto px-4 py-8">
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
        <!-- Date range echo -->
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
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <StatCard label="支付尝试" :value="stats.paymentAttempts" color="gray" />
            <StatCard label="成功支付" :value="stats.paymentSuccess" color="green" />
            <StatCard label="支付失败" :value="stats.paymentFailed" color="red" />
            <StatCard label="支付过期" :value="stats.paymentExpired" color="amber" />
          </div>
        </section>

        <!-- 库存活动 -->
        <section class="mb-8">
          <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">库存活动</h2>
          <div class="grid grid-cols-2 sm:grid-cols-2 gap-4 max-w-lg">
            <StatCard label="库存占用" :value="stats.stockReserved" color="blue" />
            <StatCard label="库存释放" :value="stats.stockReleased" color="green" />
          </div>
        </section>
      </template>

      <!-- Recent Activities -->
      <section>
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide">最近活动</h2>
          <button
            @click="loadRecent"
            :disabled="recentLoading"
            class="text-sm text-gray-500 hover:text-vmall-red transition-colors disabled:opacity-50"
          >
            {{ recentLoading ? '刷新中…' : '刷新' }}
          </button>
        </div>
        <div v-if="recentError" class="text-vmall-red text-sm mb-2">{{ recentError }}</div>
        <div class="bg-white rounded-lg border border-vmall-gray-border overflow-hidden">
          <table class="w-full text-sm">
            <thead>
              <tr class="bg-gray-50 text-gray-500 text-left">
                <th class="px-4 py-3 font-medium">时间</th>
                <th class="px-4 py-3 font-medium">事件类型</th>
                <th class="px-4 py-3 font-medium">订单 ID</th>
                <th class="px-4 py-3 font-medium">Topic</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!recentActivities.length && !recentLoading">
                <td colspan="4" class="px-4 py-8 text-center text-gray-400">暂无活动记录</td>
              </tr>
              <tr
                v-for="item in recentActivities"
                :key="item.id"
                class="border-t border-gray-100 hover:bg-gray-50 transition-colors"
              >
                <td class="px-4 py-3 text-gray-600 whitespace-nowrap">{{ formatTime(item.occurredAt) }}</td>
                <td class="px-4 py-3">
                  <span :class="['inline-block px-2 py-0.5 rounded text-xs font-medium', badgeClass(item.eventType)]">
                    {{ item.eventType }}
                  </span>
                </td>
                <td class="px-4 py-3 text-gray-700 font-mono">{{ item.orderId ?? '—' }}</td>
                <td class="px-4 py-3 text-gray-500">{{ item.topic }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import StatCard from '../shared/ui/StatCard.vue'
import { getActivityStats, getRecentActivities } from '../shared/api/activity.js'

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
const recentActivities = ref([])
const recentLoading = ref(false)
const recentError = ref('')

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

async function loadRecent() {
  recentLoading.value = true
  recentError.value = ''
  try {
    recentActivities.value = await getRecentActivities(20)
  } catch (e) {
    recentError.value = e.response?.data?.message || e.message || '加载活动失败'
  } finally {
    recentLoading.value = false
  }
}

function formatTime(isoStr) {
  if (!isoStr) return '—'
  const d = new Date(isoStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const eventBadgeMap = {
  OrderCreated: 'bg-blue-50 text-blue-700',
  OrderCancelled: 'bg-red-50 text-red-700',
  OrderCompleted: 'bg-green-50 text-green-700',
  PaymentCompleted: 'bg-green-50 text-green-700',
  PaymentFailed: 'bg-red-50 text-red-700',
  PaymentExpired: 'bg-amber-50 text-amber-700',
  StockReserved: 'bg-blue-50 text-blue-700',
  StockReleased: 'bg-green-50 text-green-700',
}

function badgeClass(eventType) {
  return eventBadgeMap[eventType] || 'bg-gray-100 text-gray-600'
}

onMounted(() => {
  loadStats({ period: 'today' })
  loadRecent()
})
</script>
