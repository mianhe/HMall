<template>
  <Teleport to="body">
    <div v-if="toast.items.length" class="fixed top-4 right-4 z-[100] flex flex-col gap-2 max-w-sm">
      <TransitionGroup name="toast">
        <div
          v-for="item in toast.items"
          :key="item.id"
          class="flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg border text-sm"
          :class="item.type === 'error' ? 'bg-white border-red-200 text-red-800' : 'bg-white border-green-200 text-green-800'"
        >
          <span class="flex-1">{{ item.message }}</span>
          <button
            v-if="item.type === 'error'"
            type="button"
            class="shrink-0 text-vmall-gray-text hover:text-gray-800"
            aria-label="关闭"
            @click="toast.remove(item.id)"
          >
            ×
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { inject } from 'vue'

const toast = inject('toast')
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(1rem);
}
</style>
