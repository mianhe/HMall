<template>
  <div class="my-1 rounded-lg border border-gray-200 bg-gray-50 text-xs overflow-hidden">
    <button
      @click="expanded = !expanded"
      class="w-full flex items-center gap-2 px-3 py-2 hover:bg-gray-100 transition-colors text-left"
    >
      <span v-if="toolCall.status === 'calling'" class="w-3 h-3 rounded-full border-2 border-vmall-red border-t-transparent animate-spin" />
      <span v-else-if="toolCall.status === 'success'" class="text-green-600">&#10003;</span>
      <span v-else class="text-red-500">&#10007;</span>

      <span class="font-mono text-gray-700 flex-1 truncate">{{ toolCall.name }}</span>

      <svg
        :class="['w-4 h-4 text-gray-400 transition-transform', expanded && 'rotate-180']"
        fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <div v-if="expanded" class="border-t border-gray-200 px-3 py-2 space-y-2">
      <div v-if="toolCall.arguments">
        <div class="text-gray-500 mb-1">参数</div>
        <pre class="bg-white rounded p-2 overflow-x-auto text-[11px] leading-relaxed">{{ formatJson(toolCall.arguments) }}</pre>
      </div>
      <div v-if="toolCall.result">
        <div class="text-gray-500 mb-1">结果</div>
        <pre class="bg-white rounded p-2 overflow-x-auto text-[11px] leading-relaxed whitespace-pre-wrap">{{ toolCall.result }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({ toolCall: Object })
const expanded = ref(false)

function formatJson(obj) {
  try {
    return typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}
</script>
