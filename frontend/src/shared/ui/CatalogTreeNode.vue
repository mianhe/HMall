<template>
  <div>
    <!-- 类目行 -->
    <div
      v-if="node.type === 'category'"
      class="flex items-center py-2.5 px-4 border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg"
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
      <CatalogTreeNode
        v-for="child in node.children"
        :key="'c-' + child.id"
        :node="child"
        :depth="depth + 1"
      />
    </template>

    <!-- 商品行（在叶子类目下） -->
    <template v-if="node.type === 'category' && expanded && node.products?.length">
      <div
        v-for="product in node.products"
        :key="'p-' + product.id"
        class="border-b border-vmall-gray-border last:border-b-0"
        :style="{ paddingLeft: `${16 + (depth + 1) * 24}px` }"
      >
        <div class="flex items-center py-2 px-4 hover:bg-vmall-gray-bg">
          <span class="w-5 mr-2 shrink-0" />
          <span class="text-gray-800">{{ product.name }}</span>
          <span v-if="product.description" class="ml-2 text-sm text-vmall-gray-text">{{ product.description }}</span>
          <span v-if="product.dimensions?.length" class="ml-2 text-xs text-vmall-gray-text">
            （规格维度：{{ product.dimensions.map((d) => d.name).join('、') }}）
          </span>
        </div>
        <!-- SKU 行 -->
        <div
          v-for="sku in product.skus"
          :key="'s-' + sku.id"
          class="flex items-center py-1.5 px-4 bg-gray-50/50 hover:bg-vmall-gray-bg text-sm"
          :style="{ paddingLeft: `${16 + (depth + 2) * 24}px` }"
        >
          <span class="text-vmall-gray-text">SKU：</span>
          <span class="ml-1 text-gray-700">
            {{ skuDisplay(sku) }}
          </span>
          <span class="ml-2 font-medium text-vmall-red">¥{{ (sku.priceCents / 100).toFixed(2) }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import CatalogTreeNode from './CatalogTreeNode.vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
})

const expanded = ref(props.depth < 2)

const hasChildren = computed(() => {
  const c = props.node.children
  return c && c.length > 0
})

function skuDisplay(sku) {
  if (sku.displayName) return sku.displayName
  if (sku.specValues?.length) {
    return sku.specValues.map((v) => v.optionValue || '').join(' ')
  }
  return `#${sku.id}`
}
</script>
