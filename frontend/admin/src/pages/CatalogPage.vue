<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">Catalog</h1>
        <button
          @click="load"
          :disabled="loading"
          class="px-4 py-2 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors disabled:opacity-50"
        >
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>
      <div v-if="error" class="text-vmall-red mb-4">{{ error }}</div>
      <CatalogTree v-if="tree.length" :nodes="tree" />
      <p v-else-if="!loading && !error" class="text-vmall-gray-text">暂无数据，请通过 MCP 添加类目与商品。</p>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import CatalogTree from '../shared/ui/CatalogTree.vue'
import { getCategoryTree } from '../shared/api/catalog.js'

const tree = ref([])
const loading = ref(false)
const error = ref('')

function errorMessage(e) {
  const status = e.response?.status
  const msg = e.response?.data?.message || e.message || ''
  if (status === 502 || (msg && msg.includes('proxy'))) {
    return 'BFF 代理失败，请确认 Catalog 服务已启动（可执行 ./scripts/hmall.sh start）'
  }
  if (!e.response && (e.code === 'ERR_NETWORK' || e.message?.includes('Network'))) {
    return '无法连接后端，请确认 BFF 已启动（端口 8085）'
  }
  if (e.code === 'ECONNABORTED' || e.message?.includes('timeout')) {
    return '请求超时，请检查 BFF 与 Catalog 服务是否正常运行'
  }
  if (status === 500 && (!msg || msg.includes('status code'))) {
    return '后端可能仍在启动，请稍后点击刷新'
  }
  return msg || '加载失败'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    tree.value = await getCategoryTree()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
