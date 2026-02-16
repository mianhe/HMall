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
        <!-- 商品名称行：点击进入详情页 -->
        <div class="flex items-center py-2 px-4 hover:bg-vmall-gray-bg">
          <span class="w-5 mr-2 shrink-0" />
          <router-link
            :to="'/products/' + product.id"
            class="text-gray-800 font-medium hover:text-vmall-red"
          >
            {{ product.name }}
          </router-link>
          <span v-if="product.description" class="ml-2 text-sm text-vmall-gray-text">{{ product.description }}</span>
        </div>

        <!-- 规格维度详情 -->
        <template v-if="product.dimensions?.length">
          <div
            v-for="dim in product.dimensions"
            :key="'d-' + dim.id"
            class="px-4 py-1.5"
            :style="{ paddingLeft: `${16 + (depth + 2) * 24}px` }"
          >
            <!-- 维度标题行 -->
            <div class="flex items-center gap-2 text-sm">
              <span class="text-gray-600 font-medium">{{ dim.name }}</span>
              <span v-if="dim.required" class="px-1.5 py-0.5 text-xs rounded bg-blue-50 text-blue-600">必填</span>
            </div>

            <!-- 选项列表 -->
            <div v-if="dim.options?.length" class="mt-1.5 ml-4 space-y-2">
              <div
                v-for="opt in dim.options"
                :key="'o-' + opt.id"
                class="flex items-start gap-2"
              >
                <!-- 选项值 -->
                <span class="text-sm text-gray-700 py-0.5 shrink-0">{{ opt.optionValue }}</span>
                <!-- 选项展示图（任意维度均可有图） -->
                <div v-if="opt.images?.length" class="flex flex-wrap gap-1.5">
                  <div
                    v-for="img in opt.images"
                    :key="'img-' + img.id"
                    class="relative group"
                  >
                    <img
                      :src="img.imageUrl"
                      :alt="opt.optionValue"
                      class="w-12 h-12 object-cover rounded border border-gray-200 hover:border-vmall-red transition-colors cursor-pointer"
                      @click="previewImage = img.imageUrl"
                    />
                    <span class="absolute -top-1 -right-1 hidden group-hover:flex items-center justify-center w-4 h-4 bg-gray-500 text-white text-xs rounded-full">
                      {{ img.sortOrder ?? '' }}
                    </span>
                  </div>
                </div>
                <span v-else class="text-xs text-gray-400 py-0.5 italic">无图片</span>
              </div>
            </div>
            <div v-else class="mt-1 ml-4 text-xs text-gray-400 italic">暂无选项</div>
          </div>
        </template>

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

    <!-- 图片预览遮罩 -->
    <Teleport to="body">
      <div
        v-if="previewImage"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
        @click="previewImage = null"
      >
        <img
          :src="previewImage"
          class="max-w-[80vw] max-h-[80vh] object-contain rounded-lg shadow-2xl"
          @click.stop
        />
      </div>
    </Teleport>
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
const previewImage = ref(null)

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
