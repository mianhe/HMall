<template>
  <div>
    <div
      class="flex items-center py-2.5 px-4 border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg cursor-pointer select-none"
      :style="{ paddingLeft: `${16 + depth * 24}px` }"
      @click="toggle"
    >
      <span class="w-5 h-5 mr-2 flex items-center justify-center text-vmall-gray-text shrink-0">
        {{ expanded ? '−' : '+' }}
      </span>
      <span class="font-medium text-gray-800">{{ node.name }}</span>
      <span v-if="node.description" class="ml-2 text-sm text-vmall-gray-text">{{ node.description }}</span>
      <span v-if="productsLoading" class="ml-2 text-xs text-vmall-gray-text">加载中…</span>
    </div>

    <template v-if="expanded">
      <CatalogTreeNode
        v-for="child in node.children"
        :key="'c-' + child.id"
        :node="child"
        :depth="depth + 1"
      />

      <div v-if="productsError" class="text-xs text-vmall-red py-1.5 px-4" :style="{ paddingLeft: `${16 + (depth + 1) * 24}px` }">
        {{ productsError }}
      </div>

      <div
        v-for="product in products"
        :key="'p-' + product.id"
        class="flex items-center py-2 px-4 border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg"
        :style="{ paddingLeft: `${16 + (depth + 1) * 24}px` }"
      >
        <span class="w-5 mr-2 shrink-0" />
        <router-link
          :to="'/products/' + product.id"
          class="text-gray-800 font-medium hover:text-vmall-red transition-colors"
        >
          {{ product.name }}
        </router-link>
        <span
          v-if="product.productType === 'SERVICE'"
          class="ml-2 px-1.5 py-0.5 text-xs rounded bg-blue-50 text-blue-600"
        >SERVICE</span>
        <span
          v-else
          class="ml-2 px-1.5 py-0.5 text-xs rounded bg-gray-100 text-gray-500"
        >PHYSICAL</span>
        <span
          v-if="product.serviceKind === 'ENGRAVING'"
          class="ml-2 px-1.5 py-0.5 text-xs rounded bg-amber-50 text-amber-700"
        >镭雕</span>
        <span v-if="product.description" class="ml-2 text-sm text-vmall-gray-text truncate">{{ product.description }}</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getProducts } from '../api/catalog.js'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
})

const expanded = ref(props.depth < 1)
const products = ref([])
const productsLoading = ref(false)
const productsError = ref('')
const loaded = ref(false)

async function loadProducts() {
  if (loaded.value) return
  productsLoading.value = true
  productsError.value = ''
  try {
    products.value = await getProducts(props.node.id)
    loaded.value = true
  } catch (e) {
    productsError.value = e.response?.data?.message || e.message || '加载商品失败'
  } finally {
    productsLoading.value = false
  }
}

function toggle() {
  expanded.value = !expanded.value
  if (expanded.value) loadProducts()
}

if (expanded.value) loadProducts()
</script>
