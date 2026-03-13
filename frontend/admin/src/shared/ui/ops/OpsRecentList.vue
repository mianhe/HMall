<template>
  <div class="space-y-1">
    <div class="text-sm font-medium text-gray-600 mb-3">最近事件动态</div>
    <div
      v-for="(item, index) in items"
      :key="item.id ?? index"
      class="flex items-center gap-3 py-2 px-3 rounded-lg hover:bg-gray-50 transition-colors"
    >
      <div :class="['w-2 h-2 rounded-full flex-shrink-0', dotColor(item.eventType)]" />
      <span :class="['text-xs font-medium px-1.5 py-0.5 rounded flex-shrink-0', eventTypeClass(item.eventType)]">
        {{ item.eventType }}
      </span>
      <span v-if="item.orderId" class="text-xs text-gray-500 flex-shrink-0">订单 #{{ item.orderId }}</span>
      <span class="text-xs text-gray-400 ml-auto flex-shrink-0">{{ formatTime(item.occurredAt) }}</span>
    </div>
    <div v-if="items.length === 0" class="text-sm text-gray-400 py-4 text-center">暂无近期事件</div>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, required: true },
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
