<template>
  <div class="min-h-screen bg-vmall-gray-bg text-gray-800">
    <AppHeader />
    <router-view />
    <AiChatButton @toggle="chatOpen = !chatOpen" />
    <AiChatPanel :open="chatOpen" @close="chatOpen = false" />
  </div>
</template>

<script setup>
import { ref, provide, onMounted, onUnmounted } from 'vue'
import AppHeader from './shared/ui/AppHeader.vue'
import AiChatButton from './shared/ui/ai-chat/AiChatButton.vue'
import AiChatPanel from './shared/ui/ai-chat/AiChatPanel.vue'
import { useAiChat } from './shared/composables/useAiChat.js'

const chatOpen = ref(false)
const chat = useAiChat()
provide('aiChat', chat)

onMounted(() => chat.loadModels())

function handleKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    chatOpen.value = !chatOpen.value
  }
  if (e.key === 'Escape' && chatOpen.value) {
    chatOpen.value = false
  }
}

onMounted(() => document.addEventListener('keydown', handleKeydown))
onUnmounted(() => document.removeEventListener('keydown', handleKeydown))
</script>
