<template>
  <div class="bg-white rounded-lg border border-vmall-gray-border overflow-hidden">
    <InventoryTreeNode
      v-for="node in nodes"
      :key="node.id"
      :node="node"
      :depth="0"
      :stock-by-sku-id="stockBySkuId"
      :load-stocks-for-skus="loadStocksForSkus"
      :on-save-stock="onSaveStock"
    />
  </div>
</template>

<script setup>
import InventoryTreeNode from './InventoryTreeNode.vue'

defineProps({
  nodes: { type: Array, default: () => [] },
  stockBySkuId: { type: Object, default: () => ({}) },
  loadStocksForSkus: { type: Function, default: () => {} },
  onSaveStock: { type: Function, default: () => {} },
})
</script>
