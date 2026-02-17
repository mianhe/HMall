<template>
  <div>
    <!-- 类目行 -->
    <div
      v-if="node.type === 'category'"
      class="flex items-center py-2.5 px-4 border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg bg-white"
      :style="{ paddingLeft: `${16 + depth * 24}px` }"
    >
      <button
        v-if="hasChildren || node.products?.length"
        @click="expanded = !expanded"
        class="w-5 h-5 mr-2 flex items-center justify-center text-vmall-gray-text hover:text-vmall-red shrink-0"
      >
        {{ expanded ? '−' : '+' }}
      </button>
      <span v-else class="w-5 mr-2 shrink-0" />
      <span class="font-medium text-gray-800">{{ node.name }}</span>
      <span v-if="node.description" class="ml-2 text-sm text-vmall-gray-text">{{ node.description }}</span>
    </div>

    <!-- 子类目 -->
    <template v-if="node.type === 'category' && expanded && node.children?.length">
      <InventoryTreeNode
        v-for="child in node.children"
        :key="'c-' + child.id"
        :node="child"
        :depth="depth + 1"
        :stock-by-sku-id="stockBySkuId"
        :load-stocks-for-skus="loadStocksForSkus"
        :on-save-stock="onSaveStock"
      />
    </template>

    <!-- 产品与 SKU -->
    <template v-if="node.type === 'category' && expanded && node.products?.length">
      <div
        v-for="product in node.products"
        :key="'p-' + product.id"
        class="border-b border-vmall-gray-border last:border-b-0"
        :style="{ paddingLeft: `${16 + (depth + 1) * 24}px` }"
      >
        <!-- 产品名称行（可展开看 SKU） -->
        <div
          class="flex items-center py-2 px-4 hover:bg-vmall-gray-bg cursor-pointer"
          @click="toggleProduct(product)"
        >
          <button
            class="w-5 h-5 mr-2 flex items-center justify-center text-vmall-gray-text hover:text-vmall-red shrink-0"
            @click.stop="toggleProduct(product)"
          >
            {{ productExpanded[product.id] ? '−' : '+' }}
          </button>
          <span class="font-medium text-gray-800">{{ product.name }}</span>
          <span v-if="product.description" class="ml-2 text-sm text-vmall-gray-text truncate max-w-md">{{ product.description }}</span>
        </div>

        <!-- SKU 行：类别、产品、名称、描述、库存可编辑 -->
        <template v-if="productExpanded[product.id] && product.skus?.length">
          <div
            v-for="sku in product.skus"
            :key="'s-' + sku.id"
            class="flex flex-wrap items-center gap-x-4 gap-y-2 py-2 px-4 bg-gray-50/60 hover:bg-vmall-gray-bg text-sm border-t border-vmall-gray-border/50"
            :style="{ paddingLeft: `${16 + (depth + 2) * 24}px` }"
          >
            <span class="text-vmall-gray-text shrink-0">类别</span>
            <span class="font-medium text-gray-800 shrink-0">{{ node.name }}</span>
            <span class="text-vmall-gray-text shrink-0">产品</span>
            <span class="font-medium text-gray-800 shrink-0">{{ product.name }}</span>
            <span class="text-vmall-gray-text shrink-0">SKU</span>
            <span class="text-gray-700 shrink-0">{{ skuDisplay(sku) }}</span>
            <span class="text-vmall-gray-text shrink-0">描述</span>
            <span class="text-gray-600 max-w-xs truncate shrink">{{ product.description || '—' }}</span>
            <span class="text-vmall-gray-text shrink-0">可用</span>
            <template v-if="stockBySkuId[sku.id] === undefined">
              <span class="text-gray-400">加载中…</span>
            </template>
            <template v-else>
              <input
                v-model.number="editAvailable[sku.id]"
                type="number"
                min="0"
                class="w-20 px-2 py-1 rounded border border-vmall-gray-border text-gray-800 text-sm"
              />
              <span class="text-vmall-gray-text shrink-0">已占用</span>
              <span class="text-gray-700 shrink-0">{{ (stockBySkuId[sku.id] && stockBySkuId[sku.id].reserved) ?? '—' }}</span>
              <button
                @click="saveStock(sku.id)"
                :disabled="saveLoading[sku.id]"
                class="px-3 py-1 rounded bg-vmall-red text-white text-sm hover:bg-vmall-red-hover disabled:opacity-50 shrink-0"
              >
                {{ saveLoading[sku.id] ? '保存中…' : '保存' }}
              </button>
              <span v-if="saveError[sku.id]" class="text-vmall-red text-xs col-span-full">{{ saveError[sku.id] }}</span>
            </template>
          </div>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import InventoryTreeNode from './InventoryTreeNode.vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  stockBySkuId: { type: Object, default: () => ({}) },
  loadStocksForSkus: { type: Function, default: () => {} },
  onSaveStock: { type: Function, default: () => {} },
})

const expanded = ref(props.depth < 2)
const productExpanded = reactive({})
const editAvailable = reactive({})
const saveLoading = reactive({})
const saveError = reactive({})

const hasChildren = computed(() => {
  const c = props.node.children
  return c && c.length > 0
})

watch(
  () => ({ stock: props.stockBySkuId, node: props.node }),
  () => {
    if (props.node.type !== 'category' || !props.node.products) return
    for (const p of props.node.products) {
      for (const sku of p.skus || []) {
        const s = props.stockBySkuId[sku.id]
        if (s && editAvailable[sku.id] === undefined && s.available != null) {
          editAvailable[sku.id] = s.available
        }
      }
    }
  },
  { deep: true }
)

function skuDisplay(sku) {
  if (sku.displayName) return sku.displayName
  if (sku.specValues?.length) {
    return sku.specValues.map((v) => v.optionValue || '').join(' ')
  }
  return `#${sku.id}`
}

function toggleProduct(product) {
  const next = !productExpanded[product.id]
  productExpanded[product.id] = next
  if (next && product.skus?.length) {
    props.loadStocksForSkus((product.skus || []).map((s) => s.id))
  }
}

async function saveStock(skuId) {
  const val = editAvailable[skuId]
  if (val == null || val < 0) return
  saveLoading[skuId] = true
  saveError[skuId] = ''
  try {
    await props.onSaveStock(skuId, val)
    saveError[skuId] = ''
  } catch (msg) {
    saveError[skuId] = msg
  } finally {
    saveLoading[skuId] = false
  }
}
</script>
