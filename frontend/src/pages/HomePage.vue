<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-gray-800 mb-4">HMall 管理后台</h1>
      <p class="text-vmall-gray-text mb-6">以展示为主，类目、商品、SKU 一页全览。增删改由 MCP 完成。</p>
      <router-link
        to="/catalog"
        class="inline-flex items-center px-4 py-3 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors"
      >
        查看 Catalog
      </router-link>
    </main>
  </div>
</template>

<script setup>
import AppHeader from '../shared/ui/AppHeader.vue'
</script>
