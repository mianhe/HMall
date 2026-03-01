<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">购物车</span>
    </nav>

    <div v-if="loading" class="text-vmall-gray-text py-12">加载中…</div>
    <div v-else-if="error" class="py-4 text-vmall-red">{{ error }}</div>

    <template v-else-if="isCartEmpty">
      <div class="bg-white rounded-lg border border-vmall-gray-border py-14 px-6 text-center">
        <div class="text-6xl leading-none text-vmall-gray-border mb-4">🛒</div>
        <p class="text-xl text-gray-700 mb-2">购物车还是空的</p>
        <p class="text-sm text-vmall-gray-text mb-6">去挑几件心仪商品吧，支持实体商品与服务一起结算。</p>
        <router-link to="/" class="inline-block px-6 py-2.5 rounded-full bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors">去购物</router-link>
      </div>
    </template>

    <template v-else>
      <div class="bg-white rounded-lg border border-vmall-gray-border divide-y divide-vmall-gray-border">
        <template v-for="group in groupedItems" :key="group.primary.cartItemId">
          <!-- 实体商品行 -->
          <label class="flex gap-4 p-4 items-center" :class="{ 'opacity-60': group.primary.available === false }">
            <input
              v-model="selectedIds"
              type="checkbox"
              :value="group.primary.cartItemId"
              :disabled="group.primary.available === false"
              class="rounded border-vmall-gray-border"
            />
            <div class="w-20 h-20 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center overflow-hidden">
              <img v-if="group.primary.skuImageUrl" :src="group.primary.skuImageUrl" :alt="group.primary.skuName" class="w-full h-full object-contain" />
              <span v-else class="text-2xl text-vmall-gray-text">📦</span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-gray-800 truncate">{{ group.primary.skuName || 'SKU ' + group.primary.skuId }}</p>
              <p v-if="group.primary.available === false" class="text-sm text-vmall-red">已下架</p>
              <p class="text-vmall-red font-medium mt-1">¥ {{ formatPrice(group.primary.skuPrice) }} × {{ group.primary.quantity }}</p>
            </div>
            <div class="flex items-center gap-2">
              <button type="button" :disabled="group.primary.quantity <= 1 || group.primary.available === false" @click="updateQty(group.primary, group.primary.quantity - 1)" class="w-8 h-8 rounded border border-vmall-gray-border hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed">−</button>
              <span class="w-10 text-center text-sm">{{ group.primary.quantity }}</span>
              <button type="button" :disabled="group.primary.available === false" @click="updateQty(group.primary, group.primary.quantity + 1)" class="w-8 h-8 rounded border border-vmall-gray-border hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed">+</button>
            </div>
            <button type="button" @click="removeItem(group.primary.cartItemId)" class="text-sm text-vmall-gray-text hover:text-vmall-red">删除</button>
          </label>
          <!-- 关联服务行（缩进展示） -->
          <label
            v-for="svc in group.services"
            :key="svc.cartItemId"
            class="flex gap-4 p-4 pl-14 items-center bg-gray-50/50"
            :class="{ 'opacity-60': svc.available === false }"
          >
            <input
              v-model="selectedIds"
              type="checkbox"
              :value="svc.cartItemId"
              :disabled="svc.available === false"
              class="rounded border-vmall-gray-border"
            />
            <div class="w-14 h-14 shrink-0 bg-blue-50 rounded flex items-center justify-center text-blue-500 text-lg">🛡</div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-xs px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 font-medium">服务</span>
                <p class="font-medium text-gray-800 truncate">{{ svc.skuName || 'SKU ' + svc.skuId }}</p>
              </div>
              <p class="text-vmall-red font-medium mt-1">¥ {{ formatPrice(svc.skuPrice) }} × {{ svc.quantity }}</p>
            </div>
            <button type="button" @click="removeItem(svc.cartItemId)" class="text-sm text-vmall-gray-text hover:text-vmall-red">删除</button>
          </label>
        </template>
      </div>

      <div class="mt-6 flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-4">
        <label class="flex items-center gap-2 cursor-pointer">
          <input v-model="selectAll" type="checkbox" class="rounded border-vmall-gray-border" />
          <span class="text-gray-800">全选</span>
        </label>
        <div class="flex items-center gap-6">
          <span class="text-gray-800">
            已选 <span class="font-medium text-vmall-red">{{ selectedIds.length }}</span> 件，
            合计：<span class="text-xl font-bold text-vmall-red">¥ {{ totalSelected }}</span>
          </span>
          <button
            :disabled="selectedIds.length === 0"
            @click="goCheckout"
            class="px-6 py-3 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
          >
            去结算
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCart, updateCartItem, deleteCartItem } from '../shared/api/cart.js'
import { formatApiError } from '../shared/utils/errorMessage.js'

const router = useRouter()
const items = ref([])
const selectedIds = ref([])
const loading = ref(true)
const error = ref('')

const availableItems = computed(() => items.value.filter((i) => i.available !== false))

const groupedItems = computed(() => {
  const physicals = items.value.filter((i) => i.productType !== 'SERVICE')
  const services = items.value.filter((i) => i.productType === 'SERVICE')
  return physicals.map((p) => ({
    primary: p,
    services: services.filter((s) => s.relatedSkuId === p.skuId),
  }))
})

const isCartEmpty = computed(() => groupedItems.value.length === 0)

const selectAll = computed({
  get() {
    return availableItems.value.length > 0 && selectedIds.value.length === availableItems.value.length
  },
  set(checked) {
    selectedIds.value = checked ? availableItems.value.map((i) => i.cartItemId) : []
  },
})

const totalSelected = computed(() => {
  let sum = 0
  selectedIds.value.forEach((id) => {
    const item = items.value.find((i) => i.cartItemId === id)
    if (item && item.skuPrice != null) sum += Number(item.skuPrice) * (item.quantity || 0)
  })
  return sum.toFixed(2)
})

function formatPrice(p) {
  if (p == null) return '0.00'
  return Number(p).toFixed(2)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await getCart()
    selectedIds.value = availableItems.value.map((i) => i.cartItemId)
  } catch (e) {
    error.value = formatApiError(e, '加载失败')
  } finally {
    loading.value = false
  }
}

async function reloadCartPreserveSelection() {
  const prevSelected = new Set(selectedIds.value)
  const data = await getCart()
  items.value = data
  const availableIdSet = new Set(availableItems.value.map((i) => i.cartItemId))
  selectedIds.value = [...prevSelected].filter((id) => availableIdSet.has(id))
}

async function updateQty(item, newQty) {
  if (newQty < 1) return
  try {
    await updateCartItem(item.cartItemId, newQty)
    await reloadCartPreserveSelection()
  } catch (e) {
    error.value = formatApiError(e, '修改数量失败')
  }
}

async function removeItem(cartItemId) {
  try {
    await deleteCartItem(cartItemId)
    items.value = items.value.filter((i) => i.cartItemId !== cartItemId)
    selectedIds.value = selectedIds.value.filter((id) => id !== cartItemId)
    window.dispatchEvent(new CustomEvent('cart-updated'))
  } catch (e) {
    error.value = formatApiError(e, '删除失败')
  }
}

function goCheckout() {
  if (selectedIds.value.length === 0) return
  router.push({
    path: '/checkout',
    state: { cartCheckout: { cartItemIds: [...selectedIds.value] } },
  })
}

onMounted(load)
</script>
