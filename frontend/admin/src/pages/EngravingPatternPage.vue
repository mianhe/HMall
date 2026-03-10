<template>
  <div>
    <AppHeader />
    <main class="max-w-6xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">镭雕图案库</h1>
        <div class="flex gap-2">
          <button
            @click="openCreate"
            class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover"
          >
            新增图案
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

      <!-- 过滤 -->
      <div class="flex flex-wrap items-end gap-4 mb-4 p-4 bg-gray-50 rounded-lg border border-vmall-gray-border">
        <label class="flex flex-col gap-1">
          <span class="text-sm font-medium text-gray-700">启用状态</span>
          <select
            v-model="filterEnabled"
            class="min-w-[140px] px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
          >
            <option value="">全部</option>
            <option :value="true">仅启用</option>
            <option :value="false">仅禁用</option>
          </select>
        </label>
        <button
          @click="load"
          :disabled="loading"
          class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
        >
          查询
        </button>
      </div>

      <!-- 列表 -->
      <div v-if="list.length" class="bg-white rounded-lg border border-vmall-gray-border overflow-x-auto">
        <table class="w-full text-sm min-w-[800px]">
          <thead class="bg-gray-100 border-b border-vmall-gray-border">
            <tr>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-16">ID</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-24">缩略图</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700">名称</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">排序</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">状态</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-32">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in list"
              :key="row.id"
              class="border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg"
            >
              <td class="py-2 px-4 text-gray-800">{{ row.id }}</td>
              <td class="py-2 px-4">
                <img
                  v-if="row.imageUrl"
                  :src="row.imageUrl"
                  :alt="row.name"
                  class="w-12 h-12 object-cover rounded border border-gray-200"
                />
                <span v-else class="text-gray-400">—</span>
              </td>
              <td class="py-2 px-4 text-gray-800">{{ row.name }}</td>
              <td class="py-2 px-4 text-gray-700">{{ row.sortOrder ?? '—' }}</td>
              <td class="py-2 px-4">
                <span
                  :class="row.enabled ? 'bg-green-50 text-green-600' : 'bg-gray-100 text-gray-500'"
                  class="px-2 py-0.5 rounded text-xs font-medium"
                >
                  {{ row.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="py-2 px-4">
                <button
                  @click="openEdit(row)"
                  class="text-vmall-red hover:underline mr-3"
                >
                  编辑
                </button>
                <button
                  @click="doDelete(row)"
                  :disabled="deletingId === row.id"
                  class="text-red-600 hover:underline disabled:opacity-50"
                >
                  {{ deletingId === row.id ? '删除中…' : '删除' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else-if="!loading && !error" class="text-vmall-gray-text">暂无图案，点击「新增图案」添加。</p>

      <!-- 新增/编辑弹窗 -->
      <div
        v-if="formVisible"
        class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
        @click.self="closeForm"
      >
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">{{ editingId ? '编辑图案' : '新增图案' }}</h2>
          <form @submit.prevent="submitForm" class="space-y-4">
            <label class="block">
              <span class="text-sm font-medium text-gray-700">名称</span>
              <input
                v-model.trim="form.name"
                type="text"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                placeholder="图案名称"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">图片 URL</span>
              <div class="mt-1 flex gap-2">
                <input
                  ref="formFileInputRef"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  @change="onFileChange"
                />
                <input
                  v-model.trim="form.imageUrl"
                  type="text"
                  required
                  class="flex-1 px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                  placeholder="上传或输入图片 URL"
                />
                <button
                  type="button"
                  :disabled="uploading"
                  @click="formFileInputRef?.click()"
                  class="px-3 py-2 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg disabled:opacity-50 shrink-0"
                >
                  {{ uploading ? '上传中…' : '上传' }}
                </button>
              </div>
              <img
                v-if="form.imageUrl"
                :src="form.imageUrl"
                alt="预览"
                class="mt-2 w-20 h-20 object-cover rounded border border-gray-200"
                @error="form.imageUrl = ''"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">排序（可选）</span>
              <input
                v-model.number="form.sortOrder"
                type="number"
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                placeholder="数字越小越靠前"
              />
            </label>
            <label class="flex items-center gap-2">
              <input v-model="form.enabled" type="checkbox" class="rounded" />
              <span class="text-sm text-gray-700">启用</span>
            </label>
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
                {{ submitting ? '提交中…' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, inject, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import {
  getEngravingPatterns,
  createEngravingPattern,
  updateEngravingPattern,
  deleteEngravingPattern,
  uploadFile,
} from '../shared/api/catalog.js'

const confirm = inject('confirm')
const list = ref([])
const loading = ref(false)
const error = ref('')
const filterEnabled = ref('')
const formVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  name: '',
  imageUrl: '',
  sortOrder: null,
  enabled: true,
})
const submitting = ref(false)
const uploading = ref(false)
const deletingId = ref(null)
const formFileInputRef = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const enabled = filterEnabled.value === '' ? null : filterEnabled.value
    list.value = await getEngravingPatterns(enabled)
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.imageUrl = ''
  form.sortOrder = null
  form.enabled = true
  formVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name
  form.imageUrl = row.imageUrl || ''
  form.sortOrder = row.sortOrder ?? null
  form.enabled = row.enabled ?? true
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
  editingId.value = null
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  e.target.value = ''
  uploading.value = true
  error.value = ''
  try {
    const { url } = await uploadFile(file)
    form.imageUrl = url
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function submitForm() {
  if (!form.name || !form.imageUrl) return
  submitting.value = true
  error.value = ''
  try {
    const body = {
      name: form.name,
      imageUrl: form.imageUrl,
      sortOrder: form.sortOrder ?? undefined,
      enabled: form.enabled,
    }
    if (editingId.value) {
      await updateEngravingPattern(editingId.value, body)
    } else {
      await createEngravingPattern(body)
    }
    closeForm()
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    submitting.value = false
  }
}

async function doDelete(row) {
  if (!(await confirm.confirm({ title: '删除图案', message: `确定删除图案「${row.name}」？` }))) return
  deletingId.value = row.id
  error.value = ''
  try {
    await deleteEngravingPattern(row.id)
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '删除失败'
  } finally {
    deletingId.value = null
  }
}

onMounted(load)
</script>
