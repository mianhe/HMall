<template>
  <div class="max-w-6xl mx-auto px-4">
    <!-- 欢迎与登录入口 -->
    <div class="py-4 flex items-center justify-between">
      <p v-if="isLoggedIn" class="text-gray-700">
        欢迎回来，<span class="font-semibold text-vmall-red">{{ username }}</span>！
      </p>
      <p v-else class="text-vmall-gray-text">
        欢迎光临。请先
        <router-link to="/login" class="text-vmall-red hover:underline">登录</router-link>
        或
        <router-link to="/register" class="text-vmall-red hover:underline">注册</router-link>。
      </p>
    </div>

    <!-- 一级类目条 + 浮层：整块作为 hover 区域，避免从一级移到二级时经过空档导致浮层消失 -->
    <nav
      class="relative bg-white rounded-lg border border-vmall-gray-border shadow-sm mb-6"
      @mouseleave="onNavLeave"
    >
      <div class="flex flex-wrap gap-x-1 gap-y-2 px-4 py-3">
        <template v-for="cat in rootCategories" :key="cat.id">
          <button
            type="button"
            class="px-4 py-2 rounded-md text-gray-700 font-medium transition-colors"
            :class="hoverRootId === cat.id ? 'bg-vmall-gray-bg text-vmall-red' : 'hover:bg-vmall-gray-bg hover:text-vmall-red'"
            @mouseenter="onRootEnter(cat)"
          >
            {{ cat.name }}
          </button>
        </template>
      </div>

      <!-- 浮层：左侧二级类目 + 右侧商品（紧贴类目条，无空隙） -->
      <Transition name="panel">
        <div
          v-if="hoverRootId != null"
          class="absolute left-0 right-0 top-full -mt-px bg-white border border-t-0 border-vmall-gray-border rounded-b-lg shadow-lg z-20 min-h-[320px] flex"
          @mouseenter="panelHover = true"
        >
          <aside class="w-48 shrink-0 border-r border-vmall-gray-border bg-gray-50/50">
            <ul class="py-2">
              <li v-if="subLoading" class="px-4 py-3 text-vmall-gray-text text-sm">加载中…</li>
              <template v-else>
                <li
                  v-for="sub in subCategories"
                  :key="sub.id"
                  class="border-b border-vmall-gray-border last:border-b-0"
                >
                  <button
                    type="button"
                    class="w-full text-left px-4 py-3 text-sm font-medium transition-colors"
                    :class="selectedSubId === sub.id ? 'bg-white text-vmall-red border-l-2 border-vmall-red' : 'text-gray-700 hover:bg-white hover:text-vmall-red'"
                    @mouseenter="selectSub(sub)"
                  >
                    {{ sub.name }}
                  </button>
                </li>
                <li v-if="subCategories.length === 0" class="px-4 py-3 text-vmall-gray-text text-sm">
                  暂无子类目
                </li>
              </template>
            </ul>
          </aside>
          <main class="flex-1 p-4 overflow-auto">
            <div v-if="productsLoading" class="text-vmall-gray-text text-sm py-8">加载商品中…</div>
            <div v-else-if="products.length === 0" class="text-vmall-gray-text text-sm py-8">
              该类目下暂无商品
            </div>
            <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
              <router-link
                v-for="p in products"
                :key="p.id"
                :to="`/products/${p.id}`"
                class="group block bg-white rounded-lg border border-vmall-gray-border overflow-hidden hover:border-vmall-red hover:shadow-md transition-all"
              >
                <div class="aspect-square bg-vmall-gray-bg flex items-center justify-center">
                  <img
                    v-if="p.coverImageUrl"
                    :src="p.coverImageUrl"
                    :alt="p.name"
                    class="w-full h-full object-contain"
                  />
                  <span v-else class="text-vmall-gray-text text-4xl">📦</span>
                </div>
                <p class="p-3 text-sm font-medium text-gray-800 group-hover:text-vmall-red truncate">
                  {{ p.name }}
                </p>
                <p v-if="productMinPrice(p)" class="px-3 pb-3 text-sm text-vmall-red font-medium">
                  {{ productMinPrice(p) }} 起
                </p>
                <p v-if="productPromoHint(p)" class="px-3 pb-3 text-xs" :class="productPromoHintClass(p)">
                  {{ productPromoHint(p) }}
                </p>
              </router-link>
            </div>
          </main>
        </div>
      </Transition>
    </nav>

    <div v-if="navError" class="text-vmall-red mb-4">{{ navError }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { useAuth } from '../shared/auth.js'
import { getCategoryTree, getProducts } from '../shared/api/catalog.js'
import { previewSkuPrices } from '../shared/api/promotion.js'
import { formatPrice } from '../shared/utils/price.js'

const { isLoggedIn, username } = useAuth()

const categoryTree = ref([])
const rootCategories = ref([])
const navError = ref('')
const hoverRootId = ref(null)
const panelHover = ref(false)
const subCategories = ref([])
const subLoading = ref(false)
const selectedSubId = ref(null)
const products = ref([])
const productsLoading = ref(false)
const activityPriceBySkuId = ref({})

const productsCache = reactive({})

function productMinPrice(p) {
  const skus = p.skus || []
  if (!skus.length) return null
  const min = Math.min(...skus.map((s) => {
    const preview = activityPriceBySkuId.value[s.id]
    return preview?.activityPriceCents ?? s.priceCents
  }))
  return formatPrice(min)
}

function productPromoHint(product) {
  const skus = product.skus || []
  for (const sku of skus) {
    const preview = activityPriceBySkuId.value[sku.id]
    if (!preview) continue
    if (preview.activityLabel) return preview.activityLabel
    if (preview.ineligibleReason) return preview.ineligibleReason
  }
  return null
}

function productPromoHintClass(product) {
  const skus = product.skus || []
  for (const sku of skus) {
    const preview = activityPriceBySkuId.value[sku.id]
    if (!preview) continue
    if (preview.activityLabel) return 'text-vmall-red'
    if (preview.ineligibleReason) return 'text-vmall-gray-text'
  }
  return 'text-vmall-gray-text'
}

async function refreshSkuActivityPrices(productList) {
  const skuItems = productList.flatMap((p) => (p.skus || []).map((s) => ({
    skuId: s.id,
    unitPriceCents: s.priceCents,
  })))
  if (!skuItems.length) {
    activityPriceBySkuId.value = {}
    return
  }
  try {
    const result = await previewSkuPrices(skuItems)
    activityPriceBySkuId.value = Object.fromEntries(result.map(i => [i.skuId, i]))
  } catch {
    activityPriceBySkuId.value = {}
  }
}

async function loadCategoryTree() {
  navError.value = ''
  try {
    const tree = await getCategoryTree()
    categoryTree.value = tree
    rootCategories.value = tree.map(({ id, name }) => ({ id, name }))
  } catch (e) {
    navError.value = e.response?.data?.message || e.message || '加载类目失败'
  }
}

function getSubCategories(rootId) {
  const root = categoryTree.value.find((c) => c.id === rootId)
  return root?.children || []
}

function onRootEnter(cat) {
  hoverRootId.value = cat.id
  const subs = getSubCategories(cat.id)
  subCategories.value = subs
  subLoading.value = false
  selectedSubId.value = null
  products.value = []
  if (subs.length > 0) {
    selectSub(subs[0])
  }
}

function onNavLeave() {
  hoverRootId.value = null
  panelHover.value = false
}

function selectSub(sub) {
  selectedSubId.value = sub.id
  if (productsCache[sub.id]) {
    products.value = productsCache[sub.id]
    productsLoading.value = false
    return
  }
  products.value = []
  productsLoading.value = true
  getProducts(sub.id, { include: 'skus' })
    .then((list) => {
      productsCache[sub.id] = list
      products.value = list
      refreshSkuActivityPrices(list)
    })
    .catch(() => {
      products.value = []
      activityPriceBySkuId.value = {}
    })
    .finally(() => {
      productsLoading.value = false
    })
}

watch(hoverRootId, (id) => {
  if (id == null) panelHover.value = false
})

loadCategoryTree()
</script>

<style scoped>
.panel-enter-active,
.panel-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
