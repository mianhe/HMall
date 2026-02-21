<template>
  <div class="flex-1 text-center">
    <div class="text-xs text-gray-500 mb-1">{{ label }}</div>
    <div class="font-semibold text-gray-800 tabular-nums">{{ count }}</div>
    <div class="mt-1 w-full bg-gray-100 rounded-full h-1.5">
      <div
        class="h-1.5 rounded-full transition-all duration-500"
        :class="color"
        :style="{ width: percentage + '%' }"
      />
    </div>
    <div class="text-xs text-gray-400 mt-0.5">{{ percentage }}%</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  count: { type: Number, default: 0 },
  total: { type: Number, default: 0 },
  color: { type: String, default: 'bg-blue-500' },
})

const percentage = computed(() => {
  if (props.total === 0) return 0
  return Math.round((props.count / props.total) * 100)
})
</script>
