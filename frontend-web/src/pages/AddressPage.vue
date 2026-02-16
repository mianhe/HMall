<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <nav class="text-sm text-vmall-gray-text mb-6">
      <router-link to="/" class="hover:text-vmall-red">首页</router-link>
      <span class="mx-1">></span>
      <span class="text-gray-800">收货地址</span>
    </nav>

    <h1 class="text-xl font-bold text-gray-800 mb-6">收货地址</h1>

    <div v-if="loading && !addresses?.length" class="text-vmall-gray-text py-12">加载中…</div>
    <div v-else-if="listError" class="text-vmall-red py-4">{{ listError }}</div>

    <div v-else class="space-y-4">
      <div
        v-for="addr in addresses"
        :key="addr.addressId"
        class="bg-white rounded-lg border border-vmall-gray-border p-4 flex justify-between items-start"
      >
        <div>
          <p class="font-medium text-gray-800">{{ addr.recipientName }} {{ addr.phone }}</p>
          <p class="text-vmall-gray-text text-sm mt-1">{{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}</p>
        </div>
        <div class="flex gap-2 shrink-0">
          <button
            @click="editAddr(addr)"
            class="px-3 py-1 text-sm text-vmall-red border border-vmall-red rounded hover:bg-red-50"
          >
            编辑
          </button>
          <button
            @click="confirmDelete(addr)"
            class="px-3 py-1 text-sm text-vmall-gray-text border border-vmall-gray-border rounded hover:bg-gray-50"
          >
            删除
          </button>
        </div>
      </div>

      <button
        v-if="!showForm"
        @click="showForm = true; editingId = null; resetForm()"
        class="w-full py-4 border-2 border-dashed border-vmall-gray-border rounded-lg text-vmall-gray-text hover:border-vmall-red hover:text-vmall-red transition-colors"
      >
        + 新增收货地址
      </button>

      <div v-if="showForm" class="bg-white rounded-lg border border-vmall-gray-border p-4 space-y-3">
        <h2 class="font-medium text-gray-800">{{ editingId ? '编辑地址' : '新增地址' }}</h2>
        <div>
          <label class="block text-sm text-gray-700 mb-1">收件人 <span class="text-vmall-red">*</span></label>
          <input v-model="form.recipientName" type="text"
            class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
            placeholder="收件人姓名"
          />
        </div>
        <div>
          <label class="block text-sm text-gray-700 mb-1">手机号 <span class="text-vmall-red">*</span></label>
          <input v-model="form.phone" type="tel"
            class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
            placeholder="手机号"
          />
        </div>
        <div class="grid grid-cols-3 gap-2">
          <div>
            <label class="block text-sm text-gray-700 mb-1">省/直辖市</label>
            <input v-model="form.province" type="text"
              class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
              placeholder="如：广东省"
            />
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-1">城市</label>
            <input v-model="form.city" type="text"
              class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
              placeholder="如：深圳市"
            />
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-1">区/县</label>
            <input v-model="form.district" type="text"
              class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
              placeholder="如：南山区"
            />
          </div>
        </div>
        <div>
          <label class="block text-sm text-gray-700 mb-1">详细地址</label>
          <input v-model="form.detail" type="text"
            class="w-full px-3 py-2 border border-vmall-gray-border rounded focus:ring-2 focus:ring-vmall-red"
            placeholder="如：科技园路1号"
          />
        </div>
        <div class="flex gap-2">
          <button
            @click="saveAddress"
            class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover"
          >
            {{ editingId ? '保存' : '新增' }}
          </button>
          <button
            @click="showForm = false; editingId = null"
            class="px-4 py-2 rounded-lg border border-vmall-gray-border text-gray-700 hover:bg-gray-50"
          >
            取消
          </button>
        </div>
        <p v-if="formError" class="text-vmall-red text-sm">{{ formError }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAddresses, createAddress, updateAddress, deleteAddress } from '../shared/api/user.js'
import { useAuth } from '../shared/auth.js'

const router = useRouter()
const { userId, isLoggedIn } = useAuth()

const addresses = ref([])
const loading = ref(true)
const listError = ref('')
const showForm = ref(false)
const editingId = ref(null)
const formError = ref('')
const form = ref({
  recipientName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
})

function resetForm() {
  form.value = {
    recipientName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
  }
  formError.value = ''
}

function editAddr(addr) {
  editingId.value = addr.addressId
  form.value = {
    recipientName: addr.recipientName,
    phone: addr.phone,
    province: addr.province,
    city: addr.city,
    district: addr.district,
    detail: addr.detail,
  }
  showForm.value = true
  formError.value = ''
}

async function saveAddress() {
  const f = form.value
  if (!f.recipientName?.trim() || !f.phone?.trim() || !f.province?.trim() || !f.city?.trim() || !f.district?.trim() || !f.detail?.trim()) {
    formError.value = '请填写完整'
    return
  }
  if (!userId.value) return
  formError.value = ''
  try {
    if (editingId.value) {
      await updateAddress(userId.value, editingId.value, f)
    } else {
      await createAddress(userId.value, f)
    }
    showForm.value = false
    editingId.value = null
    resetForm()
    await load()
  } catch (e) {
    formError.value = e.response?.data?.message || e.message || '操作失败'
  }
}

async function confirmDelete(addr) {
  if (!confirm(`确定删除「${addr.recipientName}」的收货地址？`)) return
  if (!userId.value) return
  try {
    await deleteAddress(userId.value, addr.addressId)
    await load()
  } catch (e) {
    listError.value = e.response?.data?.message || e.message || '删除失败'
  }
}

async function load() {
  if (!userId.value) return
  loading.value = true
  listError.value = ''
  try {
    addresses.value = await getAddresses(userId.value)
  } catch (e) {
    listError.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!isLoggedIn.value || !userId.value) {
    router.replace({ path: '/login', query: { redirect: '/addresses' } })
    return
  }
  load()
})
</script>
