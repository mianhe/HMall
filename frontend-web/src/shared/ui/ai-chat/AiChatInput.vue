<template>
  <div class="border-t border-gray-200 bg-white p-3">
    <div class="flex gap-2">
      <textarea
        ref="inputRef"
        v-model="input"
        @keydown.enter.exact.prevent="submit"
        :disabled="disabled"
        rows="1"
        :placeholder="disabled ? 'AI 正在思考...' : '输入消息，Enter 发送'"
        class="flex-1 resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm
               focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red
               disabled:bg-gray-50 disabled:text-gray-400
               overflow-hidden"
        style="min-height: 38px; max-height: 120px;"
        @input="autoResize"
      />
      <button
        @click="submit"
        :disabled="disabled || !input.trim()"
        class="self-end px-4 py-2 rounded-lg bg-vmall-red text-white text-sm font-medium
               hover:bg-vmall-red-hover disabled:opacity-40 disabled:cursor-not-allowed
               transition-colors"
      >
        发送
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({ disabled: Boolean })
const emit = defineEmits(['send'])

const input = ref('')
const inputRef = ref(null)

function submit() {
  if (!input.value.trim() || props.disabled) return
  emit('send', input.value)
  input.value = ''
  nextTick(() => autoResize())
}

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function focus() {
  inputRef.value?.focus()
}

defineExpose({ focus })
</script>
