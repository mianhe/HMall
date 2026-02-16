<template>
  <BlockCard>
    <div class="p-4">
      <div class="flex items-center gap-4 mb-4">
        <div class="w-14 h-14 rounded-full bg-vmall-gray-bg flex items-center justify-center text-2xl text-vmall-gray-text shrink-0">
          👤
        </div>
        <div class="min-w-0">
          <p class="font-medium text-gray-800 truncate">{{ username || '用户' }}</p>
          <p v-if="username" class="text-sm text-vmall-gray-text">已登录</p>
        </div>
      </div>
      <div class="space-y-0 border-t border-vmall-gray-border pt-3">
        <NavRow to="/addresses">收货地址</NavRow>
      </div>
      <div class="mt-4 pt-3 border-t border-vmall-gray-border">
        <button
          type="button"
          class="w-full py-2 text-sm text-vmall-gray-text border border-vmall-gray-border rounded-lg hover:bg-gray-50 hover:text-gray-800 transition-colors"
          @click="$emit('logout')"
        >
          退出登录
        </button>
      </div>
    </div>
  </BlockCard>
</template>

<script setup>
import BlockCard from './BlockCard.vue'
import NavRow from '../molecules/NavRow.vue'

defineProps({
  username: { type: String, default: '' },
})
defineEmits(['logout'])
</script>
