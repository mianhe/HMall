<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">Catalog</h1>
        <div class="flex gap-2">
          <button
            @click="openCreateProduct"
            :disabled="!tree.length"
            class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50 transition-colors"
          >
            新增商品
          </button>
          <button
            @click="load"
            :disabled="loading"
            class="px-4 py-2 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors disabled:opacity-50"
          >
            {{ loading ? '加载中…' : '刷新' }}
          </button>
        </div>
      </div>
      <div v-if="error" class="text-vmall-red mb-4">{{ error }}</div>
      <CatalogTree v-if="tree.length" :nodes="tree" />
      <p v-else-if="!loading && !error" class="text-vmall-gray-text">暂无数据，请通过 MCP 添加类目与商品。</p>

      <!-- 新增商品弹窗 -->
      <div
        v-if="formVisible"
        class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
        @click.self="closeForm"
      >
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">新增商品</h2>
          <form @submit.prevent="submitForm" class="space-y-4">
            <label class="block">
              <span class="text-sm font-medium text-gray-700">所属类目 <span class="text-vmall-red">*</span></span>
              <select
                v-model="form.categoryId"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
              >
                <option :value="null" disabled>请选择类目</option>
                <option
                  v-for="cat in leafCategories"
                  :key="cat.id"
                  :value="cat.id"
                >
                  {{ cat.path }}
                </option>
              </select>
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">商品名称 <span class="text-vmall-red">*</span></span>
              <input
                v-model.trim="form.name"
                type="text"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                placeholder="如：iPhone 16、镭雕服务"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">描述</span>
              <textarea
                v-model.trim="form.description"
                rows="2"
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 resize-none"
                placeholder="可选"
              />
            </label>
            <div>
              <span class="text-sm font-medium text-gray-700">商品类型 <span class="text-vmall-red">*</span></span>
              <div class="mt-1 flex gap-4">
                <label class="flex items-center gap-1.5 cursor-pointer">
                  <input v-model="form.productType" type="radio" value="PHYSICAL" class="accent-vmall-red" />
                  <span class="text-sm text-gray-800">实体商品</span>
                </label>
                <label class="flex items-center gap-1.5 cursor-pointer">
                  <input v-model="form.productType" type="radio" value="SERVICE" class="accent-vmall-red" />
                  <span class="text-sm text-gray-800">虚拟服务</span>
                </label>
              </div>
            </div>
            <label v-if="form.productType === 'SERVICE'" class="block">
              <span class="text-sm font-medium text-gray-700">服务分类 <span class="text-vmall-red">*</span></span>
              <select
                v-model="form.serviceKind"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
              >
                <option value="ENGRAVING">镭雕（ENGRAVING）</option>
                <option value="WARRANTY">延保（WARRANTY）</option>
                <option value="INSURANCE">碎屏险（INSURANCE）</option>
                <option value="OTHER">其他（OTHER）</option>
              </select>
            </label>
            <div v-if="formError" class="text-vmall-red text-sm">{{ formError }}</div>
            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                @click="closeForm"
                class="px-4 py-2 rounded-lg border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              >
                取消
              </button>
              <button
                type="submit"
                :disabled="submitting"
                class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
              >
                {{ submitting ? '创建中…' : '创建' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import CatalogTree from '../shared/ui/CatalogTree.vue'
import { getCategoryTree, createProduct } from '../shared/api/catalog.js'

const router = useRouter()
const tree = ref([])
const loading = ref(false)
const error = ref('')

const formVisible = ref(false)
const submitting = ref(false)
const formError = ref('')
const form = reactive({
  categoryId: null,
  name: '',
  description: '',
  productType: 'PHYSICAL',
  serviceKind: 'OTHER',
})

const leafCategories = computed(() => {
  const result = []
  function walk(nodes, ancestors) {
    for (const node of nodes) {
      const path = [...ancestors, node.name]
      if (!node.children?.length) {
        result.push({ id: node.id, path: path.join(' > ') })
      } else {
        walk(node.children, path)
      }
    }
  }
  walk(tree.value, [])
  return result
})

function openCreateProduct() {
  form.categoryId = null
  form.name = ''
  form.description = ''
  form.productType = 'PHYSICAL'
  form.serviceKind = 'OTHER'
  formError.value = ''
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
}

async function submitForm() {
  if (!form.categoryId || !form.name) return
  submitting.value = true
  formError.value = ''
  try {
    const body = {
      categoryId: form.categoryId,
      name: form.name,
      description: form.description || undefined,
      productType: form.productType,
    }
    if (form.productType === 'SERVICE') {
      body.serviceKind = form.serviceKind
    }
    const created = await createProduct(body)
    closeForm()
    router.push(`/products/${created.id}`)
  } catch (e) {
    formError.value = e.response?.data?.message || e.message || '创建失败'
  } finally {
    submitting.value = false
  }
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
