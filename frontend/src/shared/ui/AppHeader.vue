<template>
  <header class="bg-vmall-red text-white shadow-md">
    <div class="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
      <router-link to="/" class="font-semibold text-lg hover:opacity-90">HMall 管理后台</router-link>
      <nav class="flex gap-6">
        <router-link to="/" class="hover:underline opacity-95">首页</router-link>
        <router-link to="/categories" class="hover:underline opacity-95">类别管理</router-link>
      </nav>
    </div>
  </header>
</template>

<script setup>
</script>
