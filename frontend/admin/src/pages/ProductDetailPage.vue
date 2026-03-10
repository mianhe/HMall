<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <router-link
          to="/catalog"
          class="inline-flex items-center text-vmall-gray-text hover:text-vmall-red"
        >
          ← 返回 Catalog
        </router-link>
        <button
          v-if="product"
          type="button"
          class="px-3 py-1.5 rounded-lg border border-red-200 text-red-600 text-sm hover:bg-red-50"
          @click="doDeleteProduct"
        >
          删除商品
        </button>
      </div>

      <div v-if="loading && !product" class="text-vmall-gray-text">加载中…</div>
      <div v-else-if="error" class="text-vmall-red mb-4">{{ error }}</div>
      <template v-else-if="product">
        <div class="flex items-center gap-3 mb-2">
          <template v-if="!editingBasic">
            <h1 class="text-2xl font-bold text-gray-800">{{ product.name }}</h1>
            <button
              type="button"
              class="px-2 py-0.5 text-xs rounded border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              @click="startEditBasic"
            >
              编辑
            </button>
          </template>
          <template v-else>
            <input
              v-model.trim="editName"
              type="text"
              class="text-2xl font-bold text-gray-800 border border-vmall-gray-border rounded px-2 py-1 w-full max-w-md"
              placeholder="商品名称"
            />
            <button
              type="button"
              class="px-2 py-1 text-sm rounded bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
              :disabled="savingBasic || !editName"
              @click="saveBasic"
            >
              {{ savingBasic ? '保存中…' : '保存' }}
            </button>
            <button
              type="button"
              class="px-2 py-1 text-sm rounded border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              :disabled="savingBasic"
              @click="cancelEditBasic"
            >
              取消
            </button>
          </template>
          <span
            v-if="product.productType === 'SERVICE'"
            class="px-2 py-0.5 text-xs rounded bg-blue-50 text-blue-600 font-medium"
          >SERVICE</span>
          <span
            v-else
            class="px-2 py-0.5 text-xs rounded bg-gray-100 text-gray-500 font-medium"
          >PHYSICAL</span>
          <span
            v-if="product.serviceKind === 'ENGRAVING'"
            class="px-2 py-0.5 text-xs rounded bg-amber-50 text-amber-700 font-medium"
          >镭雕</span>
        </div>
        <div class="mb-6">
          <template v-if="!editingBasic">
            <p v-if="product.description" class="text-vmall-gray-text">{{ product.description }}</p>
            <p v-else class="text-vmall-gray-text">—</p>
          </template>
          <template v-else>
            <textarea
              v-model.trim="editDescription"
              rows="3"
              class="w-full max-w-md mt-1 px-2 py-1.5 border border-vmall-gray-border rounded text-vmall-gray-text"
              placeholder="描述（可选）"
            />
          </template>
        </div>

        <!-- 产品级展示图 -->
        <section class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">产品级展示图</h2>
          <div v-if="productImages.length" class="flex flex-wrap gap-3 mb-3">
            <div
              v-for="img in productImages"
              :key="'pi-' + img.id"
              class="relative group"
            >
              <img
                :src="img.imageUrl"
                alt="产品图"
                class="w-24 h-24 object-cover rounded border border-gray-200"
              />
              <button
                type="button"
                class="absolute top-0 right-0 w-5 h-5 flex items-center justify-center bg-red-500 text-white text-xs rounded-full opacity-0 group-hover:opacity-100"
                :disabled="deleting === img.id"
                @click="doDeleteProductImage(img.id)"
              >
                ×
              </button>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <input
              ref="productFileInputRef"
              type="file"
              accept="image/*"
              class="hidden"
              @change="onProductFileChange"
            />
            <button
              type="button"
              class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
              :disabled="uploading"
              @click="productFileInputRef?.click()"
            >
              {{ uploading ? '上传中…' : '上传产品级展示图' }}
            </button>
          </div>
        </section>

        <!-- 规格维度与选项 -->
        <section class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">规格维度与选项</h2>
          <div v-if="dimensions.length" class="space-y-4">
            <div
              v-for="dim in dimensions"
              :key="'d-' + dim.id"
              class="border border-vmall-gray-border rounded-lg p-4"
            >
              <div class="flex items-center gap-2 mb-2">
                <span class="font-medium text-gray-800">{{ dim.name }}</span>
                <span v-if="dim.required" class="px-1.5 py-0.5 text-xs rounded bg-blue-50 text-blue-600">必填</span>
              </div>
              <div v-if="dim.options?.length" class="space-y-3 ml-2">
                <div
                  v-for="opt in dim.options"
                  :key="'o-' + opt.id"
                  class="flex flex-wrap items-center gap-2"
                >
                  <span class="text-sm text-gray-700 py-1 shrink-0">{{ opt.optionValue }}</span>
                  <button
                    type="button"
                    class="px-1.5 py-0.5 text-xs rounded border border-red-200 text-red-600 hover:bg-red-50"
                    :disabled="deletingOptionId === opt.id"
                    @click="doDeleteOption(dim.id, opt.id)"
                  >
                    {{ deletingOptionId === opt.id ? '删除中…' : '删除' }}
                  </button>
                  <div v-if="opt.images?.length" class="flex flex-wrap gap-2">
                    <div
                      v-for="img in opt.images"
                      :key="'oi-' + img.id"
                      class="relative group"
                    >
                      <img
                        :src="img.imageUrl"
                        :alt="opt.optionValue"
                        class="w-16 h-16 object-cover rounded border border-gray-200"
                      />
                      <button
                        type="button"
                        class="absolute -top-1 -right-1 w-5 h-5 flex items-center justify-center bg-red-500 text-white text-xs rounded-full opacity-0 group-hover:opacity-100"
                        :disabled="deleting === 'o-' + opt.id + '-' + img.id"
                        @click="doDeleteOptionImage(dim.id, opt.id, img.id)"
                      >
                        ×
                      </button>
                    </div>
                  </div>
                  <input
                    :ref="(el) => setOptionFileInput(el, dim.id, opt.id)"
                    type="file"
                    accept="image/*"
                    class="hidden"
                    @change="(e) => onOptionFileChange(e, dim.id, opt.id)"
                  />
                  <button
                    type="button"
                    class="px-2 py-1 rounded text-sm border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
                    :disabled="uploading"
                    @click="clickOptionUpload(dim.id, opt.id)"
                  >
                    {{ uploading ? '上传中…' : '添加展示图' }}
                  </button>
                </div>
              </div>
              <p v-else class="text-sm text-gray-400 italic ml-2">暂无选项</p>
              <div class="mt-2 ml-2 flex items-center gap-2">
                <input
                  v-model="newOptionValues[dim.id]"
                  type="text"
                  class="w-40 px-2 py-1 border border-vmall-gray-border rounded text-sm"
                  placeholder="新选项值"
                  @keydown.enter.prevent="doCreateOption(dim.id)"
                />
                <button
                  type="button"
                  class="px-2 py-1 rounded text-sm bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
                  :disabled="creatingOptionDimId === dim.id || !newOptionValues[dim.id]?.trim()"
                  @click="doCreateOption(dim.id)"
                >
                  {{ creatingOptionDimId === dim.id ? '添加中…' : '+ 新增选项' }}
                </button>
              </div>
            </div>
          </div>
          <div class="mt-3 flex items-center gap-2">
            <template v-if="!showNewDimensionForm">
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
                @click="showNewDimensionForm = true"
              >
                + 新增维度
              </button>
            </template>
            <template v-else>
              <input
                v-model.trim="newDimensionName"
                type="text"
                class="w-48 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                placeholder="维度名称（如：颜色）"
              />
              <label class="flex items-center gap-1 text-sm text-gray-700">
                <input v-model="newDimensionRequired" type="checkbox" class="rounded" />
                必填
              </label>
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg bg-vmall-red text-white text-sm hover:bg-vmall-red-hover disabled:opacity-50"
                :disabled="creatingDimension || !newDimensionName"
                @click="doCreateDimension"
              >
                {{ creatingDimension ? '创建中…' : '创建' }}
              </button>
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
                :disabled="creatingDimension"
                @click="showNewDimensionForm = false; newDimensionName = ''; newDimensionRequired = false"
              >
                取消
              </button>
            </template>
          </div>
          <p v-if="!dimensions.length" class="text-vmall-gray-text">暂无规格维度，点击「+ 新增维度」添加。</p>
        </section>

        <!-- SKU 列表 -->
        <section class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">SKU</h2>
          <div v-if="skus.length" class="space-y-2 mb-4">
            <div
              v-for="sku in skus"
              :key="'s-' + sku.id"
              class="flex flex-wrap items-center gap-3 py-2 px-3 border border-vmall-gray-border rounded-lg text-sm"
            >
              <span class="text-gray-700 shrink-0">{{ skuDisplay(sku) }}</span>
              <div class="flex items-center gap-1">
                <span class="text-vmall-gray-text">¥</span>
                <input
                  v-model.number="skuEditPrice[sku.id]"
                  type="number"
                  min="0"
                  step="0.01"
                  class="w-24 px-2 py-1 border border-vmall-gray-border rounded"
                />
              </div>
              <input
                v-model="skuEditDisplayName[sku.id]"
                type="text"
                class="w-40 px-2 py-1 border border-vmall-gray-border rounded text-vmall-gray-text"
                placeholder="展示名（可选）"
              />
              <button
                type="button"
                class="px-2 py-1 rounded bg-vmall-red text-white text-xs hover:bg-vmall-red-hover disabled:opacity-50"
                :disabled="savingSkuId === sku.id"
                @click="doUpdateSku(sku)"
              >
                {{ savingSkuId === sku.id ? '保存中…' : '保存' }}
              </button>
              <button
                type="button"
                class="px-2 py-1 rounded border border-red-200 text-red-600 text-xs hover:bg-red-50 disabled:opacity-50"
                :disabled="deletingSkuId === sku.id"
                @click="doDeleteSku(sku.id)"
              >
                {{ deletingSkuId === sku.id ? '删除中…' : '删除' }}
              </button>
            </div>
          </div>
          <div v-if="showNewSkuForm" class="border border-vmall-gray-border rounded-lg p-4 mb-3 space-y-3">
            <div v-for="dim in dimensions" :key="'ns-d-' + dim.id" class="flex items-center gap-2">
              <span class="text-sm text-gray-700 w-24 shrink-0">{{ dim.name }}</span>
              <select
                v-model="newSkuOptionIds[dim.id]"
                class="flex-1 max-w-xs px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
              >
                <option :value="null">请选择</option>
                <option
                  v-for="opt in dim.options"
                  :key="opt.id"
                  :value="opt.id"
                >
                  {{ opt.optionValue }}
                </option>
              </select>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-sm text-gray-700 w-24 shrink-0">价格（元）</span>
              <input
                v-model="newSkuPriceYuan"
                type="number"
                min="0"
                step="0.01"
                class="w-28 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
              />
            </div>
            <div class="flex items-center gap-2">
              <span class="text-sm text-gray-700 w-24 shrink-0">展示名</span>
              <input
                v-model="newSkuDisplayName"
                type="text"
                class="flex-1 max-w-xs px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                placeholder="可选"
              />
            </div>
            <div class="flex gap-2">
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg bg-vmall-red text-white text-sm hover:bg-vmall-red-hover disabled:opacity-50"
                :disabled="creatingSku || !canCreateSku"
                @click="doCreateSku"
              >
                {{ creatingSku ? '创建中…' : '创建' }}
              </button>
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
                :disabled="creatingSku"
                @click="closeNewSkuForm"
              >
                取消
              </button>
            </div>
          </div>
          <button
            v-if="!showNewSkuForm"
            type="button"
            class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
            @click="openNewSkuForm"
          >
            + 新增 SKU
          </button>
          <p v-if="!skus.length && !showNewSkuForm" class="text-vmall-gray-text text-sm">暂无 SKU，请先添加规格维度与选项，再添加 SKU。</p>
        </section>

        <!-- 服务绑定管理（仅 SERVICE 类型商品） -->
        <section v-if="product.productType === 'SERVICE' && skus.length" class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">服务绑定</h2>
          <div v-for="sku in skus" :key="'sb-' + sku.id" class="mb-6 border border-vmall-gray-border rounded-lg p-4">
            <h3 class="text-sm font-medium text-gray-700 mb-3">
              SKU: {{ skuDisplay(sku) }}
              <span class="text-vmall-gray-text font-normal ml-1">（标准价 ¥{{ (sku.priceCents / 100).toFixed(2) }}）</span>
            </h3>

            <!-- 已绑定列表 -->
            <div v-if="bindingsMap[sku.id]?.length" class="space-y-2 mb-3">
              <div
                v-for="binding in bindingsMap[sku.id]"
                :key="'b-' + binding.id"
                class="flex items-center justify-between py-2 px-3 bg-gray-50 rounded text-sm"
              >
                <div class="flex items-center gap-2">
                  <span class="text-gray-400">→</span>
                  <span class="text-gray-700">{{ binding.targetSpuName || `SPU#${binding.targetSpuId}` }}</span>
                  <span class="text-vmall-gray-text">
                    {{ binding.priceCents != null ? `¥${(binding.priceCents / 100).toFixed(2)}` : '继承标准价' }}
                  </span>
                </div>
                <button
                  type="button"
                  class="text-red-500 hover:text-red-700 text-xs"
                  :disabled="deletingBinding === binding.id"
                  @click="doDeleteBinding(sku.id, binding.id)"
                >
                  {{ deletingBinding === binding.id ? '删除中…' : '删除' }}
                </button>
              </div>
            </div>
            <p v-else class="text-sm text-gray-400 italic mb-3">暂无绑定（可独立售卖）</p>

            <!-- 新增绑定表单 -->
            <div class="flex items-end gap-2 text-sm">
              <div>
                <label class="block text-xs text-vmall-gray-text mb-1">目标商品(SPU) ID</label>
                <input
                  v-model.number="newBindingForms[sku.id].targetSpuId"
                  type="number"
                  min="1"
                  class="w-28 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                  placeholder="SPU ID"
                />
              </div>
              <div>
                <label class="block text-xs text-vmall-gray-text mb-1">价格（元，留空继承标准价）</label>
                <input
                  v-model="newBindingForms[sku.id].priceYuan"
                  type="text"
                  class="w-28 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                  placeholder="留空=继承"
                />
              </div>
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg bg-vmall-red text-white text-sm hover:bg-red-600 disabled:opacity-50"
                :disabled="creatingBinding || !newBindingForms[sku.id].targetSpuId"
                @click="doCreateBinding(sku.id)"
              >
                {{ creatingBinding ? '创建中…' : '添加绑定' }}
              </button>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import {
  getProduct,
  getDimensions,
  getProductImages,
  getSkus,
  getServiceBindings,
  createServiceBinding,
  deleteServiceBinding,
  uploadFile,
  addProductImage,
  addOptionImage,
  deleteProductImage,
  deleteOptionImage,
  updateProduct,
  deleteProduct as apiDeleteProduct,
  createDimension,
  createOption,
  deleteOption as apiDeleteOption,
  createSku,
  updateSku,
  deleteSku as apiDeleteSku,
} from '../shared/api/catalog.js'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const editingBasic = ref(false)
const editName = ref('')
const editDescription = ref('')
const savingBasic = ref(false)
const dimensions = ref([])
const productImages = ref([])
const skus = ref([])
const loading = ref(true)
const error = ref('')
const uploading = ref(false)
const deleting = ref(null)
const productFileInputRef = ref(null)
const optionFileInputs = ref({}) // key: `${dimensionId}-${optionId}`
const bindingsMap = ref({})     // key: skuId → ServiceBinding[]
const newBindingForms = ref({}) // key: skuId → { targetSpuId, priceYuan }
const creatingBinding = ref(false)
const deletingBinding = ref(null)
const showNewDimensionForm = ref(false)
const newDimensionName = ref('')
const newDimensionRequired = ref(false)
const creatingDimension = ref(false)
const newOptionValues = ref({})
const creatingOptionDimId = ref(null)
const deletingOptionId = ref(null)
const skuEditPrice = ref({})
const skuEditDisplayName = ref({})
const savingSkuId = ref(null)
const deletingSkuId = ref(null)
const showNewSkuForm = ref(false)
const newSkuOptionIds = ref({})
const newSkuPriceYuan = ref('')
const newSkuDisplayName = ref('')
const creatingSku = ref(false)

