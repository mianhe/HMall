<template>
  <div class="min-h-screen bg-vmall-gray-bg text-gray-800">
    <router-view />

    <AiChatButton @toggle="chatOpen = !chatOpen" />
    <AiChatPanel :open="chatOpen" @close="chatOpen = false" />
    <AppToast />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, provide, onMounted, onUnmounted } from 'vue'
import { useAiChat } from './shared/composables/useAiChat.js'
import { useToast } from './shared/composables/useToast.js'
import AiChatButton from './shared/ui/ai-chat/AiChatButton.vue'
import AiChatPanel from './shared/ui/ai-chat/AiChatPanel.vue'
import AppToast from './shared/ui/AppToast.vue'
import ConfirmDialog from './shared/ui/ConfirmDialog.vue'
import { useConfirm } from './shared/composables/useConfirm.js'

const chatOpen = ref(false)
const chat = useAiChat()
const toast = useToast()
const confirm = useConfirm()

provide('aiChat', chat)
provide('toast', toast)
provide('confirm', confirm)

onMounted(() => {
  chat.loadModels()
  chat.loadSkills()
  window.addEventListener('keydown', handleShortcut)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleShortcut)
})

function handleShortcut(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    chatOpen.value = !chatOpen.value
  }
  if (e.key === 'Escape' && chatOpen.value) {
    chatOpen.value = false
  }
}
</script>
