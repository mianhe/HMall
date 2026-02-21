<template>
  <div ref="scrollContainer" class="flex-1 overflow-y-auto px-4 py-4 space-y-3">
    <template v-if="messages.length === 0">
      <div class="h-full flex flex-col items-center justify-center text-gray-400 text-sm">
        <svg class="w-12 h-12 mb-3 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
        </svg>
        <p>你好，我是 HMall 智能助手</p>
        <p class="text-xs text-gray-300 mt-1">输入消息开始对话，我可以帮你管理商品目录</p>
      </div>
    </template>

    <AiMessageBubble
      v-for="msg in messages"
      :key="msg.id"
      :msg="msg"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import AiMessageBubble from './AiMessageBubble.vue'

const props = defineProps({ messages: Array })
const scrollContainer = ref(null)

watch(
  () => props.messages.length,
  () => nextTick(scrollToBottom),
)

watch(
  () => {
    const last = props.messages[props.messages.length - 1]
    return last?.content?.length
  },
  () => nextTick(scrollToBottom),
)

function scrollToBottom() {
  const el = scrollContainer.value
  if (el) el.scrollTop = el.scrollHeight
}
</script>
