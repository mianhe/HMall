<template>
  <div class="space-y-1">
    <div v-if="title" class="text-sm font-medium text-gray-600 mb-3">{{ title }}</div>
    <div
      v-for="(item, index) in items"
      :key="item.id ?? index"
      class="flex items-start gap-3 py-2 px-3 rounded-lg hover:bg-gray-50 transition-colors"
    >
      <!-- 时间线竖线 + 圆点 -->
      <div class="flex flex-col items-center pt-1 flex-shrink-0">
        <div :class="['w-2.5 h-2.5 rounded-full flex-shrink-0', dotColor(item.eventType)]" />
        <div v-if="index < items.length - 1" class="w-px bg-gray-200 flex-1 mt-1" style="min-height: 20px;" />
      </div>
      <!-- 事件内容 -->
      <div class="flex-1 min-w-0 pb-1">
        <div class="flex items-center gap-2 flex-wrap">
          <span :class="['text-xs font-medium px-1.5 py-0.5 rounded', eventTypeClass(item.eventType)]">
            {{ item.eventType }}
          </span>
          <span v-if="item.orderId" class="text-xs text-gray-500">订单 #{{ item.orderId }}</span>
          <span v-if="item.userId" class="text-xs text-gray-500">用户 #{{ item.userId }}</span>
          <span v-if="item.skuId" class="text-xs text-gray-500">SKU #{{ item.skuId }}</span>
          <span v-if="item.spuId" class="text-xs text-gray-500">SPU #{{ item.spuId }}</span>
        </div>
        <div class="text-xs text-gray-400 mt-0.5">{{ formatTime(item.occurredAt) }}</div>
      </div>
    </div>
    <div v-if="items.length === 0" class="text-sm text-gray-400 py-4 text-center">暂无事件</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, required: true },
  dimension: { type: Object, default: () => ({}) },
})

const title = computed(() => {
  const d = props.dimension
  if (d.userId != null) return `用户 #${d.userId} 的事件序列`
  if (d.orderId != null) return `订单 #${d.orderId} 的旅程`
  if (d.skuId != null) return `SKU #${d.skuId} 的活动`
  if (d.spuId != null) return `SPU #${d.spuId} 的活动`
  return ''
})

function formatTime(ts) {
  if (!ts) return ''
  try {
    return new Date(ts).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return ts
  }
}

const ORDER_EVENTS = new Set(['OrderCreated', 'OrderCancelled', 'OrderCompleted'])
const PAYMENT_EVENTS = new Set(['PaymentAttempted', 'PaymentSuccess', 'PaymentFailed', 'PaymentExpired'])
const FULFILLMENT_EVENTS = new Set(['FulfillmentCreated', 'FulfillmentAllocated', 'FulfillmentShipped', 'FulfillmentDelivered'])

function dotColor(eventType) {
  if (ORDER_EVENTS.has(eventType)) return 'bg-blue-400'
  if (PAYMENT_EVENTS.has(eventType)) return eventType === 'PaymentSuccess' ? 'bg-green-400' : 'bg-amber-400'
  if (FULFILLMENT_EVENTS.has(eventType)) return 'bg-purple-400'
  return 'bg-gray-300'
}

function eventTypeClass(eventType) {
  if (ORDER_EVENTS.has(eventType)) return 'bg-blue-50 text-blue-700'
  if (PAYMENT_EVENTS.has(eventType)) return eventType === 'PaymentSuccess' ? 'bg-green-50 text-green-700' : 'bg-amber-50 text-amber-700'
  if (FULFILLMENT_EVENTS.has(eventType)) return 'bg-purple-50 text-purple-700'
  return 'bg-gray-50 text-gray-600'
}
</script>
