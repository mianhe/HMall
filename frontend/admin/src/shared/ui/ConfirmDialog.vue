<template>
  <Teleport to="body">
    <div
      v-if="confirm.state.visible"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
      @click.self="confirm.onCancel()"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-2">{{ confirm.state.options.title }}</h2>
        <p class="text-sm text-vmall-gray-text mb-6">{{ confirm.state.options.message }}</p>
        <div class="flex justify-end gap-2">
          <button
            type="button"
            class="px-4 py-2 rounded-lg border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
            @click="confirm.onCancel()"
          >
            {{ confirm.state.options.cancelText }}
          </button>
          <button
            type="button"
            class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover"
            @click="confirm.onConfirm()"
          >
            {{ confirm.state.options.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { inject } from 'vue'

const confirm = inject('confirm')
</script>
