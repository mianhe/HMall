<template>
  <div>
    <AppHeader />
    <main class="max-w-6xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-gray-800 mb-8">事件</h1>

      <!-- 订单旅程回放 -->
      <section class="mb-8">
        <div class="bg-white rounded-lg border border-vmall-gray-border p-5">
          <div class="flex items-center gap-3 mb-4">
            <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide">订单旅程回放</h2>
            <form @submit.prevent="loadJourney" class="flex items-center gap-2 ml-auto">
              <input
                v-model.trim="journeyOrderId"
                type="text"
                inputmode="numeric"
                placeholder="订单 ID"
                class="px-3 py-2 rounded-lg border border-vmall-gray-border text-sm text-gray-700 w-32 focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red"
              />
              <button
                type="submit"
                :disabled="!journeyOrderId"
                class="px-4 py-2 rounded-lg bg-vmall-red text-white text-sm font-medium hover:bg-vmall-red-hover transition-colors disabled:opacity-40"
              >查看旅程</button>
            </form>
          </div>

          <!-- 未输入提示 -->
          <div v-if="!activeOrderId && !journeyLoading" class="text-center py-8 text-gray-400">
            <p class="text-sm">输入订单 ID，查看交易全生命周期的事件回放</p>
          </div>

          <!-- 加载中 -->
          <div v-else-if="journeyLoading" class="text-center py-8 text-gray-400">加载中…</div>

          <!-- 错误 -->
          <div v-else-if="journeyError" class="text-vmall-red py-4 text-sm">{{ journeyError }}</div>

          <!-- 空 -->
          <div v-else-if="journeyActivities.length === 0 && activeOrderId" class="text-center py-8 text-gray-400">
            <p class="text-sm">未找到订单 #{{ activeOrderId }} 的事件记录</p>
          </div>

          <!-- 旅程内容 -->
          <template v-else-if="journeyActivities.length > 0">
            <!-- 概要 -->
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4 p-4 bg-gray-50 rounded-lg">
              <div>
                <p class="text-xs text-gray-500 mb-1">订单号</p>
                <p class="text-lg font-bold text-gray-800 font-mono">#{{ activeOrderId }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 mb-1">当前状态</p>
                <span :class="['inline-block px-2 py-0.5 rounded text-sm font-medium', statusBadgeClass]">
                  {{ statusLabel }}
                </span>
              </div>
              <div>
                <p class="text-xs text-gray-500 mb-1">事件数量</p>
                <p class="text-lg font-bold text-gray-800">{{ journeyActivities.length }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 mb-1">时间跨度</p>
                <p class="text-sm font-medium text-gray-800">{{ timeSpan }}</p>
              </div>
            </div>

            <!-- 泳道图：key 保证换订单时重新挂载，从而自动开始回放 -->
            <SwimlaneDiagram
              :key="activeOrderId"
              :events="timeline"
              @select="onSwimlaneSelect"
            />

            <!-- 选中事件详情 -->
            <transition name="expand">
              <div v-if="selectedEvent" class="mt-4 rounded-lg border p-4" :class="selectedEvent.isCompensation ? 'bg-red-50 border-red-200' : selectedEvent.isException ? 'bg-amber-50 border-amber-200' : 'bg-gray-50 border-gray-200'">
                <div class="flex items-center gap-3 mb-3">
                  <span :class="['inline-block px-2 py-0.5 rounded text-xs font-medium', selectedEvent.bcBadgeClass]">{{ selectedEvent.bcLabel }}</span>
                  <span class="text-sm font-medium text-gray-800">{{ selectedEvent.label }}</span>
                  <span v-if="selectedEvent.isCompensation" class="text-xs text-red-500 font-medium">补偿</span>
                  <span class="flex-1" />
                  <span class="text-xs text-gray-400 tabular-nums">{{ formatTimeFull(selectedEvent.occurredAt) }}</span>
                  <button @click="selectedEvent = null" class="text-gray-400 hover:text-gray-600 ml-1">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
                  </button>
                </div>
                <div v-if="selectedEvent.compensatesLabel" class="mb-2 text-xs text-red-400 flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" /></svg>
                  补偿了「{{ selectedEvent.compensatesLabel }}」
                </div>
                <div class="text-sm space-y-1.5">
                  <div v-for="(detail, dIdx) in selectedEvent.details" :key="dIdx" class="flex gap-2">
                    <span class="text-gray-400 shrink-0 w-24 text-right">{{ detail.label }}：</span>
                    <span class="text-gray-700">{{ detail.value }}</span>
                  </div>
                  <div class="flex gap-2">
                    <span class="text-gray-400 shrink-0 w-24 text-right">事件类型：</span>
                    <span class="text-gray-500 font-mono text-xs">{{ selectedEvent.eventType }}</span>
                  </div>
                  <div class="flex gap-2">
                    <span class="text-gray-400 shrink-0 w-24 text-right">Topic：</span>
                    <span class="text-gray-500 font-mono text-xs">{{ selectedEvent.topic }}</span>
                  </div>
                </div>
              </div>
            </transition>
          </template>
        </div>
      </section>

      <!-- 本周事件列表 -->
      <section>
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide">本周事件</h2>
            <span class="text-xs text-gray-400 bg-gray-100 rounded-full px-2 py-0.5">{{ weekActivities.length }}</span>
          </div>
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
                <th class="px-4 py-3 font-medium">事件</th>
                <th class="px-4 py-3 font-medium">订单 ID</th>
                <th class="px-4 py-3 font-medium">详情</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="recentLoading && !weekActivities.length">
                <td colspan="4" class="px-4 py-8 text-center text-gray-400">加载中…</td>
              </tr>
              <tr v-else-if="!weekActivities.length">
                <td colspan="4" class="px-4 py-8 text-center text-gray-400">本周暂无事件</td>
              </tr>
              <tr
                v-for="item in weekActivities"
                :key="item.id"
                class="border-t border-gray-100 hover:bg-gray-50 transition-colors cursor-pointer"
                @click="viewOrder(item.orderId)"
              >
                <td class="px-4 py-3 text-gray-600 whitespace-nowrap">{{ formatTimeShort(item.occurredAt) }}</td>
                <td class="px-4 py-3">
                  <span :class="['inline-block px-2 py-0.5 rounded text-xs font-medium', eventBadgeClass(item)]">
                    {{ resolveLabel(item) }}
                  </span>
                </td>
                <td class="px-4 py-3 font-mono">
                  <span v-if="item.orderId" class="text-vmall-red">{{ item.orderId }}</span>
                  <span v-else class="text-gray-400">—</span>
                </td>
                <td class="px-4 py-3 text-gray-500 text-xs">{{ eventDetail(item) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import SwimlaneDiagram from '../shared/ui/SwimlaneDiagram.vue'
import { getRecentActivities, getActivitiesByOrderId, getEventMetadata } from '../shared/api/activity.js'

const metadataMap = ref({})

async function loadMetadata() {
  try {
    const list = await getEventMetadata()
    const map = {}
    for (const m of list) map[m.eventType] = m
    metadataMap.value = map
  } catch { /* 降级 */ }
}

// --- 订单旅程回放 ---
const journeyOrderId = ref('')
const activeOrderId = ref(null)
const journeyActivities = ref([])
const journeyLoading = ref(false)
const journeyError = ref('')
const selectedEvent = ref(null)

const BC_BADGE_CLASS = {
  Order: 'bg-blue-50 text-blue-700',
  Payment: 'bg-green-50 text-green-700',
  Inventory: 'bg-amber-50 text-amber-700',
  Fulfillment: 'bg-indigo-50 text-indigo-700',
}

function resolveMetadata(ev) {
  const meta = ev.metadata || metadataMap.value[ev.eventType]
  if (!meta) return { bcLabel: '未知', label: ev.eventType, bcBadgeClass: 'bg-gray-100 text-gray-600', isCompensation: false, isException: false, compensatesLabel: null }
  const compensatesLabel = meta.compensatesEventType
    ? (metadataMap.value[meta.compensatesEventType]?.label || meta.compensatesEventType)
    : null
  return {
    bcLabel: meta.boundedContext,
    label: meta.label,
    bcBadgeClass: BC_BADGE_CLASS[meta.boundedContext] || 'bg-gray-100 text-gray-600',
    isCompensation: meta.category === 'compensation',
    isException: meta.category === 'exception',
    compensatesLabel,
  }
}

function parsePayload(raw) {
  try { return JSON.parse(raw || '{}') } catch { return {} }
}

function extractDetails(eventType, payload) {
  const details = []
  const p = parsePayload(payload)
  if (p.amountCents != null) details.push({ label: '支付金额', value: `¥${(p.amountCents / 100).toFixed(2)}` })
  if (p.paymentId != null) details.push({ label: '支付单号', value: `#${p.paymentId}` })
  if (p.items?.length) details.push({ label: '商品明细', value: p.items.map(i => `SKU#${i.skuId} ×${i.quantity}`).join('、') })
  if (p.fulfillmentOrderId != null) details.push({ label: '履约单号', value: `#${p.fulfillmentOrderId}` })
  if (p.fulfillmentOrderIds?.length) details.push({ label: '履约单号', value: p.fulfillmentOrderIds.map(id => `#${id}`).join('、') })
  if (p.carrier) details.push({ label: '物流公司', value: p.carrier })
  if (p.trackingNumber) details.push({ label: '物流单号', value: p.trackingNumber })
  return details
}

const timeline = computed(() =>
  journeyActivities.value.map(ev => ({
    ...ev,
    ...resolveMetadata(ev),
    details: extractDetails(ev.eventType, ev.payload),
  }))
)

const lastEvent = computed(() => timeline.value.length ? timeline.value[timeline.value.length - 1] : null)
const statusLabel = computed(() => lastEvent.value?.label || '未知')
const statusBadgeClass = computed(() => {
  const ev = lastEvent.value
  if (!ev) return 'bg-gray-100 text-gray-600'
  if (ev.isCompensation) return 'bg-red-50 text-red-700'
  if (ev.isException) return 'bg-amber-50 text-amber-700'
  return 'bg-green-50 text-green-700'
})

const timeSpan = computed(() => {
  if (journeyActivities.value.length < 2) return '—'
  const first = new Date(journeyActivities.value[0].occurredAt)
  const last = new Date(journeyActivities.value[journeyActivities.value.length - 1].occurredAt)
  const diffMs = last - first
  if (diffMs < 1000) return `${diffMs}ms`
  if (diffMs < 60000) return `${(diffMs / 1000).toFixed(1)}s`
  if (diffMs < 3600000) return `${Math.floor(diffMs / 60000)}m ${Math.floor((diffMs % 60000) / 1000)}s`
  return `${Math.floor(diffMs / 3600000)}h ${Math.floor((diffMs % 3600000) / 60000)}m`
})

function onSwimlaneSelect(node) {
  selectedEvent.value = selectedEvent.value?.eventId === node.eventId ? null : node
}

async function loadJourney() {
  if (!journeyOrderId.value) return
  activeOrderId.value = Number(journeyOrderId.value)
  journeyLoading.value = true
  journeyError.value = ''
  selectedEvent.value = null
  journeyActivities.value = []
  try {
    journeyActivities.value = await getActivitiesByOrderId(activeOrderId.value)
  } catch (e) {
    journeyError.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    journeyLoading.value = false
  }
}

function viewOrder(orderId) {
  if (!orderId) return
  journeyOrderId.value = String(orderId)
  loadJourney()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// --- 事件列表 ---
const recentActivities = ref([])
const recentLoading = ref(false)
const recentError = ref('')

const weekActivities = computed(() => {
  const now = new Date()
  const monday = new Date(now)
  const day = monday.getDay()
  const diff = day === 0 ? 6 : day - 1
  monday.setDate(monday.getDate() - diff)
  monday.setHours(0, 0, 0, 0)
  return recentActivities.value.filter(a => a.occurredAt && new Date(a.occurredAt) >= monday)
})

function resolveLabel(item) {
  const meta = item.metadata || metadataMap.value[item.eventType]
  return meta?.label || item.eventType
}

const CATEGORY_BADGE = { compensation: 'bg-red-50 text-red-700', exception: 'bg-amber-50 text-amber-700' }

function eventBadgeClass(item) {
  const meta = item.metadata || metadataMap.value[item.eventType]
  return meta ? (CATEGORY_BADGE[meta.category] || 'bg-gray-100 text-gray-600') : 'bg-gray-100 text-gray-600'
}

function eventDetail(item) {
  try {
    const p = JSON.parse(item.payload || '{}')
    if (p.amountCents) return `¥${(p.amountCents / 100).toFixed(2)}`
    if (p.fulfillmentOrderId) return `履约单 #${p.fulfillmentOrderId}`
  } catch { /* ignore */ }
  return ''
}

function formatTimeShort(isoStr) {
  if (!isoStr) return '—'
  const d = new Date(isoStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function formatTimeFull(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`
}

async function loadRecent() {
  recentLoading.value = true
  recentError.value = ''
  try {
    recentActivities.value = await getRecentActivities(200)
  } catch (e) {
    recentError.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    recentLoading.value = false
  }
}

onMounted(() => {
  loadMetadata()
  loadRecent()
})
</script>

<style scoped>
.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}
.expand-enter-to,
.expand-leave-from {
  max-height: 500px;
  opacity: 1;
}
</style>
