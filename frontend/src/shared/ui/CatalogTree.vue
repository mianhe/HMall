<template>
  <div class="bg-white rounded-lg border border-vmall-gray-border overflow-hidden">
    <CatalogTreeNode
      v-for="node in nodes"
      :key="node.id"
      :node="node"
      :depth="0"
    />
  </div>
</template>

<script setup>
import CatalogTreeNode from './CatalogTreeNode.vue'

defineProps({
  nodes: { type: Array, default: () => [] },
})
</script>
