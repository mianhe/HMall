<template>
  <!-- Single tool call: inline card -->
  <AiToolCallCard v-if="toolCalls.length === 1" :tool-call="toolCalls[0]" />

  <!-- Multiple tool calls: collapsible summary -->
  <div v-else-if="toolCalls.length > 1" class="my-1.5 rounded-lg border border-gray-200 bg-gray-50/80 text-xs overflow-hidden">
    <button
      @click="expanded = !expanded"
      class="w-full flex items-center gap-2 px-3 py-2 hover:bg-gray-100 transition-colors text-left"
    >
      <span v-if="allDone" class="text-green-600 flex-shrink-0">&#10003;</span>
      <span v-else class="w-3 h-3 rounded-full border-2 border-vmall-red border-t-transparent animate-spin flex-shrink-0" />

      <span class="text-gray-600 flex-1">
        已调用 <strong class="text-gray-700">{{ toolCalls.length }}</strong> 个工具
        <span v-if="allDone" class="text-gray-400 ml-1">（{{ uniqueToolNames }}）</span>
      </span>

      <svg
        :class="['w-4 h-4 text-gray-400 transition-transform flex-shrink-0', expanded && 'rotate-180']"
        fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <div v-if="expanded" class="border-t border-gray-200 px-2 py-1.5 space-y-1">
      <AiToolCallCard v-for="tc in toolCalls" :key="tc.id" :tool-call="tc" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import AiToolCallCard from './AiToolCallCard.vue'

const props = defineProps({ toolCalls: Array })
const expanded = ref(false)

const allDone = computed(() =>
  props.toolCalls.every(tc => tc.status === 'success' || tc.status === 'error')
)

const uniqueToolNames = computed(() => {
  const names = [...new Set(props.toolCalls.map(tc => tc.name))]
  if (names.length <= 3) return names.join('、')
  return names.slice(0, 3).join('、') + ` 等 ${names.length} 种`
})
</script>
