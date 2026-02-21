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

      <!-- Today's Events (collapsible) -->
      <section>
        <div
          class="flex items-center justify-between mb-3 cursor-pointer select-none"
          @click="eventsExpanded = !eventsExpanded"
        >
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold text-gray-500 uppercase tracking-wide">今日事件</span>
            <span class="text-xs text-gray-400 bg-gray-100 rounded-full px-2 py-0.5">{{ todayActivities.length }}</span>
            <svg
              :class="['w-4 h-4 text-gray-400 transition-transform', eventsExpanded ? 'rotate-180' : '']"
              fill="none" stroke="currentColor" viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </div>
          <button
            @click.stop="loadRecent"
            :disabled="recentLoading"
            class="text-sm text-gray-500 hover:text-vmall-red transition-colors disabled:opacity-50"
          >
            {{ recentLoading ? '刷新中…' : '刷新' }}
          </button>
        </div>
        <div v-if="recentError" class="text-vmall-red text-sm mb-2">{{ recentError }}</div>
        <transition name="collapse">
          <div v-show="eventsExpanded" class="bg-white rounded-lg border border-vmall-gray-border overflow-hidden">
            <table class="w-full text-sm">
              <thead>
                <tr class="bg-gray-50 text-gray-500 text-left">
                  <th class="px-4 py-3 font-medium">时间</th>
                  <th class="px-4 py-3 font-medium">事件</th>
                  <th class="px-4 py-3 font-medium">订单 ID</th>
                  <th class="px-4 py-3 font-medium">详情</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!todayActivities.length && !recentLoading">
                  <td colspan="4" class="px-4 py-8 text-center text-gray-400">今日暂无事件</td>
                </tr>
                <tr
                  v-for="item in todayActivities"
                  :key="item.id"
                  class="border-t border-gray-100 hover:bg-gray-50 transition-colors"
                >
                  <td class="px-4 py-3 text-gray-600 whitespace-nowrap">{{ formatTime(item.occurredAt) }}</td>
                  <td class="px-4 py-3">
                    <span :class="['inline-block px-2 py-0.5 rounded text-xs font-medium', badgeClass(item.eventType)]">
                      {{ eventLabel(item.eventType) }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-gray-700 font-mono">{{ item.orderId ?? '—' }}</td>
                  <td class="px-4 py-3 text-gray-500 text-xs">{{ eventDetail(item) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </transition>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import StatCard from '../shared/ui/StatCard.vue'
import FunnelStep from '../shared/ui/FunnelStep.vue'
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
const eventsExpanded = ref(true)

const EVENT_LABELS = {
  OrderCreated:               '创建订单',
  OrderCancelled:             '取消订单',
  OrderCompleted:             '订单完成',
  PaymentCompleted:           '支付成功',
  PaymentFailed:              '支付失败',
  PaymentExpired:             '支付过期',
  StockReserved:              '库存占用',
  StockReleased:              '库存释放',
  FulfillmentOrderCreated:    '履约单创建',
  FulfillmentOrderAllocated:  '开始配货',
  FulfillmentShipped:         '已发货',
  FulfillmentDelivered:       '已签收',
}

function eventLabel(eventType) {
  return EVENT_LABELS[eventType] || eventType
}

function eventDetail(item) {
  try {
    const p = JSON.parse(item.payload || '{}')
    if (item.eventType === 'PaymentCompleted' && p.amountCents) {
      return `¥${(p.amountCents / 100).toFixed(2)}`
    }
    if (item.eventType === 'FulfillmentShipped' && p.fulfillmentOrderId) {
      return `履约单 #${p.fulfillmentOrderId}`
    }
    if (item.eventType === 'FulfillmentOrderAllocated' && p.fulfillmentOrderId) {
      return `履约单 #${p.fulfillmentOrderId}`
    }
  } catch { /* ignore */ }
  return ''
}

const todayActivities = computed(() => {
  const todayStr = new Date().toISOString().slice(0, 10)
  return recentActivities.value.filter(a => {
    if (!a.occurredAt) return false
    return a.occurredAt.startsWith(todayStr) || new Date(a.occurredAt).toISOString().slice(0, 10) === todayStr
  })
})

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

async function loadRecent() {
  recentLoading.value = true
  recentError.value = ''
  try {
    recentActivities.value = await getRecentActivities(100)
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
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const eventBadgeMap = {
  OrderCreated:               'bg-blue-50 text-blue-700',
  OrderCancelled:             'bg-red-50 text-red-700',
  OrderCompleted:             'bg-green-50 text-green-700',
  PaymentCompleted:           'bg-green-50 text-green-700',
  PaymentFailed:              'bg-red-50 text-red-700',
  PaymentExpired:             'bg-amber-50 text-amber-700',
  StockReserved:              'bg-blue-50 text-blue-700',
  StockReleased:              'bg-green-50 text-green-700',
  FulfillmentOrderCreated:    'bg-indigo-50 text-indigo-700',
  FulfillmentOrderAllocated:  'bg-amber-50 text-amber-700',
  FulfillmentShipped:         'bg-blue-50 text-blue-700',
  FulfillmentDelivered:       'bg-green-50 text-green-700',
}

function badgeClass(eventType) {
  return eventBadgeMap[eventType] || 'bg-gray-100 text-gray-600'
}

onMounted(() => {
  loadStats({ period: 'today' })
  loadRecent()
})
</script>

<style scoped>
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.collapse-enter-from,
.collapse-leave-to {
  max-height: 0;
  opacity: 0;
}
.collapse-enter-to,
.collapse-leave-from {
  max-height: 2000px;
  opacity: 1;
}
</style>
