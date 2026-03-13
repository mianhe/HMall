<template>
  <div :class="['bg-white rounded-lg border p-5 flex flex-col', borderClass]">
    <div class="flex items-baseline gap-1.5">
      <span :class="['text-3xl font-bold tabular-nums', valueColor]">
        {{ formattedValue }}
      </span>
      <span v-if="statusIcon" :class="['text-lg', statusIconColor]">{{ statusIcon }}</span>
    </div>
    <span class="mt-1 text-sm text-gray-500">{{ label }}</span>
    <span v-if="description" :class="['mt-0.5 text-xs', descriptionColor]">{{ description }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: 0 },
  color: { type: String, default: '' },
  status: { type: String, default: '' },
  description: { type: String, default: '' },
})

const STATUS_STYLES = {
  success: { value: 'text-green-600', border: 'border-green-200', icon: '', iconColor: '', desc: 'text-green-600' },
  warning: { value: 'text-amber-600', border: 'border-amber-300', icon: '⚠', iconColor: 'text-amber-500', desc: 'text-amber-600' },
  critical: { value: 'text-red-600', border: 'border-red-300', icon: '✖', iconColor: 'text-red-500', desc: 'text-red-600' },
}

const COLOR_MAP = {
  red: 'text-red-600',
  green: 'text-green-600',
  blue: 'text-blue-600',
  amber: 'text-amber-600',
  gray: 'text-gray-800',
}

const style = computed(() => STATUS_STYLES[props.status])

const valueColor = computed(() => {
  if (style.value) return style.value.value
  return COLOR_MAP[props.color] || 'text-gray-800'
})

const borderClass = computed(() => {
  if (style.value) return style.value.border
  return 'border-vmall-gray-border'
})

const statusIcon = computed(() => style.value?.icon || '')
const statusIconColor = computed(() => style.value?.iconColor || '')

const descriptionColor = computed(() => {
  if (style.value) return style.value.desc
  return 'text-gray-400'
})

const formattedValue = computed(() => {
  if (typeof props.value === 'string') return props.value
  return props.value != null ? props.value.toLocaleString() : '0'
})
</script>
