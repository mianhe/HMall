<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">购物车</span>
    </nav>

    <div v-if="loading" class="text-vmall-gray-text py-12">加载中…</div>
    <div v-else-if="error" class="py-4 text-vmall-red">{{ error }}</div>

    <template v-else-if="items.length === 0">
      <div class="bg-white rounded-lg border border-vmall-gray-border p-12 text-center text-vmall-gray-text">
        <p class="text-lg mb-4">购物车是空的</p>
        <router-link to="/" class="inline-block px-4 py-2 rounded bg-vmall-red text-white hover:bg-vmall-red-hover">去逛逛</router-link>
      </div>
    </template>

    <template v-else>
      <div class="bg-white rounded-lg border border-vmall-gray-border divide-y divide-vmall-gray-border">
        <label
          v-for="item in items"
          :key="item.cartItemId"
          class="flex gap-4 p-4 items-center"
          :class="{ 'opacity-60': item.available === false }"
        >
          <input
            v-model="selectedIds"
            type="checkbox"
            :value="item.cartItemId"
            :disabled="item.available === false"
            class="rounded border-vmall-gray-border"
          />
          <div class="w-20 h-20 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center overflow-hidden">
            <img v-if="item.skuImageUrl" :src="item.skuImageUrl" :alt="item.skuName" class="w-full h-full object-contain" />
            <span v-else class="text-2xl text-vmall-gray-text">📦</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-gray-800 truncate">{{ item.skuName || 'SKU ' + item.skuId }}</p>
            <p v-if="item.available === false" class="text-sm text-vmall-red">已下架</p>
            <p class="text-vmall-red font-medium mt-1">¥ {{ formatPrice(item.skuPrice) }} × {{ item.quantity }}</p>
          </div>
          <div class="flex items-center gap-2">
            <button
              type="button"
              :disabled="item.quantity <= 1 || item.available === false"
              @click="updateQty(item, item.quantity - 1)"
              class="w-8 h-8 rounded border border-vmall-gray-border hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              −
            </button>
            <span class="w-10 text-center text-sm">{{ item.quantity }}</span>
            <button
              type="button"
              :disabled="item.available === false"
              @click="updateQty(item, item.quantity + 1)"
              class="w-8 h-8 rounded border border-vmall-gray-border hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              +
            </button>
          </div>
          <button
            type="button"
            @click="removeItem(item.cartItemId)"
            class="text-sm text-vmall-gray-text hover:text-vmall-red"
          >
            删除
          </button>
        </label>
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

async function updateQty(item, newQty) {
  if (newQty < 1) return
  try {
    await updateCartItem(item.cartItemId, newQty)
    item.quantity = newQty
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
