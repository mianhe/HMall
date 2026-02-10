<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-gray-800 mb-4">管理后台</h1>
      <p class="text-vmall-gray-text mb-6">维护类别与商品，供前台展示。</p>
      <div class="flex flex-col gap-3">
        <router-link
          to="/categories"
          class="inline-flex items-center px-4 py-3 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors"
        >
          类别管理
        </router-link>
        <p class="text-sm text-vmall-gray-text">先维护类别，再在叶子类别下添加商品。进入类别管理后可查看子类别与商品。</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import AppHeader from '../shared/ui/AppHeader.vue'
</script>
