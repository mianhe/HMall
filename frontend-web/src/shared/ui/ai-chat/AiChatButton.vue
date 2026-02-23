<template>
  <button
    @click="$emit('toggle')"
    class="fixed bottom-6 right-6 z-40 w-14 h-14 rounded-full bg-vmall-red text-white shadow-lg
           hover:bg-vmall-red-hover transition-all duration-200 flex items-center justify-center
           hover:scale-105 active:scale-95"
    title="智能助手"
  >
    <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round"
            d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
    </svg>
  </button>
</template>

<script setup>
defineEmits(['toggle'])
</script>
