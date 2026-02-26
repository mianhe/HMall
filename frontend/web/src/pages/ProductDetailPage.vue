<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <!-- 面包屑：首页 > 商品名称 -->
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">{{ product?.name ?? '…' }}</span>
    </nav>

    <div v-if="loading && !product" class="text-vmall-gray-text py-12">加载中…</div>
    <div v-else-if="error" class="text-vmall-red py-4">{{ error }}</div>

    <template v-else-if="product">
      <!-- 主区域：左侧图廊 + 右侧信息（VMALL 布局） -->
      <div class="flex flex-col lg:flex-row gap-8 lg:gap-10 mb-10">
        <!-- 左侧：主图 + 缩略图（随选中规格切换，如颜色） -->
        <div class="lg:w-1/2 shrink-0">
          <div class="aspect-square bg-vmall-gray-bg rounded-lg overflow-hidden flex items-center justify-center">
            <img
              v-if="currentImage"
              :src="currentImage"
              :alt="product.name"
              class="w-full h-full object-contain"
            />
            <span v-else class="text-vmall-gray-text text-6xl">📦</span>
          </div>
          <div v-if="displayImages.length > 1" class="flex gap-2 mt-3 overflow-x-auto pb-1">
            <button
              v-for="(img, idx) in displayImages"
              :key="img.id"
              type="button"
              class="shrink-0 w-16 h-16 rounded border-2 overflow-hidden transition-colors"
              :class="currentImageIndex === idx ? 'border-vmall-red' : 'border-vmall-gray-border hover:border-vmall-red'"
              @click="currentImageIndex = idx"
            >
              <img :src="img.imageUrl" :alt="'图' + (idx + 1)" class="w-full h-full object-cover" />
            </button>
          </div>
        </div>

        <!-- 右侧：名称、价格、规格选择 -->
        <div class="lg:flex-1">
          <h1 class="text-xl lg:text-2xl font-bold text-gray-800 mb-4">{{ product.name }}</h1>

          <!-- 价格（选中 SKU 后显示） -->
          <div class="mb-6">
            <template v-if="matchedSku">
              <span class="text-2xl font-bold text-vmall-red">¥ {{ (matchedSku.priceCents / 100).toFixed(2) }}</span>
              <p v-if="selectedSpecSummary" class="text-sm text-vmall-gray-text mt-1">已选：{{ selectedSpecSummary }}</p>
            </template>
            <template v-else>
              <span class="text-xl text-vmall-gray-text">请选择规格</span>
              <p v-if="dimensions.length" class="text-sm text-vmall-gray-text mt-1">选择下方规格后显示价格</p>
            </template>
          </div>

          <!-- 规格维度：颜色、版本等 -->
          <div v-for="dim in dimensions" :key="dim.id" class="mb-5">
            <p class="text-sm font-medium text-gray-700 mb-2">
              {{ dim.name }}
              <span v-if="dim.required" class="text-vmall-red">*</span>
            </p>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="opt in (dim.options || [])"
                :key="opt.id"
                type="button"
                class="px-4 py-2 rounded border text-sm transition-colors"
                :class="selectedOptionIds[dim.id] === opt.id
                  ? 'border-vmall-red text-vmall-red bg-red-50/50'
                  : 'border-vmall-gray-border text-gray-700 hover:border-vmall-red hover:text-vmall-red'"
                @click="selectOption(dim, opt)"
              >
                {{ opt.optionValue }}
              </button>
            </div>
          </div>

          <!-- 暂无规格时提示 -->
          <p v-if="dimensions.length === 0 && skus.length > 0" class="text-sm text-vmall-gray-text">
            当前商品暂无规格维度，默认展示首个 SKU 价格。
          </p>

          <!-- 立即购买 / 加入购物车 -->
          <div class="mt-6 flex flex-wrap gap-3 items-center">
            <button
              :disabled="!matchedSku"
              @click="goCheckout"
              class="px-8 py-3 rounded-lg bg-vmall-red text-white font-medium hover:bg-vmall-red-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              立即购买
            </button>
            <button
              :disabled="!matchedSku || addingToCart"
              @click="addToCart"
              class="px-8 py-3 rounded-lg border-2 border-vmall-red text-vmall-red font-medium hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ addingToCart ? '添加中…' : '加入购物车' }}
            </button>
            <p v-if="addToCartMessage" class="w-full text-sm text-green-600">{{ addToCartMessage }}</p>
          </div>
        </div>
      </div>

      <!-- 下方 Tab：详情 / 参数 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border overflow-hidden">
        <div class="flex border-b border-vmall-gray-border">
          <button
            type="button"
            class="px-6 py-3 text-sm font-medium transition-colors"
            :class="activeTab === 'detail' ? 'text-vmall-red border-b-2 border-vmall-red -mb-px' : 'text-vmall-gray-text hover:text-vmall-red'"
            @click="activeTab = 'detail'"
          >
            详情
          </button>
          <button
            type="button"
            class="px-6 py-3 text-sm font-medium transition-colors"
            :class="activeTab === 'params' ? 'text-vmall-red border-b-2 border-vmall-red -mb-px' : 'text-vmall-gray-text hover:text-vmall-red'"
            @click="activeTab = 'params'"
          >
            参数
          </button>
        </div>
        <div class="p-6 text-vmall-gray-text">
          <div v-show="activeTab === 'detail'">
            <p v-if="product.description" class="whitespace-pre-wrap">{{ product.description }}</p>
            <p v-else>暂无详细描述</p>
          </div>
          <div v-show="activeTab === 'params'">
            <p>暂无参数信息</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProduct, getDimensions, getSkus } from '../shared/api/catalog.js'
