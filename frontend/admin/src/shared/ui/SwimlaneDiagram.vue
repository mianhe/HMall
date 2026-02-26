<template>
  <div :class="fullscreen ? 'fixed inset-0 z-50 bg-white flex flex-col' : 'relative'">
    <!-- 回放控制 -->
    <div :class="['flex items-center gap-3 flex-wrap', fullscreen ? 'px-6 py-3 border-b border-gray-200 shrink-0' : 'mb-4']">
      <button
        @click="togglePlayback"
        class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-sm font-medium transition-colors"
        :class="playing
          ? 'border-vmall-red text-vmall-red hover:bg-red-50'
          : 'border-vmall-gray-border text-gray-600 hover:bg-gray-50'"
      >
        <svg v-if="!playing" class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M8 5v14l11-7z" />
        </svg>
        <svg v-else class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
        </svg>
        {{ playing ? '暂停' : visibleCount < events.length ? '继续回放' : '重新回放' }}
      </button>
      <button
        v-if="visibleCount < events.length && !playing"
        @click="showAll"
        class="text-sm text-gray-500 hover:text-vmall-red transition-colors"
      >显示全部</button>
      <span class="text-xs text-gray-400">{{ visibleCount }} / {{ events.length }} 事件</span>
      <div class="flex items-center gap-1.5">
        <span class="text-xs text-gray-400">速度</span>
        <button
          v-for="s in SPEED_OPTIONS"
          :key="s.value"
          @click="speed = s.value"
          :class="[
            'px-2 py-0.5 rounded text-xs font-medium transition-colors',
            speed === s.value ? 'bg-vmall-red text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
          ]"
        >{{ s.label }}</button>
      </div>
      <div class="flex items-center gap-3 text-xs text-gray-400 border-l border-gray-200 pl-3 ml-1">
        <span class="inline-flex items-center gap-1">
          <span class="w-3 h-3 rounded border-2 border-amber-300 bg-amber-50" />
          异常
        </span>
        <span class="inline-flex items-center gap-1">
          <span class="w-3 h-3 rounded border-2 border-red-300 bg-red-50" />
          补偿
        </span>
      </div>
      <div class="ml-auto flex items-center gap-2">
        <button
          v-if="fullscreen"
          @click="toggleFitToScreen"
          class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border text-sm font-medium transition-colors"
          :class="fitToScreen
            ? 'border-vmall-red bg-red-50 text-vmall-red'
            : 'border-vmall-gray-border text-gray-600 hover:bg-gray-50'"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
          {{ fitToScreen ? '滚动视图' : '一屏总览' }}
        </button>
        <button
          @click="toggleFullscreen"
          class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
        >
          <svg v-if="!fullscreen" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 9V4H4m0 0l5 5M9 15v5H4m0 0l5-5m6-5V4h5m0 0l-5 5m5 6v5h-5m0 0l5-5" />
          </svg>
          {{ fullscreen ? '退出全屏' : '全屏' }}
        </button>
      </div>
    </div>

    <!-- 泳道区域 -->
    <div
      ref="diagramRef"
      :class="[
        'relative',
        fullscreen ? 'flex-1 overflow-hidden' : 'rounded-lg border border-vmall-gray-border overflow-hidden'
      ]"
      :style="{ height: fullscreen ? undefined : (containerHeight + SCROLLBAR_RESERVE) + 'px' }"
    >
      <!-- 左侧固定标签列 -->
      <div class="absolute left-0 top-0 bottom-0 z-30 bg-gray-50 border-r border-gray-200" :style="{ width: LABEL_WIDTH + 'px' }">
        <div
          v-for="(lane, idx) in LANES"
          :key="'label-' + lane.bc"
          class="flex items-center px-3 border-b border-gray-100"
          :style="{ height: laneHeight + 'px' }"
        >
          <span class="text-xs font-medium text-gray-600 whitespace-nowrap">{{ lane.label }}</span>
        </div>
      </div>

      <!-- 可滚动内容区域 -->
      <div
        ref="scrollRef"
        class="absolute top-0 bottom-0"
        :class="fitActive ? 'overflow-hidden' : 'overflow-x-auto overflow-y-hidden'"
        :style="{ left: LABEL_WIDTH + 'px', right: '0' }"
      >
        <div class="relative" :style="{ width: scrollContentWidth + 'px', height: (containerHeight + SCROLLBAR_RESERVE) + 'px' }">
          <!-- 泳道背景条纹 -->
          <div
            v-for="(lane, idx) in LANES"
            :key="'bg-' + lane.bc"
            class="absolute left-0 right-0 border-b border-gray-100"
            :class="idx % 2 === 0 ? 'bg-white' : 'bg-gray-50/40'"
            :style="{ top: laneTop(idx) + 'px', height: laneHeight + 'px' }"
          />

          <!-- SVG 连线层 -->
          <svg
            class="absolute top-0 left-0 pointer-events-none z-10"
            :width="scrollContentWidth"
            :height="containerHeight"
          >
            <template v-for="(edge, eIdx) in visibleEdges" :key="'edge-' + eIdx">
              <path
                :d="edgePath(edge)"
                fill="none"
                :stroke="edge.isCompensation ? '#FCA5A5' : '#CBD5E1'"
                :stroke-width="edge.isCompensation ? 2 : 1.5"
                :stroke-dasharray="edge.isCompensation ? '6 4' : 'none'"
                class="transition-opacity duration-500"
                :opacity="edge.visible ? 1 : 0"
              />
              <polygon
                :points="arrowPoints(edge)"
                :fill="edge.isCompensation ? '#FCA5A5' : '#CBD5E1'"
                class="transition-opacity duration-500"
                :opacity="edge.visible ? 1 : 0"
              />
            </template>
          </svg>

          <!-- 事件卡片节点 -->
          <div
            v-for="node in visibleNodes"
            :key="node.eventId"
            class="absolute z-20 transition-all duration-500 cursor-pointer"
            :style="{ left: node.x + 'px', top: node.y + 'px', width: cardWidth + 'px' }"
            :class="node.visible ? 'opacity-100 scale-100' : 'opacity-0 scale-90'"
            @click="$emit('select', node)"
          >
            <div
              class="rounded-lg border-2 px-2 py-2 shadow-sm hover:shadow-md transition-shadow text-center overflow-hidden"
              :class="cardClass(node)"
            >
              <div class="flex items-center justify-center gap-1">
                <span
                  class="inline-flex items-center justify-center w-5 h-5 rounded-full text-[10px] font-bold text-white shrink-0"
                  :style="{ backgroundColor: numberBg(node) }"
                >{{ node.order }}</span>
                <span class="text-xs font-semibold truncate" :class="textClass(node)">{{ node.label }}</span>
              </div>
              <div class="text-[10px] mt-0.5 truncate" :class="node.isCompensation ? 'text-red-400' : 'text-gray-400'">
                {{ formatShortTime(node.occurredAt) }}
              </div>
              <div v-if="node.isCompensation" class="text-[10px] text-red-500 font-medium mt-0.5">
                ↩ 补偿
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  events: { type: Array, required: true },
})

