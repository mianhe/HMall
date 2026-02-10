<template>
  <div>
    <div
      class="flex items-center py-2 px-3 rounded-lg hover:bg-gray-50"
      :style="{ paddingLeft: `${12 + depth * 24}px` }"
    >
      <router-link
        v-if="isLeaf"
        :to="{ name: 'ProductList', query: { categoryId: node.id } }"
        class="min-w-0 flex-1 text-gray-800 hover:text-vmall-red transition-colors underline-offset-2 hover:underline"
      >
        {{ node.name }}
      </router-link>
      <span v-else class="text-gray-800 min-w-0 flex-1">{{ node.name }}</span>
      <div class="flex justify-end shrink-0 w-[108px]">
        <router-link
          v-if="!node.hasProducts"
          :to="{ name: 'CategoryNew', query: { parentId: node.id } }"
          class="px-3 py-1.5 rounded-lg border border-vmall-red text-vmall-red hover:bg-vmall-red hover:text-white transition-colors text-sm whitespace-nowrap"
        >
          新建子类别
        </router-link>
      </div>
    </div>
    <CategoryNode
      v-for="child in node.children"
      :key="child.id"
      :node="child"
      :depth="depth + 1"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import CategoryNode from './CategoryNode.vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
})

const isLeaf = computed(() => !props.node.children?.length)
</script>
