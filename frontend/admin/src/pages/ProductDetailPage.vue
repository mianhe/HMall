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
        <h1 class="text-2xl font-bold text-gray-800 mb-2">{{ product.name }}</h1>
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

function skuDisplay(sku) {
  if (sku.displayName) return sku.displayName
  if (sku.specValues?.length) return sku.specValues.map((v) => v.optionValue).join(' · ')
  return '#' + sku.id
}

watch(spuId, load, { immediate: true })
</script>
