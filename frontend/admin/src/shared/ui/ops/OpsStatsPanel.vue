<template>
  <div>
    <div :class="gridClass">
      <StatCard
        v-for="(card, i) in normalizedCards"
        :key="i"
        :label="card.label"
        :value="card.value"
        :color="card.color"
        :status="card.status"
        :description="card.description"
      />
    </div>
    <div v-if="dateRange" class="mt-3 text-xs text-gray-400">
      统计区间：{{ dateRange }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatCard from '../StatCard.vue'

const props = defineProps({
  data: { type: Object, required: true },
})

function formatPrice(cents) {
  return `¥${(cents / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

const LEGACY_FIELDS = [
  { key: 'ordersCreated', label: '已开出订单' },
  { key: 'paymentSuccess', label: '支付成功', color: 'green' },
  { key: 'paymentTotalCents', label: '支付总额', format: 'price' },
  { key: 'fulfillmentShipped', label: '已发货', color: 'blue' },
  { key: 'stockReserved', label: '库存占用' },
]

const normalizedCards = computed(() => {
  const d = props.data || {}

  if (Array.isArray(d.cards) && d.cards.length > 0) {
    return d.cards.map(c => ({
      label: c.label || '',
      value: c.value ?? 0,
      color: c.color || '',
      status: c.status || '',
      description: c.description || '',
    }))
  }

  return LEGACY_FIELDS
    .filter(f => d[f.key] != null)
    .map(f => ({
      label: f.label,
      value: f.format === 'price' ? formatPrice(d[f.key]) : d[f.key],
      color: f.color || '',
      status: '',
      description: '',
    }))
})

const gridClass = computed(() => {
  const count = normalizedCards.value.length
  if (count <= 3) return 'grid grid-cols-1 sm:grid-cols-3 gap-4'
  if (count <= 4) return 'grid grid-cols-2 sm:grid-cols-4 gap-4'
  return 'grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4'
})

const dateRange = computed(() => {
  const d = props.data || {}
  if (d.from || d.to) return `${d.from || '—'} ~ ${d.to || '—'}`
  return ''
})
</script>
