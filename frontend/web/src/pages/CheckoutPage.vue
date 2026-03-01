<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">确认订单</span>
    </nav>

    <div v-if="!hasCheckoutData && !previewLoading" class="text-vmall-gray-text py-12">
      <p>未获取到商品信息，请从商品详情页「立即购买」或购物车「去结算」进入。</p>
      <router-link to="/" class="mt-4 inline-block text-vmall-red hover:underline">返回首页</router-link>
    </div>

    <div v-else-if="previewLoading" class="text-vmall-gray-text py-12">加载中…</div>

    <template v-else>
      <!-- 商品信息：单件（立即购买）或 多件（购物车） -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-4 mb-6">
        <h2 class="text-lg font-medium text-gray-800 mb-3">商品信息</h2>
        <!-- 购物车结算（有 groups 分组） -->
        <template v-if="checkoutPreviewData?.groups?.length">
          <div v-for="group in checkoutPreviewData.groups" :key="group.primaryCartItemId" class="border-b border-vmall-gray-border last:border-0 py-3">
            <div class="flex gap-4">
              <div class="w-20 h-20 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center text-2xl text-vmall-gray-text">📦</div>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-800 truncate">{{ group.primarySkuName }}</p>
                <p class="text-vmall-red font-medium mt-1">¥ {{ formatPrice(findPreviewItem(group.primaryCartItemId)?.price) }} × {{ findPreviewItem(group.primaryCartItemId)?.quantity ?? 1 }}</p>
              </div>
            </div>
            <div v-for="svc in group.serviceItems" :key="svc.cartItemId" class="flex gap-4 mt-2 ml-8">
              <div class="w-14 h-14 shrink-0 bg-blue-50 rounded flex items-center justify-center text-blue-500 text-lg">🛡</div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-xs px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 font-medium">服务</span>
                  <p class="font-medium text-gray-800 truncate">{{ svc.skuName }}</p>
                </div>
                <p class="text-vmall-red font-medium mt-1">¥ {{ formatPrice(svc.price) }} × {{ svc.quantity }}</p>
              </div>
            </div>
          </div>
        </template>
        <!-- 购物车结算（无 groups，降级平铺） -->
        <template v-else-if="checkoutPreviewData">
          <div v-for="row in checkoutPreviewData.items" :key="row.cartItemId" class="flex gap-4 py-3 border-b border-vmall-gray-border last:border-0">
            <div class="w-20 h-20 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center text-2xl text-vmall-gray-text">📦</div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-gray-800 truncate">{{ row.skuName }}</p>
              <p class="text-vmall-red font-medium mt-1">¥ {{ formatPrice(row.price) }} × {{ row.quantity }}</p>
            </div>
          </div>
        </template>
        <!-- 立即购买（含可能的服务 items） -->
        <template v-else-if="checkoutItems?.length">
          <template v-for="group in checkoutItemGroups" :key="group.primary.skuId">
            <div class="flex gap-4 py-3 border-b border-vmall-gray-border last:border-0">
              <div class="w-20 h-20 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center text-2xl text-vmall-gray-text">📦</div>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-800 truncate">{{ group.primary.productName || group.primary.displayName || ('SKU ' + group.primary.skuId) }}</p>
                <p class="text-vmall-red font-medium mt-1">¥ {{ ((group.primary.unitPriceCents || 0) / 100).toFixed(2) }} × {{ group.primary.quantity || 1 }}</p>
              </div>
            </div>
            <div v-for="(svc, sIdx) in group.services" :key="`svc-${svc.skuId}-${sIdx}`" class="flex gap-4 py-2 ml-8 border-b border-vmall-gray-border last:border-0">
              <div class="w-14 h-14 shrink-0 bg-blue-50 rounded flex items-center justify-center text-blue-500 text-lg">🛡</div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-xs px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 font-medium">服务</span>
                  <p class="font-medium text-gray-800 truncate">{{ svc.productName || svc.displayName || ('SKU ' + svc.skuId) }}</p>
                </div>
                <p class="text-vmall-red font-medium mt-1">¥ {{ ((svc.unitPriceCents || 0) / 100).toFixed(2) }} × {{ svc.quantity || 1 }}</p>
              </div>
            </div>
          </template>
        </template>
        <template v-else-if="checkoutItem">
          <div class="flex gap-4">
            <div class="w-20 h-20 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center text-2xl text-vmall-gray-text">📦</div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-gray-800 truncate">{{ checkoutItem.productName || checkoutItem.displayName }}</p>
              <p v-if="checkoutItem.displayName && checkoutItem.productName" class="text-sm text-vmall-gray-text">{{ checkoutItem.displayName }}</p>
              <p class="text-vmall-red font-medium mt-1">¥ {{ ((checkoutItem.unitPriceCents || 0) * (checkoutItem.quantity || 1) / 100).toFixed(2) }}</p>
              <p class="text-sm text-vmall-gray-text">¥ {{ ((checkoutItem.unitPriceCents || 0) / 100).toFixed(2) }} × {{ checkoutItem.quantity }}</p>
            </div>
          </div>
        </template>
      </div>

      <!-- 收货地址 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-4 mb-6">
        <h2 class="text-lg font-medium text-gray-800 mb-3">收货地址</h2>
        <router-link v-if="addresses.length" to="/addresses" class="block text-sm text-vmall-red hover:underline mb-3">管理收货地址</router-link>
        <div v-if="addresses.length" class="space-y-2 mb-4">
          <label
            v-for="addr in addresses"
            :key="addr.addressId"
            class="flex items-start gap-3 p-3 rounded border cursor-pointer transition-colors"
            :class="selectedAddressId === addr.addressId ? 'border-vmall-red bg-red-50/30' : 'border-vmall-gray-border hover:border-vmall-red/50'"
          >
            <input v-model="selectedAddressId" type="radio" :value="addr.addressId" class="mt-1" />
            <div>
              <p class="font-medium text-gray-800">{{ addr.recipientName }} {{ addr.phone }}</p>
              <p class="text-sm text-vmall-gray-text">{{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}</p>
            </div>
          </label>
        </div>
        <p v-if="addresses.length" class="text-sm text-vmall-gray-text mb-2">或新增地址：</p>
        <form class="space-y-3">
          <div>
            <label class="block text-sm text-gray-700 mb-1">收件人 <span class="text-vmall-red">*</span></label>
            <input v-model="form.recipientName" type="text" required
              class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red focus:border-transparent"
              placeholder="请输入收件人姓名"
            />
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-1">手机号 <span class="text-vmall-red">*</span></label>
            <input v-model="form.phone" type="tel" required
              class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red focus:border-transparent"
              placeholder="请输入手机号"
            />
          </div>
          <div class="grid grid-cols-3 gap-2">
            <div>
              <label class="block text-sm text-gray-700 mb-1">省/直辖市 <span class="text-vmall-red">*</span></label>
              <input v-model="form.province" type="text" required
                class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
                placeholder="如：广东省"
              />
            </div>
            <div>
              <label class="block text-sm text-gray-700 mb-1">城市 <span class="text-vmall-red">*</span></label>
              <input v-model="form.city" type="text" required
                class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
                placeholder="如：深圳市"
              />
            </div>
            <div>
              <label class="block text-sm text-gray-700 mb-1">区/县 <span class="text-vmall-red">*</span></label>
              <input v-model="form.district" type="text" required
                class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
                placeholder="如：南山区"
              />
            </div>
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-1">详细地址 <span class="text-vmall-red">*</span></label>
            <input v-model="form.detail" type="text" required
              class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red focus:border-transparent"
              placeholder="如：科技园路 1 号"
            />
          </div>
        </form>
      </div>

      <!-- 提交 -->
      <p v-if="error" class="mb-4 px-4 py-2 rounded-lg bg-red-50 text-vmall-red text-sm border border-red-200">{{ error }}</p>
      <div class="flex justify-between items-center">
        <p class="text-gray-800">
          合计：<span class="text-xl font-bold text-vmall-red">¥ {{ totalDisplay }}</span>
        </p>
        <button
          :disabled="submitting"
          @click="submitOrder"
          class="px-6 py-3 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-60 transition-colors"
        >
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { createOrder } from '../shared/api/order.js'
import { getAddresses } from '../shared/api/user.js'
import { checkoutPreview, deleteCartItems } from '../shared/api/cart.js'
import { useAuth } from '../shared/auth.js'
import { formatApiError } from '../shared/utils/errorMessage.js'

const router = useRouter()
const { userId, isLoggedIn } = useAuth()

const checkoutItem = ref(null)
const checkoutItems = ref(null)
const cartCheckoutIds = ref(null)
const checkoutPreviewData = ref(null)
const previewLoading = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const form = ref({
  recipientName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
})
const submitting = ref(false)
const error = ref('')

const hasCheckoutData = computed(() => checkoutItem.value || checkoutItems.value?.length || checkoutPreviewData.value)

const totalDisplay = computed(() => {
  if (checkoutPreviewData.value?.totalPrice != null) {
    return Number(checkoutPreviewData.value.totalPrice).toFixed(2)
  }
  if (checkoutItems.value?.length) {
    const sum = checkoutItems.value.reduce((acc, row) => {
      return acc + ((row.unitPriceCents || 0) * (row.quantity || 1) / 100)
    }, 0)
    return sum.toFixed(2)
  }
  const item = checkoutItem.value
  if (!item) return '0.00'
  return ((item.unitPriceCents || 0) * (item.quantity || 1) / 100).toFixed(2)
})

function formatPrice(p) {
  if (p == null) return '0.00'
  return Number(p).toFixed(2)
}

function findPreviewItem(cartItemId) {
  return checkoutPreviewData.value?.items?.find((i) => i.cartItemId === cartItemId) ?? null
}

const checkoutItemGroups = computed(() => {
  if (!checkoutItems.value?.length) return []
  const primaries = checkoutItems.value.filter((i) => !i.relatedSkuId)
  const services = checkoutItems.value.filter((i) => i.relatedSkuId)
  if (primaries.length === 0) return [{ primary: checkoutItems.value[0], services: checkoutItems.value.slice(1) }]
  return primaries.map((p) => ({
    primary: p,
    services: services.filter((s) => s.relatedSkuId === p.skuId),
  }))
})

watch(selectedAddressId, (id) => {
  if (!id) return
  const addr = addresses.value.find((a) => a.addressId === id)
  if (addr) {
    form.value.recipientName = addr.recipientName
    form.value.phone = addr.phone
    form.value.province = addr.province
    form.value.city = addr.city
    form.value.district = addr.district
    form.value.detail = addr.detail
  }
})

onMounted(async () => {
  const state = history.state
  const stored = sessionStorage.getItem('checkoutItem')
  const storedItems = sessionStorage.getItem('checkoutItems')

  if (state?.cartCheckout?.cartItemIds?.length) {
    cartCheckoutIds.value = state.cartCheckout.cartItemIds
    previewLoading.value = true
    try {
      checkoutPreviewData.value = await checkoutPreview(cartCheckoutIds.value)
    } catch (e) {
      error.value = formatApiError(e, '结算预览加载失败')
    } finally {
      previewLoading.value = false
    }
  } else if (state?.checkoutItems?.length) {
    checkoutItems.value = state.checkoutItems
    sessionStorage.setItem('checkoutItems', JSON.stringify(state.checkoutItems))
  } else if (state?.checkoutItem) {
    checkoutItem.value = state.checkoutItem
    sessionStorage.setItem('checkoutItem', JSON.stringify(state.checkoutItem))
  } else if (storedItems) {
    try {
      checkoutItems.value = JSON.parse(storedItems)
    } catch {
      sessionStorage.removeItem('checkoutItems')
    }
  } else if (stored) {
    try {
      checkoutItem.value = JSON.parse(stored)
    } catch {
      sessionStorage.removeItem('checkoutItem')
    }
  }

  try {
    addresses.value = await getAddresses(userId.value)
    if (addresses.value?.length && !selectedAddressId.value) {
      selectedAddressId.value = addresses.value[0].addressId
    }
  } catch {
    addresses.value = []
  }
})

async function submitOrder() {
  let orderItems
  if (checkoutPreviewData.value?.items?.length) {
    orderItems = checkoutPreviewData.value.items.map((i) => ({
      skuId: i.skuId,
      quantity: i.quantity,
      relatedSkuId: i.relatedSkuId ?? null,
    }))
  } else if (checkoutItems.value?.length) {
    orderItems = checkoutItems.value.map((i) => ({
      skuId: i.skuId,
      quantity: i.quantity || 1,
      relatedSkuId: i.relatedSkuId ?? null,
    }))
  } else if (checkoutItem.value?.skuId && checkoutItem.value?.quantity) {
    orderItems = [{
      skuId: checkoutItem.value.skuId,
      quantity: checkoutItem.value.quantity,
      relatedSkuId: checkoutItem.value.relatedSkuId ?? null,
    }]
  } else {
    error.value = '商品信息不完整'
    return
  }

  const addr = selectedAddressId.value
    ? (addresses.value.find((a) => a.addressId === selectedAddressId.value) || form.value)
    : form.value
  if (!addr?.recipientName || !addr?.phone || !addr?.province || !addr?.city || !addr?.district || !addr?.detail) {
    error.value = '请填写完整收货地址'
    return
  }
  if (!userId.value) {
    error.value = '请先登录'
    router.push('/login')
    return
  }

  error.value = ''
  submitting.value = true
  try {
    const order = await createOrder({
      userId: userId.value,
      items: orderItems,
      shippingAddress: {
        recipientName: addr.recipientName,
        phone: addr.phone,
        province: addr.province,
        city: addr.city,
        district: addr.district,
        detail: addr.detail,
      },
    })
    if (!order?.orderId) {
      error.value = '订单创建异常，请重试'
      return
    }
    sessionStorage.removeItem('checkoutItem')
    sessionStorage.removeItem('checkoutItems')
    if (cartCheckoutIds.value?.length) {
      await deleteCartItems(cartCheckoutIds.value)
      window.dispatchEvent(new CustomEvent('cart-updated'))
    }
    router.replace({ path: `/orders/${order.orderId}` })
  } catch (e) {
    error.value = formatApiError(e, '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>
