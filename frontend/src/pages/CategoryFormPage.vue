<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-gray-800 mb-6">新建类别</h1>
      <form @submit.prevent="submit" class="space-y-4 max-w-md">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">名称 *</label>
          <input
            v-model="form.name"
            type="text"
            required
            class="w-full px-3 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-vmall-red"
            placeholder="如：手机"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
          <input
            v-model="form.description"
            type="text"
            class="w-full px-3 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-vmall-red"
            placeholder="可选"
          />
        </div>
        <div v-if="parentOptions.length">
          <label class="block text-sm font-medium text-gray-700 mb-1">父类别</label>
          <select
            v-model="form.parentId"
            class="w-full px-3 py-2 border border-vmall-gray-border rounded-lg focus:ring-2 focus:ring-vmall-red focus:border-vmall-red"
          >
            <option :value="null">无（根类别）</option>
            <option v-for="p in parentOptions" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
        </div>
        <div v-if="submitError" class="text-vmall-red text-sm">{{ submitError }}</div>
        <div class="flex gap-3">
          <button
            type="submit"
            class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors"
          >
            创建
          </button>
          <router-link to="/categories" class="px-4 py-2 rounded-lg border border-vmall-gray-border text-gray-700 hover:bg-gray-100">
            取消
          </router-link>
        </div>
      </form>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import { getCategories, createCategory } from '../shared/api/catalog.js'

const router = useRouter()
const route = useRoute()
const parentOptions = ref([])
const form = reactive({
  name: '',
  description: '',
  parentId: null,
})
const submitError = ref('')

onMounted(async () => {
  const list = await getCategories(null)
  parentOptions.value = list
  const pid = route.query.parentId ? Number(route.query.parentId) : null
  if (pid) form.parentId = pid
})

async function submit() {
  submitError.value = ''
  try {
    await createCategory({
      name: form.name,
      description: form.description || undefined,
      parentId: form.parentId || undefined,
    })
    router.push('/categories')
  } catch (e) {
    submitError.value = e.response?.data?.message || e.message || '创建失败'
  }
}
</script>
