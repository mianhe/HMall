<template>
  <div class="min-h-screen bg-vmall-gray-bg text-gray-800">
    <AppHeader />
    <router-view />
  </div>
</template>

<script setup>
import AppHeader from './shared/ui/AppHeader.vue'
</script>
