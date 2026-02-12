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
import { getCategories, getProducts, getDimensions, getSkus } from '../shared/api/catalog.js'

const tree = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const roots = await getCategories(null)
    tree.value = await Promise.all(roots.map((c) => loadCategoryNode(c)))
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
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
      return { ...p, dimensions: dims, skus }
    })
  )
  return {
    type: 'category',
    ...category,
    children: childNodes,
    products: productNodes,
  }
}

onMounted(load)
</script>
