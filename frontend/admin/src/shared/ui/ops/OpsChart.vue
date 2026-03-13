<template>
  <div>
    <div v-if="title" class="text-sm font-medium text-gray-600 mb-4">{{ title }}</div>
    <div class="bg-white rounded-lg border border-vmall-gray-border p-5">
      <canvas ref="canvasRef" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const props = defineProps({
  chartType: { type: String, required: true },
  title: { type: String, default: '' },
  data: { type: Object, required: true },
})

const canvasRef = ref(null)
let chartInstance = null

const COLORS = [
  { bg: 'rgba(220, 38, 38, 0.15)', border: 'rgb(220, 38, 38)' },
  { bg: 'rgba(59, 130, 246, 0.15)', border: 'rgb(59, 130, 246)' },
  { bg: 'rgba(16, 185, 129, 0.15)', border: 'rgb(16, 185, 129)' },
  { bg: 'rgba(245, 158, 11, 0.15)', border: 'rgb(245, 158, 11)' },
  { bg: 'rgba(139, 92, 246, 0.15)', border: 'rgb(139, 92, 246)' },
]

const PIE_COLORS = [
  'rgb(220, 38, 38)', 'rgb(59, 130, 246)', 'rgb(16, 185, 129)',
  'rgb(245, 158, 11)', 'rgb(139, 92, 246)', 'rgb(236, 72, 153)',
]

function buildConfig() {
  const type = props.chartType === 'line_chart' ? 'line'
    : props.chartType === 'bar_chart' ? 'bar'
    : props.chartType === 'pie_chart' ? 'pie'
    : 'line'

  if (type === 'pie') {
    const items = props.data.items || []
    return {
      type: 'pie',
      data: {
        labels: items.map(i => i.label),
        datasets: [{
          data: items.map(i => i.value),
          backgroundColor: PIE_COLORS.slice(0, items.length),
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: { legend: { position: 'bottom' } },
      },
    }
  }

  const labels = props.data.labels || []
  const series = props.data.series || []
  return {
    type,
    data: {
      labels,
      datasets: series.map((s, i) => ({
        label: s.name,
        data: s.values,
        backgroundColor: COLORS[i % COLORS.length].bg,
        borderColor: COLORS[i % COLORS.length].border,
        borderWidth: type === 'line' ? 2 : 1,
        fill: type === 'line',
        tension: 0.3,
        pointRadius: type === 'line' ? 4 : 0,
        pointHoverRadius: type === 'line' ? 6 : 0,
      })),
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      interaction: { intersect: false, mode: 'index' },
      plugins: {
        legend: { display: series.length > 1, position: 'top' },
      },
      scales: {
        y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } },
        x: { grid: { display: false } },
      },
    },
  }
}

function renderChart() {
  if (chartInstance) {
    chartInstance.destroy()
    chartInstance = null
  }
  if (!canvasRef.value) return
  chartInstance = new Chart(canvasRef.value, buildConfig())
}

onMounted(() => nextTick(renderChart))

watch(() => [props.chartType, props.data], () => nextTick(renderChart), { deep: true })

onUnmounted(() => {
  if (chartInstance) chartInstance.destroy()
})
</script>
