<template>
  <div class="h-full flex flex-col bg-gray-50 border-l border-gray-200">
    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-3 bg-white border-b border-gray-200 flex-shrink-0">
      <div class="flex items-center gap-2">
        <span class="font-semibold text-gray-800 text-sm">智能运营助手</span>
        <select
          v-if="chat.models.value.length > 1"
          v-model="chat.selectedProvider.value"
          class="text-xs border border-gray-200 rounded px-1.5 py-0.5 text-gray-600 bg-gray-50
                 focus:outline-none focus:ring-1 focus:ring-vmall-red/30"
        >
          <option v-for="m in chat.models.value" :key="m.id" :value="m.id">{{ m.name }}</option>
        </select>
        <SkillSelector />
      </div>
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
    </div>

    <!-- Skill matched notification -->
    <Transition name="fade">
      <div
        v-if="chat.lastMatchedSkills.value.length > 0"
        class="px-4 py-1.5 bg-blue-50 border-b border-blue-100 text-xs text-blue-600 flex items-center gap-1.5 flex-shrink-0"
      >
        <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
        <span>已匹配：{{ chat.lastMatchedSkills.value.map(s => s.name).join('、') }}</span>
      </div>
    </Transition>

    <!-- Messages -->
    <AiMessageList :messages="chat.messages.value" />

    <!-- Input -->
    <AiChatInput
      ref="inputRef"
      :disabled="chat.isStreaming.value"
      @send="chat.sendMessage"
    />
  </div>
</template>

<script setup>
import { ref, inject } from 'vue'
import AiMessageList from '../ai-chat/AiMessageList.vue'
import AiChatInput from '../ai-chat/AiChatInput.vue'
import SkillSelector from '../ai-chat/SkillSelector.vue'

const chat = inject('aiChat')
const inputRef = ref(null)

defineExpose({ focus: () => inputRef.value?.focus() })
</script>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
