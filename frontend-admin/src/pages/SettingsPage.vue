<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-gray-800 mb-6">系统设置</h1>

      <!-- 支付设置 -->
      <section class="bg-white rounded-lg border border-vmall-gray-border p-6 mb-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">支付设置</h2>

        <div v-if="loadError" class="text-vmall-red mb-4">{{ loadError }}</div>

        <div class="flex items-end gap-4">
          <label class="flex flex-col gap-1">
            <span class="text-sm font-medium text-gray-700">支付超时时间（分钟）</span>
            <input
              v-model.number="expireMinutes"
              type="number"
              min="1"
              :disabled="loading"
              class="w-32 px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 disabled:opacity-50"
            />
          </label>
          <button
            @click="save"
            :disabled="saving || loading"
            class="px-5 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover transition-colors disabled:opacity-50"
          >
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>

        <p v-if="saveSuccess" class="mt-3 text-green-600 text-sm">保存成功</p>
        <p v-if="saveError" class="mt-3 text-vmall-red text-sm">{{ saveError }}</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import { getPaymentSettings, updatePaymentSettings } from '../shared/api/payment.js'

const expireMinutes = ref(30)
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)
const saveSuccess = ref(false)
const saveError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getPaymentSettings()
    expireMinutes.value = data.expireMinutes
  } catch (e) {
    loadError.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  saveSuccess.value = false
  saveError.value = ''
  try {
    const data = await updatePaymentSettings({ expireMinutes: expireMinutes.value })
    expireMinutes.value = data.expireMinutes
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 3000)
  } catch (e) {
    saveError.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
