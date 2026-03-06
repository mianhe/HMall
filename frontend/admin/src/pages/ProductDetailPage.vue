<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <router-link
        to="/catalog"
        class="inline-flex items-center text-vmall-gray-text hover:text-vmall-red mb-6"
      >
        ← 返回 Catalog
      </router-link>

      <div v-if="loading && !product" class="text-vmall-gray-text">加载中…</div>
      <div v-else-if="error" class="text-vmall-red mb-4">{{ error }}</div>
      <template v-else-if="product">
        <div class="flex items-center gap-3 mb-2">
          <h1 class="text-2xl font-bold text-gray-800">{{ product.name }}</h1>
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
        <p v-if="product.description" class="text-vmall-gray-text mb-6">{{ product.description }}</p>
        <p v-else class="text-vmall-gray-text mb-6">—</p>

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
                  class="flex flex-wrap items-start gap-2"
                >
                  <span class="text-sm text-gray-700 py-1 shrink-0">{{ opt.optionValue }}</span>
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
            </div>
          </div>
          <p v-else class="text-vmall-gray-text">暂无规格维度</p>
        </section>

        <!-- SKU 列表（简要） -->
        <section v-if="skus.length" class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">SKU</h2>
          <ul class="text-sm text-vmall-gray-text space-y-1">
            <li v-for="sku in skus" :key="'s-' + sku.id">
              {{ skuDisplay(sku) }} — ¥{{ (sku.priceCents / 100).toFixed(2) }}
            </li>
          </ul>
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
import { useRoute } from 'vue-router'
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
} from '../shared/api/catalog.js'

const route = useRoute()
const product = ref(null)
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

const spuId = computed(() => {
  const id = route.params.id
  return id != null && id !== '' ? Number(id) : null
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

function skuDisplay(sku) {
  if (sku.displayName) return sku.displayName
  if (sku.specValues?.length) return sku.specValues.map((v) => v.optionValue).join(' · ')
  return '#' + sku.id
}

watch(spuId, load, { immediate: true })
</script>
