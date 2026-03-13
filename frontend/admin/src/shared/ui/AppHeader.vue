<template>
  <header class="bg-vmall-red text-white shadow-md">
    <div class="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
      <router-link to="/" class="font-semibold text-lg hover:opacity-90">HMall</router-link>
      <nav class="flex gap-6">
        <router-link to="/" class="hover:underline opacity-95">首页</router-link>
        <router-link to="/catalog" class="hover:underline opacity-95">Catalog</router-link>
        <router-link to="/inventory" class="hover:underline opacity-95">库存</router-link>
        <router-link to="/fulfillment" class="hover:underline opacity-95">履约</router-link>
        <router-link to="/engraving-patterns" class="hover:underline opacity-95">图案库</router-link>
        <router-link to="/events" class="hover:underline opacity-95">事件</router-link>
        <router-link to="/activity" class="hover:underline opacity-95">监控</router-link>
        <router-link to="/ops" class="hover:underline opacity-95">智能运营</router-link>
        <router-link to="/settings" class="hover:underline opacity-95">设置</router-link>
      </nav>
    </div>
  </header>
</template>

<script setup>
</script>
