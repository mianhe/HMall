<template>
  <BlockCard>
    <div class="p-4">
      <div class="flex items-center justify-between mb-4">
        <BlockTitle>优惠券</BlockTitle>
        <router-link
          to="/my/coupons"
          class="text-sm text-vmall-red hover:underline flex items-center gap-0.5"
        >
          我的优惠券
          <span class="text-vmall-gray-text">›</span>
        </router-link>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <IconLabel icon="🎫" label="领券中心" to="/coupons" />
        <IconLabel icon="🏷️" label="我的优惠券" to="/my/coupons" />
      </div>
    </div>
  </BlockCard>
</template>

<script setup>
import BlockCard from './BlockCard.vue'
import BlockTitle from '../atoms/BlockTitle.vue'
import IconLabel from '../molecules/IconLabel.vue'
</script>
