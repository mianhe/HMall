<template>
  <div class="flex flex-col h-screen overflow-hidden">
    <AppHeader />

    <div class="flex flex-1 overflow-hidden">
      <!-- 主内容区（固定指标栏 + 动态画布） -->
      <div class="flex-1 flex flex-col overflow-hidden">
        <!-- 顶部标题栏（无今日数据，避免干扰画布） -->
        <div class="flex-shrink-0 bg-white border-b border-gray-200 px-6 py-3">
          <div class="flex items-center justify-between">
            <h1 class="text-base font-semibold text-gray-800">智能运营</h1>
            <button
              v-if="!todayStats || todayStats.ordersCreated === 0"
              @click="handleSeed"
              :disabled="seeding"
              class="px-3 py-1 text-xs rounded-md border transition-colors"
              :class="seeding
                ? 'border-gray-200 text-gray-400 cursor-not-allowed'
                : 'border-vmall-red/30 text-vmall-red hover:bg-vmall-red/5'"
            >
              {{ seeding ? '生成中…' : '生成演示数据' }}
            </button>
          </div>
        </div>

        <!-- 动态画布 -->
        <div class="flex-1 overflow-auto px-6 py-4">
          <!-- EMPTY 欢迎引导 -->
          <div v-if="canvas.phase.value === 'EMPTY'" class="flex flex-col items-center justify-center h-full text-center">
            <div class="w-16 h-16 rounded-2xl bg-vmall-red/10 flex items-center justify-center mb-4">
              <svg class="w-8 h-8 text-vmall-red" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                      d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
              </svg>
            </div>
            <h2 class="text-lg font-semibold text-gray-700 mb-2">与 AI 对话，驱动数据视图</h2>
            <p class="text-sm text-gray-400 max-w-xs mb-4">在右侧对话框输入问题，AI 将在此区域渲染对应的数据可视化</p>
            <div class="space-y-2 text-left">
              <div
                v-for="hint in hints"
                :key="hint"
                @click="sendHint(hint)"
                class="px-4 py-2 bg-white border border-gray-200 rounded-lg text-sm text-gray-600 cursor-pointer hover:border-vmall-red/40 hover:bg-vmall-red/5 transition-colors"
              >
                {{ hint }}
              </div>
            </div>
          </div>

          <!-- LOADING 状态（无面板时显示居中加载） -->
          <div v-else-if="canvas.phase.value === 'LOADING' && canvas.panels.value.length === 0"
               class="flex items-center justify-center h-full">
            <div class="flex items-center gap-2 text-sm text-gray-500">
              <svg class="w-4 h-4 animate-spin text-vmall-red" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
              </svg>
              AI 正在查询数据…
            </div>
          </div>

          <!-- 多面板渲染 -->
          <div v-else class="relative">
            <!-- Loading 遮罩（面板已有但仍在加载更多） -->
            <div v-if="canvas.phase.value === 'LOADING'"
                 class="absolute top-2 right-2 z-10 flex items-center gap-1.5 px-3 py-1.5 bg-white/90 backdrop-blur-sm rounded-full shadow-sm border border-gray-200 text-xs text-gray-500">
              <svg class="w-3.5 h-3.5 animate-spin text-vmall-red" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
              </svg>
              加载中…
            </div>

            <!-- 面板网格 -->
            <div :class="gridClass">
              <div
                v-for="(panel, i) in canvas.panels.value"
                :key="i"
                :class="panelClass(panel, i)"
                class="transition-all duration-300"
              >
                <component
                  :is="resolveComponent(panel)"
                  v-bind="resolveProps(panel)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 常驻 AI 侧边栏 -->
      <div class="w-80 xl:w-96 flex-shrink-0 flex flex-col border-l border-gray-200 overflow-hidden">
        <OpsAiSidebar ref="sidebarRef" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted, onUnmounted, watchEffect } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import OpsStatsPanel from '../shared/ui/ops/OpsStatsPanel.vue'