defineEmits(['select'])

const LANES = [
  { bc: 'Order',       label: 'Order' },
  { bc: 'Inventory',   label: 'Inventory' },
  { bc: 'Payment',     label: 'Payment' },
  { bc: 'Fulfillment', label: 'Fulfillment' },
]

const LANE_HEIGHT_NORMAL = 88
const LANE_HEIGHT_FULL = 120
const CARD_W_NORMAL = 120
const CARD_W_FULL = 140
const MIN_FIT_CARD_W = 64
const CARD_H = 56
const NODE_GAP = 32
const MIN_FIT_GAP = 10
const LABEL_WIDTH = 96
const CONTENT_PADDING = 24
const SCROLLBAR_RESERVE = 14
const TOOLBAR_HEIGHT = 52

const SPEED_OPTIONS = [
  { label: '0.5x', value: 2000 },
  { label: '1x', value: 1000 },
  { label: '2x', value: 500 },
  { label: '4x', value: 250 },
]

const scrollRef = ref(null)
const diagramRef = ref(null)
const fullscreen = ref(false)
const fitToScreen = ref(false)
const playing = ref(false)
const visibleCount = ref(0)
const speed = ref(1000)
const winW = ref(window.innerWidth)
const winH = ref(window.innerHeight)
let playTimer = null

const fitActive = computed(() => fitToScreen.value && fullscreen.value)

const laneHeight = computed(() => {
  if (fitActive.value) {
    return Math.max(60, (winH.value - TOOLBAR_HEIGHT) / LANES.length)
  }
  return fullscreen.value ? LANE_HEIGHT_FULL : LANE_HEIGHT_NORMAL
})

const cardWidth = computed(() => {
  if (fitActive.value && props.events.length > 0) {
    const availW = winW.value - LABEL_WIDTH - 2 * CONTENT_PADDING
    const n = props.events.length
    const cw = Math.floor((availW - MIN_FIT_GAP * Math.max(0, n - 1)) / n)
    return Math.max(MIN_FIT_CARD_W, Math.min(CARD_W_FULL, cw))
  }
  return fullscreen.value ? CARD_W_FULL : CARD_W_NORMAL
})

const nodeGap = computed(() => {
  if (fitActive.value && props.events.length > 1) {
    const availW = winW.value - LABEL_WIDTH - 2 * CONTENT_PADDING
    const n = props.events.length
    const totalGap = availW - n * cardWidth.value
    return Math.max(MIN_FIT_GAP, totalGap / (n - 1))
  }
  return NODE_GAP
})

const containerHeight = computed(() => LANES.length * laneHeight.value)

function laneTop(idx) { return idx * laneHeight.value }
function laneCenterY(idx) { return idx * laneHeight.value + laneHeight.value / 2 }
function laneIndex(bc) { return LANES.findIndex(l => l.bc === bc) }

