<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">›</span>
      <span class="text-gray-800">领券中心</span>
    </nav>

    <h1 class="text-xl font-bold text-gray-800 mb-6">领券中心</h1>

    <div v-if="loading" class="text-vmall-gray-text py-12 text-center">加载中…</div>
    <div v-else-if="error" class="text-vmall-red py-4">{{ error }}</div>
    <div v-else-if="!templates.length" class="text-vmall-gray-text py-12 text-center">
      <p>暂无可领取的优惠券</p>
      <router-link to="/" class="mt-4 inline-block text-vmall-red hover:underline">去逛逛</router-link>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="t in templates"
        :key="t.id"
        class="bg-white rounded-lg border border-vmall-gray-border p-4 flex items-center justify-between"
      >
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span
              class="inline-block px-2 py-0.5 text-xs rounded"
              :class="t.type === 'AMOUNT_OFF'
                ? 'bg-red-50 text-vmall-red'
                : 'bg-orange-50 text-orange-600'"
            >
              {{ t.type === 'AMOUNT_OFF' ? '满减' : '折扣' }}
            </span>
            <span class="font-medium text-gray-800 truncate">{{ t.name }}</span>
          </div>
          <div class="text-sm text-vmall-gray-text">
            <span v-if="t.type === 'AMOUNT_OFF'">
              满 {{ formatPrice(t.thresholdCents) }} 减 {{ formatPrice(t.discountCents) }}
            </span>
            <span v-else>
              {{ t.thresholdCents > 0 ? `满 ${formatPrice(t.thresholdCents)}` : '' }}
              打 {{ ((1 - t.discountRate) * 10).toFixed(1) }} 折
              <span v-if="t.maxDiscountCents">（最高减 {{ formatPrice(t.maxDiscountCents) }}）</span>
            </span>
          </div>
          <div class="text-xs text-vmall-gray-text mt-1">
            领取后 {{ t.validDays }} 天有效 · 剩余 {{ t.totalQuantity - t.issuedQuantity }} 张
          </div>
        </div>
        <button
          @click="handleClaim(t)"
          :disabled="claiming === t.id"
          class="ml-4 px-4 py-2 rounded-lg bg-vmall-red text-white text-sm hover:bg-vmall-red-hover disabled:opacity-50 shrink-0"
        >
          {{ claiming === t.id ? '领取中…' : '立即领取' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { getClaimableTemplates, claimCoupon } from '../shared/api/promotion.js'
import { formatPrice } from '../shared/utils/price.js'
import { formatApiError } from '../shared/utils/errorMessage.js'

const toast = inject('toast')

const templates = ref([])
const loading = ref(true)
const error = ref('')
const claiming = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    templates.value = await getClaimableTemplates()
  } catch (e) {
    error.value = formatApiError(e, '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleClaim(template) {
  claiming.value = template.id
  try {
    await claimCoupon(template.id)
    toast?.success?.(`成功领取「${template.name}」`)
    await load()
  } catch (e) {
    const msg = formatApiError(e, '领取失败')
    toast?.error?.(msg)
  } finally {
    claiming.value = null
  }
}

onMounted(load)
</script>
