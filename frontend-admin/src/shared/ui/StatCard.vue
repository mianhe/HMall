<template>
  <div class="bg-white rounded-lg border border-vmall-gray-border p-5 flex flex-col">
    <span :class="['text-3xl font-bold tabular-nums', valueColor]">
      {{ formattedValue }}
    </span>
    <span class="mt-1 text-sm text-gray-500">{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: Number, default: 0 },
  color: { type: String, default: 'gray' },
})

const colorMap = {
  red: 'text-red-600',
  green: 'text-green-600',
  blue: 'text-blue-600',
  amber: 'text-amber-600',
  gray: 'text-gray-800',
}

const valueColor = computed(() => colorMap[props.color] || colorMap.gray)

const formattedValue = computed(() =>
  props.value != null ? props.value.toLocaleString() : '0'
)
</script>
