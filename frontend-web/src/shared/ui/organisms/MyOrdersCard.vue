<template>
  <BlockCard>
    <div class="p-4">
      <div class="flex items-center justify-between mb-4">
        <BlockTitle>我的订单</BlockTitle>
        <router-link
          to="/orders"
          class="text-sm text-vmall-red hover:underline flex items-center gap-0.5"
        >
          全部订单
          <span class="text-vmall-gray-text">›</span>
        </router-link>
      </div>
      <div class="grid grid-cols-3 gap-4">
        <IconLabel icon="💰" label="待付款" :to="{ path: '/orders', query: { status: 'PENDING_PAYMENT' } }" />
        <IconLabel icon="📦" label="待收货" :to="{ path: '/orders', query: { status: 'PAID' } }" />
        <IconLabel icon="💬" label="待评价" :to="{ path: '/orders', query: { status: 'DELIVERED' } }" />
      </div>
    </div>
  </BlockCard>
</template>

<script setup>
import BlockCard from './BlockCard.vue'
import BlockTitle from '../atoms/BlockTitle.vue'
import IconLabel from '../molecules/IconLabel.vue'
</script>