import { addCartItem } from '../shared/api/cart.js'
import { useAuth } from '../shared/auth.js'

const route = useRoute()
const router = useRouter()
const { isLoggedIn } = useAuth()
const product = ref(null)
const dimensions = ref([])
const skus = ref([])
const loading = ref(true)
const error = ref('')
const activeTab = ref('detail')

/** 当前选中的规格：dimensionId -> optionId */
const selectedOptionIds = ref({})
/** 当前展示的图片索引 */
const currentImageIndex = ref(0)

const id = computed(() => Number(route.params.id))

/** 当前应展示的图廊：选中某规格且该选项有图时用选项图，否则用接口返回的默认图廊 product.defaultDisplayImages */
const displayImages = computed(() => {
  const dims = dimensions.value
  const selected = selectedOptionIds.value
  for (const dim of dims) {
    const optId = selected[dim.id]
    if (optId == null) continue
    const opt = (dim.options || []).find((o) => o.id === optId)
    if (opt?.images?.length) {
      return opt.images.map((img) => ({ id: img.id, imageUrl: img.imageUrl }))
    }
  }
  const defaultFromApi = product.value?.defaultDisplayImages
  if (defaultFromApi?.length) return defaultFromApi.map((img) => ({ id: img.id, imageUrl: img.imageUrl }))
  return []
})

const currentImage = computed(() => {
  if (!displayImages.value.length) return null
  const idx = Math.min(currentImageIndex.value, displayImages.value.length - 1)
  return displayImages.value[idx]?.imageUrl ?? null
})

/** 根据当前选择匹配 SKU：要求每个已选维度与 SKU 的 specValues 一致 */
const matchedSku = computed(() => {
  const dims = dimensions.value
  const selected = selectedOptionIds.value
  if (!dims.length || !skus.value.length) {
    return skus.value[0] ?? null
  }
  const requiredIds = dims.filter((d) => d.required).map((d) => d.id)
  const hasAllRequired = requiredIds.every((did) => selected[did] != null)
  if (!hasAllRequired) return null
  const selectedMap = {}
  dims.forEach((d) => {
    const optId = selected[d.id]
    if (optId == null) return
    const opt = (d.options || []).find((o) => o.id === optId)
    if (opt) selectedMap[d.name] = opt.optionValue
  })
  return skus.value.find((sku) => {
    const sv = (sku.specValues || [])
    return Object.entries(selectedMap).every(([name, value]) =>
      sv.some((s) => s.dimensionName === name && s.optionValue === value)
    )
  }) ?? null
})

const selectedSpecSummary = computed(() => {
  const sku = matchedSku.value
  if (sku?.displayName) return sku.displayName
  if (!sku?.specValues?.length) return ''
  return sku.specValues.map((s) => s.optionValue).join('·')
})

function selectOption(dim, opt) {
  selectedOptionIds.value = { ...selectedOptionIds.value, [dim.id]: opt.id }
  // 切换规格后图廊会变，重置到第一张
  currentImageIndex.value = 0
}

function goCheckout() {
  const sku = matchedSku.value
  const p = product.value
  if (!sku || !p) return
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: `/products/${id.value}` } })
    return
  }
  router.push({
    path: '/checkout',
    state: {
      checkoutItem: {
        skuId: sku.id,
        quantity: 1,
        displayName: sku.displayName || sku.specValues?.map((s) => s.optionValue).join('·') || p.name,
        unitPriceCents: sku.priceCents,
        productName: p.name,
        spuId: p.id,
      },
    },
  })
}

const addingToCart = ref(false)
const addToCartMessage = ref('')

async function addToCart() {
  const sku = matchedSku.value
  if (!sku) return
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: `/products/${id.value}` } })
    return
  }
  addingToCart.value = true
  addToCartMessage.value = ''
  try {
    await addCartItem(sku.id, 1)
    addToCartMessage.value = '已添加到购物车'
    window.dispatchEvent(new CustomEvent('cart-updated'))
    setTimeout(() => { addToCartMessage.value = '' }, 2000)
  } catch (e) {
    addToCartMessage.value = e.response?.data?.message || e.message || '添加失败'
  } finally {
    addingToCart.value = false
  }
}

async function load() {
  if (!id.value) return
  loading.value = true
  error.value = ''
  product.value = null
  dimensions.value = []
  skus.value = []
  selectedOptionIds.value = {}
  currentImageIndex.value = 0
  try {
    const [p, dims, skuList] = await Promise.all([
      getProduct(id.value),
      getDimensions(id.value).catch(() => []),
      getSkus(id.value).catch(() => []),
    ])
    product.value = p
    dimensions.value = dims?.length ? dims : []
    skus.value = skuList?.length ? skuList : []
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, () => load())
</script>
