<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div v-if="loading" class="text-vmall-gray-text">加载中…</div>
      <div v-else-if="error" class="text-vmall-red">{{ error }}</div>
      <div v-else-if="product" class="space-y-6">
        <!-- 商品基础信息 -->
        <div class="space-y-2">
          <h1 class="text-2xl font-bold text-gray-800">{{ product.name }}</h1>
          <p v-if="product.description" class="text-vmall-gray-text">{{ product.description }}</p>
          <p class="text-sm text-vmall-gray-text">类别 ID：{{ product.categoryId }}</p>
          <router-link :to="{ path: '/products', query: { categoryId: product.categoryId } }" class="text-vmall-gray-text hover:text-vmall-red">← 返回该类别商品列表</router-link>
        </div>

        <!-- 规格配置 -->
        <section class="border-t pt-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">规格配置</h2>
          <p v-if="dimensionsError" class="text-sm text-vmall-red mb-2">{{ dimensionsError }}</p>
          <div v-if="dimensions.length === 0 && !showDimensionForm" class="text-sm text-vmall-gray-text mb-2">暂无规格维度，可先添加维度再为维度添加选项。</div>
          <ul class="space-y-3 mb-3">
            <li v-for="dim in dimensions" :key="dim.id" class="border rounded p-3 bg-gray-50">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-medium">{{ dim.name }}</span>
                <span v-if="dim.required" class="text-xs px-1.5 py-0.5 bg-amber-100 text-amber-800 rounded">必填</span>
                <span v-if="dim.affectsAppearance" class="text-xs text-vmall-gray-text">影响外观</span>
              </div>
              <div class="mt-2 text-sm text-vmall-gray-text">
                选项：<span v-if="!dim.options || dim.options.length === 0">无</span>
                <span v-else>{{ dim.options.map(o => o.optionValue).join('、') }}</span>
              </div>
              <div class="mt-2">
                <button v-if="showOptionFormFor !== dim.id" type="button" class="text-sm text-vmall-red hover:underline" @click="showOptionFormFor = dim.id">添加选项</button>
                <div v-else class="flex flex-col gap-2 max-w-md">
                  <div class="flex gap-2">
                    <input v-model="newOptionValue" type="text" placeholder="选项值" class="border rounded px-2 py-1 text-sm w-32" />
                    <input v-model.number="newOptionSortOrder" type="number" placeholder="排序" class="border rounded px-2 py-1 text-sm w-20" />
                  </div>
                  <input v-if="dim.affectsAppearance" v-model="newOptionImage" type="text" placeholder="图片链接（可选）" class="border rounded px-2 py-1 text-sm" />
                  <div class="flex gap-2">
                    <button type="button" class="text-sm px-2 py-1 bg-gray-200 rounded hover:bg-gray-300" @click="submitOption(dim.id)">确定</button>
                    <button type="button" class="text-sm text-vmall-gray-text hover:underline" @click="showOptionFormFor = null; newOptionValue = ''; newOptionSortOrder = null; newOptionImage = ''">取消</button>
                  </div>
                </div>
              </div>
            </li>
          </ul>
          <div v-if="!showDimensionForm">
            <button type="button" class="text-sm text-vmall-red hover:underline" @click="showDimensionForm = true">新建维度</button>
          </div>
          <div v-else class="border rounded p-3 bg-gray-50 space-y-2 max-w-md">
            <input v-model="newDimName" type="text" placeholder="维度名称（如容量、颜色）" class="border rounded px-2 py-1 w-full" />
            <label class="flex items-center gap-2 text-sm">
              <input v-model="newDimRequired" type="checkbox" />
              创建 SKU 时必选
            </label>
            <label class="flex items-center gap-2 text-sm">
              <input v-model="newDimAffectsAppearance" type="checkbox" />
              影响外观（可选图片）
            </label>
            <div class="flex gap-2">
              <button type="button" class="px-3 py-1 bg-vmall-red text-white rounded text-sm hover:opacity-90" @click="submitDimension">添加</button>
              <button type="button" class="text-sm text-vmall-gray-text hover:underline" @click="showDimensionForm = false; newDimName = ''; newDimRequired = true; newDimAffectsAppearance = false">取消</button>
            </div>
          </div>
        </section>

        <!-- SKU 列表 -->
        <section class="border-t pt-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">SKU 列表</h2>
          <p v-if="skusError" class="text-sm text-vmall-red mb-2">{{ skusError }}</p>
          <div v-if="skus.length === 0 && !showSkuForm" class="text-sm text-vmall-gray-text mb-2">暂无 SKU。请先完成规格配置，再新建 SKU。</div>
          <table v-if="skus.length > 0" class="w-full text-sm border-collapse mb-3">
            <thead>
              <tr class="border-b text-left text-vmall-gray-text">
                <th class="py-2 pr-3">规格</th>
                <th class="py-2 pr-3">价格</th>
                <th class="py-2">展示名</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="sku in skus" :key="sku.id" class="border-b last:border-0">
                <td class="py-2 pr-3">
                  <span v-if="sku.specValues && sku.specValues.length">{{ sku.specValues.map(s => `${s.dimensionName}: ${s.optionValue}`).join(' · ') }}</span>
                  <span v-else class="text-vmall-gray-text">—</span>
                </td>
                <td class="py-2 pr-3 font-medium">¥{{ (sku.priceCents / 100).toFixed(2) }}</td>
                <td class="py-2 text-vmall-gray-text">{{ sku.displayName || '—' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!showSkuForm">
            <button type="button" class="text-sm text-vmall-red hover:underline" @click="openSkuForm">新建 SKU</button>
          </div>
          <div v-else class="border rounded p-4 bg-gray-50 space-y-3 max-w-md">
            <p v-if="skuFormError" class="text-sm text-vmall-red">{{ skuFormError }}</p>
            <div v-for="dim in dimensions" :key="'sku-' + dim.id">
              <label class="text-sm font-medium">
                {{ dim.name }}
                <span v-if="dim.required" class="text-xs text-amber-700">*</span>
              </label>
              <select v-model="skuSelectedOptionIds[dim.id]" class="mt-1 block w-full border rounded px-2 py-1 text-sm">
                <option :value="null">请选择</option>
                <option v-for="opt in (dim.options || [])" :key="opt.id" :value="opt.id">{{ opt.optionValue }}</option>
              </select>
            </div>
            <div>
              <label class="text-sm font-medium">价格（分）<span class="text-xs text-amber-700">*</span></label>
              <input v-model.number="skuPriceCents" type="number" min="0" class="mt-1 block w-full border rounded px-2 py-1 text-sm" />
            </div>
            <div>
              <label class="text-sm font-medium">展示名（可选）</label>
              <input v-model="skuDisplayName" type="text" placeholder="如 黑色 128G" class="mt-1 block w-full border rounded px-2 py-1 text-sm" />
            </div>
            <div class="flex gap-2 pt-1">
              <button type="button" class="px-3 py-1 bg-vmall-red text-white rounded text-sm hover:opacity-90" @click="submitSku">添加</button>
              <button type="button" class="text-sm text-vmall-gray-text hover:underline" @click="closeSkuForm">取消</button>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import {
  getProduct,
  getDimensions,
  createDimension,
  createOption,
  getSkus,
  createSku,
} from '../shared/api/catalog.js'

const route = useRoute()

const product = ref(null)
const loading = ref(true)
const error = ref('')

const dimensions = ref([])
const dimensionsError = ref('')
const showDimensionForm = ref(false)
const newDimName = ref('')
const newDimRequired = ref(true)
const newDimAffectsAppearance = ref(false)

const showOptionFormFor = ref(null)
const newOptionValue = ref('')
const newOptionSortOrder = ref(null)
const newOptionImage = ref('')

const skus = ref([])
const skusError = ref('')
const showSkuForm = ref(false)
const skuFormError = ref('')
const skuSelectedOptionIds = ref({})
const skuPriceCents = ref(0)
const skuDisplayName = ref('')

function loadProduct() {
  loading.value = true
  error.value = ''
  getProduct(route.params.id)
    .then((data) => { product.value = data })
    .catch((e) => { error.value = e.response?.data?.message || e.message || '加载失败' })
    .finally(() => { loading.value = false })
}

function loadDimensions() {
  dimensionsError.value = ''
  getDimensions(route.params.id)
    .then((data) => { dimensions.value = data || [] })
    .catch((e) => { dimensionsError.value = e.response?.data?.message || e.message || '加载维度失败' })
}

function submitDimension() {
  if (!newDimName.value?.trim()) return
  createDimension(route.params.id, {
    name: newDimName.value.trim(),
    required: newDimRequired.value,
    affectsAppearance: newDimAffectsAppearance.value,
  })
    .then(() => {
      showDimensionForm.value = false
      newDimName.value = ''
      newDimRequired.value = true
      newDimAffectsAppearance.value = false
      loadDimensions()
    })
    .catch((e) => { dimensionsError.value = e.response?.data?.message || e.message || '添加维度失败' })
}

function submitOption(dimensionId) {
  if (!newOptionValue.value?.trim()) return
  createOption(route.params.id, dimensionId, {
    optionValue: newOptionValue.value.trim(),
    sortOrder: newOptionSortOrder.value ?? undefined,
    image: newOptionImage.value?.trim() || undefined,
  })
    .then(() => {
      showOptionFormFor.value = null
      newOptionValue.value = ''
      newOptionSortOrder.value = null
      newOptionImage.value = ''
      loadDimensions()
    })
    .catch((e) => { dimensionsError.value = e.response?.data?.message || e.message || '添加选项失败' })
}

function loadSkus() {
  skusError.value = ''
  getSkus(route.params.id)
    .then((data) => { skus.value = data || [] })
    .catch((e) => { skusError.value = e.response?.data?.message || e.message || '加载 SKU 失败' })
}

function openSkuForm() {
  skuFormError.value = ''
  skuSelectedOptionIds.value = {}
  dimensions.value.forEach((d) => { skuSelectedOptionIds.value[d.id] = null })
  skuPriceCents.value = 0
  skuDisplayName.value = ''
  showSkuForm.value = true
}

function closeSkuForm() {
  showSkuForm.value = false
}

function submitSku() {
  const requiredDims = dimensions.value.filter((d) => d.required)
  for (const d of requiredDims) {
    if (skuSelectedOptionIds.value[d.id] == null) {
      skuFormError.value = `请选择「${d.name}」`
      return
    }
  }
  if (skuPriceCents.value == null || skuPriceCents.value < 0) {
    skuFormError.value = '价格不能为负'
    return
  }
  const specOptionIds = Object.values(skuSelectedOptionIds.value).filter((id) => id != null)
  skuFormError.value = ''
  createSku(route.params.id, {
    specOptionIds,
    priceCents: skuPriceCents.value,
    displayName: skuDisplayName.value?.trim() || null,
  })
    .then(() => {
      closeSkuForm()
      loadSkus()
    })
    .catch((e) => { skuFormError.value = e.response?.data?.message || e.message || '创建 SKU 失败' })
}

onMounted(() => {
  loadProduct()
})

watch(product, (p) => {
  if (p) {
    loadDimensions()
    loadSkus()
  }
})
</script>
