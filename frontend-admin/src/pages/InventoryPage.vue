<template>
  <div>
    <AppHeader />
    <main class="max-w-6xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">库存管理</h1>
        <button
          @click="load"
          :disabled="loading"
          class="px-4 py-2 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors disabled:opacity-50"
        >
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>
      <div v-if="error" class="text-vmall-red mb-4">{{ error }}</div>

      <!-- 过滤 -->
      <div v-if="flatRows.length" class="flex flex-wrap items-end gap-4 mb-4 p-4 bg-gray-50 rounded-lg border border-vmall-gray-border">
        <label class="flex flex-col gap-1">
          <span class="text-sm font-medium text-gray-700">一级类别</span>
          <select
            v-model="filterRootId"
            class="min-w-[140px] px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
          >
            <option value="">全部</option>
            <option v-for="r in rootCategories" :key="r.id" :value="r.id">{{ r.name }}</option>
          </select>
        </label>
        <label class="flex flex-col gap-1">
          <span class="text-sm font-medium text-gray-700">二级子类别</span>
          <select
            v-model="filterSubcatId"
            class="min-w-[140px] px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
            :disabled="!filterRootId || !subcatOptions.length"
          >
            <option value="">全部</option>
            <option v-for="s in subcatOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </label>
        <label class="flex flex-col gap-1">
          <span class="text-sm font-medium text-gray-700">产品名称</span>
          <input
            v-model.trim="filterProductName"
            type="text"
            placeholder="输入产品名称筛选"
            class="min-w-[180px] px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
          />
        </label>
      </div>

      <InventoryTable
        v-if="filteredRows.length"
        :rows="filteredRows"
        :stock-by-sku-id="stockBySkuId"
        :on-save-stock="handleSaveStock"
      />
      <p v-else-if="!loading && !error && flatRows.length" class="text-vmall-gray-text">当前筛选无数据。</p>
      <p v-else-if="!loading && !error" class="text-vmall-gray-text">暂无数据，请通过 MCP 添加类目与商品。</p>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import InventoryTable from '../shared/ui/InventoryTable.vue'
import { getCategories, getProducts, getDimensions, getSkus } from '../shared/api/catalog.js'
import { getStock, setStock } from '../shared/api/inventory.js'

const tree = ref([])
const loading = ref(false)
const error = ref('')
const stockBySkuId = reactive({})
const filterRootId = ref('')
const filterSubcatId = ref('')
const filterProductName = ref('')

function skuDisplay(sku) {
  if (sku.displayName) return sku.displayName
  if (sku.specValues?.length) {
    return sku.specValues.map((v) => v.optionValue || '').join(' ')
  }
  return `#${sku.id}`
}

/** 扁平化：一级类别、二级子类别、产品、SKU，仅 SKU 名称无描述 */
const flatRows = computed(() => {
  const rows = []
  for (const root of tree.value || []) {
    if (root.children?.length) {
      for (const sub of root.children) {
        for (const product of sub.products || []) {
          for (const sku of product.skus || []) {
            rows.push({
              rootId: root.id,
              rootName: root.name,
              subId: sub.id,
              subName: sub.name,
              product,
              sku,
              skuDisplayName: skuDisplay(sku),
            })
          }
        }
      }
    } else {
      for (const product of root.products || []) {
        for (const sku of product.skus || []) {
          rows.push({
            rootId: root.id,
            rootName: root.name,
            subId: null,
            subName: '—',
            product,
            sku,
            skuDisplayName: skuDisplay(sku),
          })
        }
      }
    }
  }
  return rows
})

const rootCategories = computed(() => tree.value || [])

const subcatOptions = computed(() => {
  if (!filterRootId.value) return []
  const root = tree.value?.find((r) => String(r.id) === String(filterRootId.value))
  return root?.children || []
})

const filteredRows = computed(() => {
  let list = flatRows.value
  if (filterRootId.value) {
    list = list.filter((r) => String(r.rootId) === String(filterRootId.value))
  }
  if (filterSubcatId.value) {
    list = list.filter((r) => r.subId != null && String(r.subId) === String(filterSubcatId.value))
  }
  if (filterProductName.value) {
    const q = filterProductName.value.toLowerCase()
    list = list.filter((r) => r.product.name?.toLowerCase().includes(q))
  }
  return list
})

watch(filterRootId, () => {
  filterSubcatId.value = ''
})

function isServerError(e) {
  const status = e.response?.status
  return status >= 500 || status === 502 || status === 503
}

function errorMessage(e) {
  const msg = e.response?.data?.message || e.message || ''
  if (e.response?.status === 404) {
    return msg || '未找到该 SKU 的库存记录'
  }
  if (!e.response && (e.code === 'ERR_NETWORK' || e.message?.includes('Network'))) {
    return '无法连接后端，请确认 BFF 已启动（端口 8085）'
  }
  return msg || '加载失败'
}

async function loadCategoryNode(category) {
  const [children, products] = await Promise.all([
    getCategories(category.id),
    getProducts(category.id),
  ])
  const childNodes = await Promise.all(children.map((c) => loadCategoryNode(c)))
  const productNodes = await Promise.all(
    products.map(async (p) => {
      const [dims, skus] = await Promise.all([
        getDimensions(p.id),
        getSkus(p.id),
      ])
      return { ...p, dimensions: dims, skus }
    })
  )
  return {
    type: 'category',
    ...category,
    children: childNodes,
    products: productNodes,
  }
}

async function load(opts = {}) {
  const { retries = 0 } = opts
  loading.value = true
  error.value = ''
  let lastErr = null
  try {
    const roots = await getCategories(null)
    tree.value = await Promise.all(roots.map((c) => loadCategoryNode(c)))
    return
  } catch (e) {
    lastErr = e
    if (retries > 0 && isServerError(e)) {
      await new Promise((r) => setTimeout(r, 2000))
      return load({ retries: retries - 1 })
    }
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

async function loadStocksForSkus(skuIds) {
  const missing = skuIds.filter((id) => stockBySkuId[id] === undefined)
  if (!missing.length) return
  await Promise.all(
    missing.map(async (skuId) => {
      try {
        const s = await getStock(skuId)
        stockBySkuId[skuId] = s
      } catch (e) {
        if (e.response?.status === 404) {
          stockBySkuId[skuId] = { skuId, available: 0, reserved: 0 }
        } else {
          stockBySkuId[skuId] = { skuId, available: 0, reserved: 0, _error: errorMessage(e) }
        }
      }
    })
  )
}

watch(
  flatRows,
  (rows) => {
    const ids = (rows || []).map((r) => r.sku?.id).filter(Boolean)
    if (ids.length) loadStocksForSkus(ids)
  },
  { immediate: true }
)

function saveErrorMessage(e) {
  const status = e.response?.status
  const msg = e.response?.data?.message || e.message || ''
  if (status === 502 || status === 503) {
    return msg
      ? `库存服务不可用（${msg}）。请确认 inventory-service 已启动（端口 8083）。`
      : '库存服务不可用，请确认 inventory-service 已启动（端口 8083）。'
  }
  return msg || '保存失败'
}

async function handleSaveStock(skuId, available) {
  try {
    const s = await setStock(skuId, available)
    stockBySkuId[skuId] = s
  } catch (e) {
    throw saveErrorMessage(e)
  }
}

onMounted(() => load({ retries: 2 }))
</script>
