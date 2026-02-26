<template>
  <component
    :is="to ? 'router-link' : 'button'"
    :to="to"
    type="button"
    class="w-full flex items-center justify-between py-2 text-left text-sm text-gray-800 hover:text-vmall-red transition-colors"
    @click="to ? undefined : $emit('click')"
  >
    <span><slot /></span>
    <span class="text-vmall-gray-text">›</span>
  </component>
</template>

<script setup>
defineProps({
  to: { type: [String, Object], default: '' },
})
defineEmits(['click'])
</script>
