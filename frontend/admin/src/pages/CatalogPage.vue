<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">Catalog</h1>
        <div class="flex items-center gap-2">
          <button
            type="button"
            @click="openCreateSubCategory(null, '根级类目')"
            class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover"
          >
            + 新增根类目
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
      <p v-else-if="!loading && !error" class="text-vmall-gray-text">暂无数据，点击「+ 新增根类目」开始构建商品目录。</p>

      <!-- 新增商品弹窗 -->
      <div
        v-if="formVisible"
        class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
        @click.self="closeForm"
      >
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-1">新增商品</h2>
          <p class="text-sm text-vmall-gray-text mb-4">类目：{{ form.categoryName }}</p>
          <form @submit.prevent="submitForm" class="space-y-4">
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

      <!-- 新增/编辑类目弹窗 -->
      <div
        v-if="categoryFormVisible"
        class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
        @click.self="closeCategoryForm"
      >
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">
            {{ categoryFormMode === 'create' ? '新增子类目' : '编辑类目' }}
          </h2>
          <p v-if="categoryFormMode === 'create'" class="text-sm text-vmall-gray-text mb-4">父类目：{{ categoryForm.parentName }}</p>
          <form @submit.prevent="submitCategoryForm" class="space-y-4">
            <label class="block">
              <span class="text-sm font-medium text-gray-700">名称 <span class="text-vmall-red">*</span></span>
              <input
                v-model.trim="categoryForm.name"
                type="text"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                placeholder="类目名称"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">描述</span>
              <textarea
                v-model.trim="categoryForm.description"
                rows="2"
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 resize-none"
                placeholder="可选"
              />
            </label>
            <div v-if="categoryFormError" class="text-vmall-red text-sm">{{ categoryFormError }}</div>
            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                @click="closeCategoryForm"
                class="px-4 py-2 rounded-lg border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              >
                取消
              </button>
              <button
                type="submit"
                :disabled="categorySubmitting"
                class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
              >
                {{ categorySubmitting ? '保存中…' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, provide, inject, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import CatalogTree from '../shared/ui/CatalogTree.vue'
import {
  getCategoryTree,
  createProduct,
  createCategory,
  updateCategory,
  deleteCategory as apiDeleteCategory,
  deleteProduct as apiDeleteProduct,
} from '../shared/api/catalog.js'

const router = useRouter()
const toast = inject('toast')
const confirm = inject('confirm')
const tree = ref([])
const loading = ref(false)
const error = ref('')
const expandedCategoryIds = ref(new Set())

function setExpanded(nodeId, isExpanded) {
  const next = new Set(expandedCategoryIds.value)
  if (isExpanded) next.add(nodeId)
  else next.delete(nodeId)
  expandedCategoryIds.value = next
}

const formVisible = ref(false)
const submitting = ref(false)
const formError = ref('')
const form = reactive({
  categoryId: null,
  categoryName: '',
  name: '',
  description: '',
  productType: 'PHYSICAL',
  serviceKind: 'OTHER',
})

const categoryFormVisible = ref(false)
const categoryFormMode = ref('create')
const categorySubmitting = ref(false)
const categoryFormError = ref('')
const categoryForm = reactive({
  parentId: null,
  parentName: '',
  id: null,
  name: '',
  description: '',
})

function openCreateProductForCategory(categoryId, categoryName) {
  form.categoryId = categoryId
  form.categoryName = categoryName
  form.name = ''
  form.description = ''
  form.productType = 'PHYSICAL'
  form.serviceKind = 'OTHER'
  formError.value = ''
  formVisible.value = true
}

function openCreateSubCategory(parentId, parentName) {
  categoryFormMode.value = 'create'
  categoryForm.parentId = parentId
  categoryForm.parentName = parentName
  categoryForm.id = null
  categoryForm.name = ''
  categoryForm.description = ''
  categoryFormError.value = ''
  categoryFormVisible.value = true
}

function openEditCategory(id, name, description) {
  categoryFormMode.value = 'edit'
  categoryForm.parentId = null
  categoryForm.parentName = ''
  categoryForm.id = id
  categoryForm.name = name ?? ''
  categoryForm.description = description ?? ''
  categoryFormError.value = ''
  categoryFormVisible.value = true
}

async function deleteCategoryAndRefresh(id) {
  if (!(await confirm.confirm({ title: '删除类目', message: '确定删除该类目？若有子类目或商品将无法删除。' }))) return
  try {
    await apiDeleteCategory(id)
    await load()
    toast.showToast('类目已删除', 'success')
  } catch (e) {
    toast.showToast(e.response?.data?.message || e.message || '删除失败', 'error')
  }
}

async function deleteProductAndRefresh(id) {
  if (!(await confirm.confirm({ title: '删除商品', message: '确定删除该商品？' }))) return
  try {
    await apiDeleteProduct(id)
    await load()
    toast.showToast('商品已删除', 'success')
  } catch (e) {
    toast.showToast(e.response?.data?.message || e.message || '删除失败', 'error')
  }
}

provide('openCreateProductForCategory', openCreateProductForCategory)
provide('openCreateSubCategory', openCreateSubCategory)
provide('openEditCategory', openEditCategory)
provide('deleteCategoryAndRefresh', deleteCategoryAndRefresh)
provide('deleteProductAndRefresh', deleteProductAndRefresh)
provide('refreshTree', load)
provide('expandedCategoryIds', expandedCategoryIds)
provide('setExpanded', setExpanded)

function closeForm() {
  formVisible.value = false
}

function closeCategoryForm() {
  categoryFormVisible.value = false
}

async function submitCategoryForm() {
  const name = categoryForm.name?.trim()
  if (!name) return
  categorySubmitting.value = true
  categoryFormError.value = ''
  try {
    if (categoryFormMode.value === 'create') {
      await createCategory({
        parentId: categoryForm.parentId,
        name,
        description: categoryForm.description?.trim() || undefined,
      })
    } else {
      await updateCategory(categoryForm.id, {
        name,
        description: categoryForm.description?.trim() || undefined,
      })
    }
    closeCategoryForm()
    await load()
  } catch (e) {
    categoryFormError.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    categorySubmitting.value = false
  }
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
    const next = new Set(expandedCategoryIds.value)
    tree.value.forEach((n) => next.add(n.id))
    expandedCategoryIds.value = next
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
