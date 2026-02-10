<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-gray-800 mb-6">新建商品</h1>
      <p v-if="!categoryId" class="text-vmall-gray-text">请从商品列表页点击「新建商品」进入（需带 categoryId）。</p>
      <form v-else @submit.prevent="submit" class="space-y-4 max-w-md">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">名称 *</label>
          <input
            v-model="form.name"
            type="text"
            required
            class="w-full px-3 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-vmall-red"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
          <input
            v-model="form.description"
            type="text"
            class="w-full px-3 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-vmall-red"
          />
        </div>
        <div v-if="submitError" class="text-vmall-red text-sm">{{ submitError }}</div>
        <div class="flex gap-3">
          <button type="submit" class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors">创建</button>
          <router-link :to="{ path: '/products', query: { categoryId } }" class="px-4 py-2 rounded-lg border border-vmall-gray-border text-gray-700 hover:bg-gray-100">取消</router-link>
        </div>
      </form>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import { createProduct } from '../shared/api/catalog.js'

const router = useRouter()
const route = useRoute()
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : null)
const form = reactive({ name: '', description: '' })
const submitError = ref('')

onMounted(() => {
  if (route.query.categoryId) categoryId.value = Number(route.query.categoryId)
})

async function submit() {
  if (!categoryId.value) return
  submitError.value = ''
  try {
    await createProduct({
      categoryId: categoryId.value,
      name: form.name,
      description: form.description || undefined,
    })
    router.push({ path: '/products', query: { categoryId: categoryId.value } })
  } catch (e) {
    submitError.value = e.response?.data?.message || e.message || '创建失败'
  }
}
</script>
