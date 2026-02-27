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
import { getCategories, getProducts, getDimensions, getSkus, getServiceBindings } from '../shared/api/catalog.js'

const tree = ref([])
const loading = ref(false)
const error = ref('')

function isServerError(e) {
  const status = e.response?.status
  return status >= 500 || status === 502 || status === 503
}

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

async function load(opts = {}) {
  const { retries = 0 } = opts
  loading.value = true
  error.value = ''
  let lastErr = null
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const roots = await getCategories(null)
      tree.value = await Promise.all(roots.map((c) => loadCategoryNode(c)))
      return
    } catch (e) {
      lastErr = e
      if (attempt < retries && isServerError(e)) {
        await new Promise((r) => setTimeout(r, 2000))
        continue
      }
      error.value = errorMessage(e)
      return
    } finally {
      if (attempt === retries || !lastErr || !isServerError(lastErr)) {
        loading.value = false
      }
    }
  }
  loading.value = false
  if (lastErr) error.value = errorMessage(lastErr)
}

async function loadCategoryNode(category) {
  const [children, products] = await Promise.all([
    getCategories(category.id),
    getProducts(category.id),
  ])
  const childNodes = await Promise.all(children.map((c) => loadCategoryNode(c)))
  const productNodes = await Promise.all(
    products.map(async (p) => {
      const [dims, skus] = await Promise.all([
        getDimensions(p.id),
        getSkus(p.id),
      ])
      let skusWithBindings = skus
      if (p.productType === 'SERVICE') {
        skusWithBindings = await Promise.all(
          skus.map(async (sku) => {
            const bindings = await getServiceBindings(sku.id)
            return { ...sku, bindings }
          })
        )
      }
      return { ...p, dimensions: dims, skus: skusWithBindings }
    })
  )
  return {
    type: 'category',
    ...category,
    children: childNodes,
    products: productNodes,
  }
}

onMounted(() => load({ retries: 2 }))
</script>
