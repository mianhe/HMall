<template>
  <!-- Backdrop -->
  <Transition name="fade">
    <div
      v-if="open"
      class="fixed inset-0 bg-black/20 z-40"
      @click="$emit('close')"
    />
  </Transition>

  <!-- Drawer -->
  <Transition name="slide">
    <div
      v-if="open"
      class="fixed top-0 right-0 bottom-0 w-[420px] max-w-full z-50 flex flex-col bg-gray-50 shadow-2xl"
    >
      <!-- Header -->
      <div class="flex items-center justify-between px-4 py-3 bg-white border-b border-gray-200">
        <div class="flex items-center gap-2">
          <h2 class="font-semibold text-gray-800">AI 助手</h2>
          <select
            v-if="chat.models.value.length > 1"
            v-model="chat.selectedProvider.value"
            class="text-xs border border-gray-200 rounded px-1.5 py-0.5 text-gray-600 bg-gray-50
                   focus:outline-none focus:ring-1 focus:ring-vmall-red/30"
          >
            <option v-for="m in chat.models.value" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div class="flex items-center gap-1">
          <button
            @click="chat.clearMessages()"
            class="p-1.5 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
            title="清空对话"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
          <button
            @click="$emit('close')"
            class="p-1.5 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
            title="关闭 (Esc)"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Messages -->
      <AiMessageList :messages="chat.messages.value" />

      <!-- Input -->
      <AiChatInput
        ref="chatInputRef"
        :disabled="chat.isStreaming.value"
        @send="chat.sendMessage"
      />
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, nextTick, inject } from 'vue'
import AiMessageList from './AiMessageList.vue'
import AiChatInput from './AiChatInput.vue'

const props = defineProps({ open: Boolean })
defineEmits(['close'])

const chat = inject('aiChat')
const chatInputRef = ref(null)

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    nextTick(() => chatInputRef.value?.focus())
  }
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.slide-enter-active, .slide-leave-active {
  transition: transform 0.3s ease;
}
.slide-enter-from, .slide-leave-to {
  transform: translateX(100%);
}
</style>
