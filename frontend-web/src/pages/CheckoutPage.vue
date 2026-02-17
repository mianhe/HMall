<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">确认订单</span>
    </nav>

    <div v-if="!checkoutItem" class="text-vmall-gray-text py-12">
      <p>未获取到商品信息，请从商品详情页「立即购买」进入。</p>
      <router-link to="/" class="mt-4 inline-block text-vmall-red hover:underline">返回首页</router-link>
    </div>

    <template v-else>
      <!-- 商品信息 -->
      <div class="bg-white rounded-lg border border-vmall-gray-border p-4 mb-6">
        <h2 class="text-lg font-medium text-gray-800 mb-3">商品信息</h2>
        <div class="flex gap-4">
          <div class="w-20 h-20 shrink-0 bg-vmall-gray-bg rounded flex items-center justify-center text-2xl text-vmall-gray-text">📦</div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-gray-800 truncate">{{ checkoutItem.productName || checkoutItem.displayName }}</p>
            <p v-if="checkoutItem.displayName && checkoutItem.productName" class="text-sm text-vmall-gray-text">{{ checkoutItem.displayName }}</p>
            <p class="text-vmall-red font-medium mt-1">¥ {{ ((checkoutItem.unitPriceCents || 0) * (checkoutItem.quantity || 1) / 100).toFixed(2) }}</p>
            <p class="text-sm text-vmall-gray-text">¥ {{ ((checkoutItem.unitPriceCents || 0) / 100).toFixed(2) }} × {{ checkoutItem.quantity }}</p>
          </div>
        </div>
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
          合计：<span class="text-xl font-bold text-vmall-red">¥ {{ totalCents / 100 }}</span>
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
import { useAuth } from '../shared/auth.js'

const router = useRouter()
const { userId, isLoggedIn } = useAuth()

const checkoutItem = ref(null)
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

const totalCents = computed(() => {
  const item = checkoutItem.value
  if (!item) return 0
  return (item.unitPriceCents || 0) * (item.quantity || 1)
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
  if (state?.checkoutItem) {
    checkoutItem.value = state.checkoutItem
    sessionStorage.setItem('checkoutItem', JSON.stringify(state.checkoutItem))
  } else if (stored) {
    try {
      checkoutItem.value = JSON.parse(stored)
    } catch {
      sessionStorage.removeItem('checkoutItem')
    }
  }
  if (!isLoggedIn.value || !userId.value) {
    if (checkoutItem.value) {
      sessionStorage.setItem('checkoutItem', JSON.stringify(checkoutItem.value))
    }
    router.replace({ path: '/login', query: { redirect: '/checkout' } })
    return
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
  const item = checkoutItem.value
  if (!item?.skuId || !item?.quantity) {
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
      items: [{ skuId: item.skuId, quantity: item.quantity }],
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
    router.replace({ path: `/orders/${order.orderId}` })
  } catch (e) {
    if (e.code === 'ECONNABORTED') {
      error.value = '请求超时，请稍后重试'
    } else {
      error.value = e.response?.data?.message || e.message || '提交失败'
    }
  } finally {
    submitting.value = false
  }
}
</script>
