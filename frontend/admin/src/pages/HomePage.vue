<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-gray-800 mb-4">HMall 管理后台</h1>
      <p class="text-vmall-gray-text mb-6">以展示为主，类目、商品、SKU 一页全览；库存管理支持按 SKU 查询与设置可用数量。增删改由 MCP 完成。</p>
      <div class="flex flex-wrap gap-3">
        <router-link
          to="/catalog"
          class="inline-flex items-center px-4 py-3 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors"
        >
          查看 Catalog
        </router-link>
        <router-link
          to="/inventory"
          class="inline-flex items-center px-4 py-3 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors"
        >
          库存管理
        </router-link>
        <router-link
          to="/fulfillment"
          class="inline-flex items-center px-4 py-3 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors"
        >
          履约管理
        </router-link>
        <router-link
          to="/activity"
          class="inline-flex items-center px-4 py-3 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors"
        >
          活动监控
        </router-link>
        <router-link
          to="/settings"
          class="inline-flex items-center px-4 py-3 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors"
        >
          系统设置
        </router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import AppHeader from '../shared/ui/AppHeader.vue'
</script>