import OpsActivityTimeline from '../shared/ui/ops/OpsActivityTimeline.vue'
import OpsRecentList from '../shared/ui/ops/OpsRecentList.vue'
import OpsChart from '../shared/ui/ops/OpsChart.vue'
import OpsTable from '../shared/ui/ops/OpsTable.vue'
import OpsAiSidebar from '../shared/ui/ops/OpsAiSidebar.vue'
import { useOpsCanvas } from '../shared/composables/useOpsCanvas.js'
import { getActivityStats, seedActivities } from '../shared/api/activity.js'

const chat = inject('aiChat')
const canvas = useOpsCanvas()
const sidebarRef = ref(null)

const todayStats = ref(null)
const seeding = ref(false)

const hints = [
  '过去 7 天的销售趋势',
  '今天的整体情况如何？',
  '给我看用户 1 的事件序列',
  '最近发生了什么？',
]

const CHART_TYPES = new Set(['LINE_CHART', 'BAR_CHART', 'PIE_CHART'])
const COMPACT_TYPES = new Set(['PIE_CHART'])

const gridClass = computed(() => {
  const count = canvas.panels.value.length
  if (count <= 1) return 'space-y-4'
  return 'grid gap-4 grid-cols-1 lg:grid-cols-2'
})

function panelClass(panel, index) {
  const count = canvas.panels.value.length
  if (count <= 1) return ''
  const isFullWidth = !COMPACT_TYPES.has(panel.type)
  if (isFullWidth && count > 1) return 'lg:col-span-2'
  return ''
}

function resolveComponent(panel) {
  if (CHART_TYPES.has(panel.type)) return OpsChart
  if (panel.type === 'STAT_CARDS') return OpsStatsPanel
  if (panel.type === 'TIMELINE') return OpsActivityTimeline
  if (panel.type === 'EVENT_LIST') return OpsRecentList
  if (panel.type === 'TABLE') return OpsTable
  return null
}

function resolveProps(panel) {
  if (CHART_TYPES.has(panel.type)) {
    return { chartType: panel.type.toLowerCase(), title: panel.title, data: panel.data }
  }
  if (panel.type === 'STAT_CARDS') return { data: panel.data || {} }
  if (panel.type === 'TIMELINE') return { items: panel.data?.items || panel.data || [], dimension: panel.data?.dimension }
  if (panel.type === 'EVENT_LIST') return { items: panel.data?.items || panel.data || [] }
  if (panel.type === 'TABLE') return { title: panel.title, columns: panel.data?.columns || [], rows: panel.data?.rows || [] }
  return {}
}

async function loadTodayStats() {
  try {
    todayStats.value = await getActivityStats({ period: 'today' })
  } catch (e) {
    console.warn('Failed to load today stats:', e)
  }
}

function sendHint(text) {
  chat.sendMessage(text)
}

async function handleSeed() {
  seeding.value = true
  try {
    const result = await seedActivities({ days: 30, ordersPerDay: 5 })
    console.log('Seed result:', result)
    await loadTodayStats()
  } catch (e) {
    console.error('Seed failed:', e)
  } finally {
    seeding.value = false
  }
}

watchEffect(() => {
  const panels = canvas.panels.value
  if (panels.length > 0) {
    chat.contextExtras.value = {
      canvasPanels: panels.map(p => ({ type: p.type, title: p.title })),
    }
  } else {
    chat.contextExtras.value = {}
  }
})

let unregisterStart = null
let unregisterSuccess = null
let unregisterDone = null

onMounted(() => {
  loadTodayStats()
  unregisterStart = chat.onToolCallStart((toolName) => {
    canvas.onToolCall(toolName)
  })
  unregisterSuccess = chat.onToolCallSuccess((toolName, result) => {
    canvas.onToolResult(toolName, result)
  })
  unregisterDone = chat.onDone(() => {
    canvas.onTurnEnd()
  })
})

onUnmounted(() => {
  if (unregisterStart) unregisterStart()
  if (unregisterSuccess) unregisterSuccess()
  if (unregisterDone) unregisterDone()
})
</script>
