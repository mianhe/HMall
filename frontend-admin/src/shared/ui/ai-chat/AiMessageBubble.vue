<template>
  <div :class="['flex', msg.role === 'user' ? 'justify-end' : 'justify-start']">
    <div
      :class="[
        'max-w-[85%] rounded-2xl text-sm leading-relaxed',
        msg.role === 'user'
          ? 'bg-vmall-red text-white rounded-br-md px-4 py-2.5'
          : 'bg-white border border-gray-200 text-gray-800 rounded-bl-md shadow-sm overflow-hidden',
      ]"
    >
      <!-- User message -->
      <div v-if="msg.role === 'user' && msg.content" v-html="renderedUserContent" />

      <!-- Assistant message -->
      <template v-if="msg.role === 'assistant'">
        <!-- Tool calls (collapsible group) -->
        <div v-if="msg.toolCalls.length > 0" class="px-3 pt-2 pb-1">
          <AiToolCallGroup :tool-calls="msg.toolCalls" />
        </div>

        <!-- Thinking (collapsible, dimmed) -->
        <div v-if="sections.thinking" class="px-4 pt-2 pb-1">
          <button
            @click="thinkingExpanded = !thinkingExpanded"
            class="flex items-center gap-1.5 text-xs text-gray-400 hover:text-gray-500 transition-colors"
          >
            <svg
              :class="['w-3 h-3 transition-transform', thinkingExpanded && 'rotate-90']"
              fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
            </svg>
            <span>思考过程</span>
          </button>
          <Transition name="collapse">
            <div
              v-if="thinkingExpanded"
              class="mt-1.5 pl-4 border-l-2 border-gray-100 text-xs text-gray-400 leading-relaxed"
              v-html="renderMd(sections.thinking)"
            />
          </Transition>
        </div>

        <!-- Divider between process and conclusion -->
        <div
          v-if="sections.answer && (msg.toolCalls.length > 0 || sections.thinking)"
          class="mx-4 border-t border-gray-100"
        />

        <!-- Main answer (prominent) -->
        <div
          v-if="sections.answer"
          class="px-4 py-2.5 prose prose-sm max-w-none prose-table:text-xs prose-th:py-1 prose-td:py-1"
          v-html="renderMd(sections.answer)"
        />

        <!-- Follow-up suggestion -->
        <div
          v-if="sections.suggestion"
          class="mx-4 mb-2.5 pt-2 border-t border-gray-100 text-xs text-gray-500 leading-relaxed"
          v-html="renderMd(sections.suggestion)"
        />
      </template>

      <!-- Loading indicator -->
      <span
        v-if="msg.loading && !msg.content && msg.toolCalls.length === 0"
        class="inline-flex gap-1 px-4 py-2.5"
      >
        <span class="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce" style="animation-delay: 0ms" />
        <span class="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce" style="animation-delay: 150ms" />
        <span class="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce" style="animation-delay: 300ms" />
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import AiToolCallGroup from './AiToolCallGroup.vue'

const props = defineProps({ msg: Object })
const thinkingExpanded = ref(false)

marked.setOptions({ breaks: true, gfm: true })

function renderMd(text) {
  if (!text) return ''
  return DOMPurify.sanitize(marked.parse(text))
}

const sections = computed(() => {
  const content = props.msg.content || ''
  if (props.msg.role !== 'assistant') return { thinking: '', answer: content, suggestion: '' }

  let thinking = ''
  let rest = content

  const thinkRegex = /<think>([\s\S]*?)(?:<\/think>|$)/g
  const thinkParts = []
  let match
  while ((match = thinkRegex.exec(content)) !== null) {
    thinkParts.push(match[1].trim())
  }
  if (thinkParts.length > 0) {
    thinking = thinkParts.join('\n')
    rest = content.replace(/<think>[\s\S]*?(?:<\/think>|$)/g, '').trim()
  }

  let answer = rest
  let suggestion = ''
  const sepIndex = rest.indexOf('\n---\n')
  if (sepIndex !== -1) {
    answer = rest.substring(0, sepIndex).trim()
    suggestion = rest.substring(sepIndex + 5).trim()
  }

  return { thinking, answer, suggestion }
})

const renderedUserContent = computed(() => {
  if (!props.msg.content) return ''
  return props.msg.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})
</script>

<style scoped>
.collapse-enter-active, .collapse-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.collapse-enter-from, .collapse-leave-to {
  opacity: 0;
  max-height: 0;
}
.collapse-enter-to, .collapse-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
