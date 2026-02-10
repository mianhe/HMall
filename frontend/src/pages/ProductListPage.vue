<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">商品列表</h1>
        <router-link
          :to="{ name: 'ProductNew', query: { categoryId } }"
          class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors"
        >
          新建商品
        </router-link>
      </div>
      <p v-if="!categoryId" class="text-vmall-gray-text">请从类别管理中选择「查看商品」进入，或带 categoryId 参数。</p>
      <template v-else>
        <div v-if="loading" class="text-vmall-gray-text">加载中…</div>
        <div v-else-if="error" class="text-vmall-red">{{ error }}</div>
        <ul v-else-if="products.length" class="space-y-2">
          <li
            v-for="p in products"
            :key="p.id"
            class="flex items-center justify-between p-3 rounded-lg border border-vmall-gray-border bg-white hover:bg-gray-50"
          >
            <router-link :to="`/products/${p.id}`" class="font-medium text-gray-800 hover:text-vmall-red">{{ p.name }}</router-link>
            <span v-if="p.description" class="text-sm text-vmall-gray-text">{{ p.description }}</span>
          </li>
        </ul>
        <p v-else class="text-vmall-gray-text">该类别下暂无商品。</p>
        <div class="mt-4">
          <router-link to="/categories" class="text-vmall-gray-text hover:text-vmall-red">← 返回类别</router-link>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import { getProducts } from '../shared/api/catalog.js'

const route = useRoute()
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : null)
const products = ref([])
const loading = ref(false)
const error = ref('')

function load() {
  if (!categoryId.value) return
  loading.value = true
  error.value = ''
  getProducts(categoryId.value)
    .then((data) => { products.value = data })
    .catch((e) => { error.value = e.response?.data?.message || e.message || '加载失败' })
    .finally(() => { loading.value = false })
}

watch(() => route.query.categoryId, () => {
  categoryId.value = route.query.categoryId ? Number(route.query.categoryId) : null
  load()
})
onMounted(load)
</script>
