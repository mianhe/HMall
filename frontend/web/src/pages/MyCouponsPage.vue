<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">›</span>
      <span class="text-gray-800">我的优惠券</span>
    </nav>

    <h1 class="text-xl font-bold text-gray-800 mb-4">我的优惠券</h1>

    <div class="flex gap-2 mb-6">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        @click="activeTab = tab.value"
        class="px-4 py-1.5 rounded-full text-sm transition-colors"
        :class="activeTab === tab.value
          ? 'bg-vmall-red text-white'
          : 'bg-white border border-vmall-gray-border text-vmall-gray-text hover:border-vmall-red hover:text-vmall-red'"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="text-vmall-gray-text py-12 text-center">加载中…</div>
    <div v-else-if="error" class="text-vmall-red py-4">{{ error }}</div>
    <div v-else-if="!coupons.length" class="text-vmall-gray-text py-12 text-center">
      <p>暂无{{ activeTabLabel }}优惠券</p>
      <router-link to="/coupons" class="mt-4 inline-block text-vmall-red hover:underline">去领券中心看看</router-link>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="c in coupons"
        :key="c.id"
        class="bg-white rounded-lg border border-vmall-gray-border overflow-hidden"
        :class="{ 'opacity-60': c.status === 'USED' || c.status === 'EXPIRED' }"
      >
        <div class="flex">
          <div
            class="w-28 flex flex-col items-center justify-center p-3"
            :class="c.status === 'AVAILABLE'
              ? 'bg-vmall-red text-white'
              : 'bg-gray-200 text-vmall-gray-text'"
          >
            <template v-if="c.type === 'AMOUNT_OFF'">
              <span class="text-2xl font-bold">¥{{ (c.discountCents / 100).toFixed(0) }}</span>
              <span class="text-xs mt-0.5">满{{ (c.thresholdCents / 100).toFixed(0) }}可用</span>
            </template>
            <template v-else>
              <span class="text-2xl font-bold">{{ ((1 - c.discountRate) * 10).toFixed(1) }}折</span>
              <span v-if="c.thresholdCents > 0" class="text-xs mt-0.5">满{{ (c.thresholdCents / 100).toFixed(0) }}可用</span>
              <span v-else class="text-xs mt-0.5">无门槛</span>
            </template>
          </div>
          <div class="flex-1 p-3">
            <div class="flex items-center justify-between">
              <span class="font-medium text-gray-800">{{ c.name }}</span>
              <span class="text-xs" :class="statusClass(c.status)">
                {{ statusText(c.status) }}
              </span>
            </div>
            <div class="text-xs text-vmall-gray-text mt-2">
              {{ formatDate(c.issuedAt) }} 至 {{ formatDate(c.expiresAt) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getMyCoupons } from '../shared/api/promotion.js'
import { formatApiError } from '../shared/utils/errorMessage.js'

const tabs = [
  { label: '全部', value: '' },
  { label: '可用', value: 'AVAILABLE' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' },
]

const activeTab = ref('')
const coupons = ref([])
const loading = ref(true)
const error = ref('')

const activeTabLabel = computed(() => {
  const found = tabs.find(t => t.value === activeTab.value)
  return found?.value ? found.label : ''
})

function statusText(status) {
  const map = { AVAILABLE: '可用', LOCKED: '使用中', USED: '已使用', EXPIRED: '已过期' }
  return map[status] || status
}

function statusClass(status) {
  if (status === 'AVAILABLE') return 'text-green-600'
  if (status === 'USED' || status === 'EXPIRED') return 'text-vmall-gray-text'
  return 'text-orange-500'
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('zh-CN')
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    coupons.value = await getMyCoupons(activeTab.value || undefined)
  } catch (e) {
    error.value = formatApiError(e, '加载失败')
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => load())
onMounted(load)
</script>
