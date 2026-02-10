<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">类别管理</h1>
        <router-link
          :to="{ name: 'CategoryNew' }"
          class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors"
        >
          新建根类别
        </router-link>
      </div>
      <div v-if="loading" class="text-vmall-gray-text">加载中…</div>
      <div v-else-if="error" class="text-vmall-red">{{ error }}</div>
      <div v-else-if="tree.length" class="space-y-1">
        <CategoryNode
          v-for="root in tree"
          :key="root.id"
          :node="root"
          :depth="0"
        />
      </div>
      <p v-else class="text-vmall-gray-text">暂无类别，请先新建根类别。</p>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import CategoryNode from '../shared/ui/CategoryNode.vue'
import { getCategories, getProducts } from '../shared/api/catalog.js'

const tree = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const roots = await getCategories(null)
    tree.value = await Promise.all(roots.map((r) => loadNode(r)))
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadNode(node, depth = 0) {
  const [children, products] = await Promise.all([
    getCategories(node.id),
    getProducts(node.id),
  ])
  const hasProducts = products.length > 0
  const childNodes = depth < 2 ? await Promise.all(children.map((c) => loadNode(c, depth + 1))) : []
  return {
    ...node,
    children: childNodes,
    hasProducts,
  }
}

onMounted(load)
</script>