const spuId = computed(() => {
  const id = route.params.id
  return id != null && id !== '' ? Number(id) : null
})

const canCreateSku = computed(() => {
  const dims = dimensions.value
  if (dims.length > 0) {
    const allSelected = dims.every((d) => newSkuOptionIds.value[d.id])
    if (!allSelected) return false
  }
  const price = Number(newSkuPriceYuan.value)
  return newSkuPriceYuan.value !== '' && !Number.isNaN(price) && price >= 0
})

function setOptionFileInput(el, dimensionId, optionId) {
  if (el) optionFileInputs.value[`${dimensionId}-${optionId}`] = el
}

function clickOptionUpload(dimensionId, optionId) {
  const el = optionFileInputs.value[`${dimensionId}-${optionId}`]
  if (el) el.click()
}

async function load() {
  if (!spuId.value) return
  loading.value = true
  error.value = ''
  try {
    const [p, dims, imgs, s] = await Promise.all([
      getProduct(spuId.value),
      getDimensions(spuId.value),
      getProductImages(spuId.value),
      getSkus(spuId.value),
    ])
    product.value = p
    dimensions.value = dims || []
    productImages.value = imgs || []
    skus.value = s || []
    const priceMap = {}
    const displayMap = {}
    ;(s || []).forEach((sku) => {
      priceMap[sku.id] = sku.priceCents / 100
      displayMap[sku.id] = sku.displayName ?? ''
    })
    skuEditPrice.value = priceMap
    skuEditDisplayName.value = displayMap
    if (p.productType === 'SERVICE') {
      await loadAllBindings(s || [])
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function onProductFileChange(e) {
  const file = e.target.files?.[0]
  if (!file || !spuId.value) return
  e.target.value = ''
  uploading.value = true
  error.value = ''
  try {
    const { url } = await uploadFile(file)
    await addProductImage(spuId.value, { imageUrl: url })
    productImages.value = await getProductImages(spuId.value)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function onOptionFileChange(e, dimensionId, optionId) {
  const file = e.target.files?.[0]
  if (!file || !spuId.value) return
  e.target.value = ''
  uploading.value = true
  error.value = ''
  try {
    const { url } = await uploadFile(file)
    await addOptionImage(spuId.value, dimensionId, optionId, { imageUrl: url })
    dimensions.value = await getDimensions(spuId.value)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function doDeleteProductImage(imageId) {
  if (!spuId.value) return
  deleting.value = imageId
  error.value = ''
  try {
    await deleteProductImage(spuId.value, imageId)
    productImages.value = await getProductImages(spuId.value)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '删除失败'
  } finally {
    deleting.value = null
  }
}

async function doDeleteOptionImage(dimensionId, optionId, imageId) {
  if (!spuId.value) return
  deleting.value = 'o-' + optionId + '-' + imageId
  error.value = ''
  try {
    await deleteOptionImage(spuId.value, dimensionId, optionId, imageId)
    dimensions.value = await getDimensions(spuId.value)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '删除失败'
  } finally {
    deleting.value = null
  }
}

async function loadAllBindings(skuList) {
  const map = {}
  const forms = {}
  await Promise.all(
    skuList.map(async (sku) => {
      map[sku.id] = await getServiceBindings(sku.id)
      forms[sku.id] = { targetSpuId: null, priceYuan: '' }
    })
  )
  bindingsMap.value = map
  newBindingForms.value = forms
}

async function doCreateBinding(skuId) {
  const form = newBindingForms.value[skuId]
  if (!form?.targetSpuId) return
  creatingBinding.value = true
  error.value = ''
  try {
    const priceCents = form.priceYuan !== '' && form.priceYuan != null
      ? Math.round(Number(form.priceYuan) * 100)
      : null
    await createServiceBinding(skuId, { targetSpuId: form.targetSpuId, priceCents })
    bindingsMap.value[skuId] = await getServiceBindings(skuId)
    form.targetSpuId = null
    form.priceYuan = ''
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '创建绑定失败'
  } finally {
    creatingBinding.value = false
  }
}

async function doDeleteBinding(skuId, bindingId) {
  deletingBinding.value = bindingId
  error.value = ''
  try {
    await deleteServiceBinding(skuId, bindingId)
    bindingsMap.value[skuId] = await getServiceBindings(skuId)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '删除绑定失败'
  } finally {
    deletingBinding.value = null
  }
}

async function doCreateDimension() {
  if (!spuId.value || !newDimensionName.value.trim()) return
  creatingDimension.value = true
  error.value = ''
  try {
    await createDimension(spuId.value, {
      name: newDimensionName.value.trim(),
      required: newDimensionRequired.value,
    })
    dimensions.value = await getDimensions(spuId.value)
    showNewDimensionForm.value = false
    newDimensionName.value = ''
    newDimensionRequired.value = false
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '创建维度失败'
  } finally {
    creatingDimension.value = false
  }
}

async function doCreateOption(dimensionId) {
  const val = newOptionValues.value[dimensionId]?.trim()
  if (!spuId.value || !val) return
  creatingOptionDimId.value = dimensionId
  error.value = ''
  try {
    await createOption(spuId.value, dimensionId, { optionValue: val })
    dimensions.value = await getDimensions(spuId.value)
    newOptionValues.value[dimensionId] = ''
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '创建选项失败'
  } finally {
    creatingOptionDimId.value = null
  }
}

async function doDeleteOption(dimensionId, optionId) {
  if (!spuId.value) return
  deletingOptionId.value = optionId
  error.value = ''
  try {
    await apiDeleteOption(spuId.value, dimensionId, optionId)
    dimensions.value = await getDimensions(spuId.value)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '删除选项失败'
  } finally {
    deletingOptionId.value = null
  }
}

function skuDisplay(sku) {
  if (sku.displayName) return sku.displayName
  if (sku.specValues?.length) return sku.specValues.map((v) => v.optionValue).join(' · ')
  return '#' + sku.id
}

function startEditBasic() {
  if (!product.value) return
  editName.value = product.value.name ?? ''
  editDescription.value = product.value.description ?? ''
  editingBasic.value = true
}

function cancelEditBasic() {
  editingBasic.value = false
}

async function saveBasic() {
  if (!spuId.value || !editName.value.trim()) return
  savingBasic.value = true
  error.value = ''
  try {
    await updateProduct(spuId.value, {
      name: editName.value.trim(),
      description: editDescription.value || undefined,
    })
    product.value = await getProduct(spuId.value)
    editingBasic.value = false
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    savingBasic.value = false
  }
}

async function doDeleteProduct() {
  if (!spuId.value) return
  if (!confirm('确定删除该商品？此操作不可恢复。')) return
  error.value = ''
  try {
    await apiDeleteProduct(spuId.value)
    router.push('/catalog')
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '删除失败'
  }
}

function openNewSkuForm() {
  showNewSkuForm.value = true
  newSkuOptionIds.value = {}
  newSkuPriceYuan.value = ''
  newSkuDisplayName.value = ''
}

function closeNewSkuForm() {
  showNewSkuForm.value = false
}

async function doCreateSku() {
  if (!spuId.value || !canCreateSku.value) return
  creatingSku.value = true
  error.value = ''
  try {
    const dims = dimensions.value
    const specOptionIds = dims.length > 0
      ? dims.map((d) => newSkuOptionIds.value[d.id]).filter(Boolean)
      : []
    const priceCents = Math.round(Number(newSkuPriceYuan.value) * 100)
    await createSku(spuId.value, {
      specOptionIds,
      priceCents,
      displayName: newSkuDisplayName.value?.trim() || undefined,
    })
    skus.value = await getSkus(spuId.value)
    const priceMap = {}
    const displayMap = {}
    skus.value.forEach((sku) => {
      priceMap[sku.id] = sku.priceCents / 100
      displayMap[sku.id] = sku.displayName ?? ''
    })
    skuEditPrice.value = priceMap
    skuEditDisplayName.value = displayMap
    if (product.value?.productType === 'SERVICE') await loadAllBindings(skus.value)
    closeNewSkuForm()
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '创建 SKU 失败'
  } finally {
    creatingSku.value = false
  }
}

async function doUpdateSku(sku) {
  if (!spuId.value) return
  const priceCents = Math.round(Number(skuEditPrice.value[sku.id]) * 100)
  if (Number.isNaN(priceCents) || priceCents < 0) return
  savingSkuId.value = sku.id
  error.value = ''
  try {
    await updateSku(spuId.value, sku.id, {
      priceCents,
      displayName: skuEditDisplayName.value[sku.id]?.trim() || undefined,
    })
    skus.value = await getSkus(spuId.value)
    skuEditPrice.value[sku.id] = priceCents / 100
    skuEditDisplayName.value[sku.id] = skuEditDisplayName.value[sku.id]?.trim() ?? ''
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '保存 SKU 失败'
  } finally {
    savingSkuId.value = null
  }
}

async function doDeleteSku(skuId) {
  if (!spuId.value) return
  if (!confirm('确定删除该 SKU？')) return
  deletingSkuId.value = skuId
  error.value = ''
  try {
    await apiDeleteSku(spuId.value, skuId)
    skus.value = await getSkus(spuId.value)
    const priceMap = {}
    const displayMap = {}
    skus.value.forEach((sku) => {
      priceMap[sku.id] = sku.priceCents / 100
      displayMap[sku.id] = sku.displayName ?? ''
    })
    skuEditPrice.value = priceMap
    skuEditDisplayName.value = displayMap
    if (product.value?.productType === 'SERVICE') await loadAllBindings(skus.value)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '删除 SKU 失败'
  } finally {
    deletingSkuId.value = null
  }
}

watch(spuId, load, { immediate: true })
</script>
