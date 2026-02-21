<template>
  <div :class="['flex', msg.role === 'user' ? 'justify-end' : 'justify-start']">
    <div
      :class="[
        'max-w-[85%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed',
        msg.role === 'user'
          ? 'bg-vmall-red text-white rounded-br-md'
          : 'bg-white border border-gray-200 text-gray-800 rounded-bl-md shadow-sm',
      ]"
    >
      <!-- Tool calls -->
      <AiToolCallCard
        v-for="tc in msg.toolCalls"
        :key="tc.id"
        :tool-call="tc"
      />

      <!-- User message -->
      <div v-if="msg.role === 'user' && msg.content" v-html="renderedUserContent" />

      <!-- Assistant structured content -->
      <template v-if="msg.role === 'assistant' && msg.content">
        <!-- Thinking (collapsible) -->
        <div v-if="sections.thinking" class="mb-2">
          <button
            @click="thinkingExpanded = !thinkingExpanded"
            class="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-500 transition-colors"
          >
            <svg
              :class="['w-3 h-3 transition-transform', thinkingExpanded && 'rotate-90']"
              fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
            </svg>
            <span>思考过程</span>
          </button>
          <div
            v-if="thinkingExpanded"
            class="mt-1 pl-4 border-l-2 border-gray-200 text-xs text-gray-400 leading-relaxed"
            v-html="renderMd(sections.thinking)"
          />
        </div>

        <!-- Main answer -->
        <div
          v-if="sections.answer"
          class="prose prose-sm max-w-none"
          v-html="renderMd(sections.answer)"
        />

        <!-- Follow-up suggestion -->
        <div
          v-if="sections.suggestion"
          class="mt-2 pt-2 border-t border-gray-100 text-xs text-gray-500 leading-relaxed"
          v-html="renderMd(sections.suggestion)"
        />
      </template>

      <!-- Loading indicator -->
      <span
        v-if="msg.loading && !msg.content && msg.toolCalls.length === 0"
        class="inline-flex gap-1"
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
import AiToolCallCard from './AiToolCallCard.vue'

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