const allNodes = computed(() =>
  props.events.map((ev, idx) => {
    const bc = ev.bcLabel || 'Order'
    const li = laneIndex(bc)
    const cw = cardWidth.value
    const gap = nodeGap.value
    const x = CONTENT_PADDING + idx * (cw + gap)
    const y = laneCenterY(li) - CARD_H / 2
    return {
      ...ev, bc, laneIdx: li, x, y,
      cx: x + cw / 2, cy: laneCenterY(li),
      order: idx + 1,
    }
  })
)

const visibleNodes = computed(() =>
  allNodes.value.map((n, idx) => ({ ...n, visible: idx < visibleCount.value }))
)

const allEdges = computed(() => {
  const edges = []
  const nodes = allNodes.value
  const cw = cardWidth.value
  for (let i = 1; i < nodes.length; i++) {
    const from = nodes[i - 1], to = nodes[i]
    const x1 = from.x + cw, y1 = from.cy
    const x2 = to.x,         y2 = to.cy
    const midX = (x1 + x2) / 2
    edges.push({ x1, y1, x2, y2, midX, isCompensation: to.isCompensation, eventIdx: i })
  }
  return edges
})

const visibleEdges = computed(() =>
  allEdges.value.map(e => ({ ...e, visible: e.eventIdx < visibleCount.value }))
)

const scrollContentWidth = computed(() => {
  if (!allNodes.value.length) return 600
  const last = allNodes.value[allNodes.value.length - 1]
  return last.x + cardWidth.value + CONTENT_PADDING
})

function cardClass(node) {
  if (node.isCompensation) return 'bg-red-50 border-red-300'
  if (node.isException) return 'bg-amber-50 border-amber-300'
  return 'bg-white border-gray-200'
}

function textClass(node) {
  if (node.isCompensation) return 'text-red-700'
  if (node.isException) return 'text-amber-700'
  return 'text-gray-800'
}

function numberBg(node) {
  if (node.isCompensation) return '#EF4444'
  if (node.isException) return '#F59E0B'
  return '#6B7280'
}

function edgePath(edge) {
  const { x1, y1, x2, y2, midX } = edge
  if (y1 === y2) return `M${x1},${y1} L${x2},${y2}`
  return `M${x1},${y1} L${midX},${y1} L${midX},${y2} L${x2},${y2}`
}

function arrowPoints(edge) {
  const size = 5
  const { x2, y2 } = edge
  return `${x2},${y2} ${x2 - size * 2},${y2 - size} ${x2 - size * 2},${y2 + size}`
}

function formatShortTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function scrollToNode(idx) {
  if (fitActive.value) return
  const el = scrollRef.value
  if (!el || idx < 0) return
  const node = allNodes.value[idx]
  if (!node) return
  const targetLeft = node.x + cardWidth.value / 2 - el.clientWidth / 2
  el.scrollTo({ left: Math.max(0, targetLeft), behavior: 'smooth' })
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
  if (!fullscreen.value) fitToScreen.value = false
}

function toggleFitToScreen() {
  fitToScreen.value = !fitToScreen.value
  if (fitToScreen.value) {
    pause()
    visibleCount.value = props.events.length
  }
}

function onKeydown(e) {
  if (e.key === 'Escape' && fullscreen.value) {
    fullscreen.value = false
    fitToScreen.value = false
  }
}

function onResize() {
  winW.value = window.innerWidth
  winH.value = window.innerHeight
}

function togglePlayback() {
  if (playing.value) { pause() }
  else {
    if (visibleCount.value >= props.events.length) visibleCount.value = 0
    if (fitToScreen.value) fitToScreen.value = false
    play()
  }
}

function play() { playing.value = true; step() }

function pause() {
  playing.value = false
  if (playTimer) { clearTimeout(playTimer); playTimer = null }
}

function step() {
  if (!playing.value) return
  if (visibleCount.value >= props.events.length) { playing.value = false; return }
  visibleCount.value++
  nextTick(() => scrollToNode(visibleCount.value - 1))
  playTimer = setTimeout(step, speed.value)
}

function showAll() {
  pause()
  visibleCount.value = props.events.length
  nextTick(() => scrollToNode(0))
}

function eventListSignature(events) {
  if (!events?.length) return ''
  return events.length + '-' + (events[0]?.eventId ?? '')
}

watch(() => props.events, (newEvents, oldEvents) => {
  const newSig = eventListSignature(newEvents)
  const oldSig = eventListSignature(oldEvents)
  if (newSig && newSig === oldSig) return
  fitToScreen.value = false
  pause()
  visibleCount.value = 0
  if (newEvents?.length > 0) {
    nextTick(() => play())
  }
}, { deep: true })

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', onResize)
  if (props.events.length > 0) {
    nextTick(() => play())
  }
})
onUnmounted(() => {
  if (playTimer) clearTimeout(playTimer)
  document.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onResize)
})
</script>
