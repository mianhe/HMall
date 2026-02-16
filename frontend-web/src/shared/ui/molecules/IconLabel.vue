<template>
  <component
    :is="to ? 'router-link' : 'div'"
    :to="to"
    class="flex flex-col items-center justify-center gap-1 min-w-0 text-center text-sm text-gray-800 hover:text-vmall-red transition-colors"
    :class="to ? 'cursor-pointer' : ''"
  >
    <span class="text-2xl" aria-hidden="true">{{ icon }}</span>
    <span class="truncate">{{ label }}</span>
  </component>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '📦' },
  label: { type: String, required: true },
  to: { type: [String, Object], default: '' },
})
</script>
