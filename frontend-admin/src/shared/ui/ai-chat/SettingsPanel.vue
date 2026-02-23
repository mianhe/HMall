<template>
  <div class="flex flex-col h-full bg-gray-50">
    <!-- Standalone Header (hidden when embedded in ConfigPanel) -->
    <div v-if="!embedded" class="flex items-center justify-between px-4 py-3 bg-white border-b border-gray-200">
      <div class="flex items-center gap-2">
        <button
          @click="$emit('back')"
          class="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h2 class="font-semibold text-gray-800">系统设置</h2>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-4 space-y-5">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center h-32 text-gray-400 text-sm">
        加载中...
      </div>

      <template v-else>
        <!-- Admin Base Prompt -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            管理端 Base Prompt
          </label>
          <p class="text-xs text-gray-400 mb-2">
            管理后台对话的基础系统提示词。支持 %s 占位符（当前页面路径）。点"恢复默认"可重置。
          </p>
          <textarea
            v-model="form.adminBasePrompt"
            rows="8"
            placeholder="系统提示词"
            class="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg resize-y focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red/50 font-mono leading-relaxed"
          />
        </div>

        <!-- Consumer Base Prompt -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            消费端 Base Prompt
          </label>
          <p class="text-xs text-gray-400 mb-2">
            消费者前台对话的基础系统提示词。支持 %s 占位符（当前页面路径）。点"恢复默认"可重置。
          </p>
          <textarea
            v-model="form.consumerBasePrompt"
            rows="8"
            placeholder="系统提示词"
            class="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg resize-y focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red/50 font-mono leading-relaxed"
          />
        </div>

        <!-- Error -->
        <div v-if="error" class="text-sm text-red-500 bg-red-50 px-3 py-2 rounded-lg">{{ error }}</div>

        <!-- Actions -->
        <div class="flex gap-2 pt-1">
          <button
            @click="save()"
            :disabled="saving"
            class="flex-1 px-4 py-2 text-sm font-medium text-white bg-vmall-red hover:bg-vmall-red-dark disabled:opacity-50 rounded-lg transition-colors"
          >
            {{ saving ? '保存中...' : '保存' }}
          </button>
          <button
            @click="resetToDefaults()"
            :disabled="saving || resetting"
            class="px-4 py-2 text-sm text-gray-600 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 rounded-lg transition-colors"
          >
            {{ resetting ? '重置中...' : '恢复默认' }}
          </button>
        </div>

        <!-- Save Success -->
        <Transition name="fade">
          <div v-if="saved" class="text-sm text-green-600 bg-green-50 px-3 py-2 rounded-lg text-center">
            已保存
          </div>
        </Transition>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSettings, updateSettings, resetSettings } from '../../api/settings.js'

defineProps({ embedded: { type: Boolean, default: false } })
defineEmits(['back'])

const loading = ref(true)
const saving = ref(false)
const resetting = ref(false)
const saved = ref(false)
const error = ref('')
const form = ref({
  adminBasePrompt: '',
  consumerBasePrompt: '',
})

let savedTimer = null

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await getSettings()
    form.value.adminBasePrompt = data.adminBasePrompt || ''
    form.value.consumerBasePrompt = data.consumerBasePrompt || ''
  } catch (e) {
    error.value = '加载设置失败: ' + (e.message || '未知错误')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  saved.value = false
  try {
    await updateSettings({
      adminBasePrompt: form.value.adminBasePrompt || '',
      consumerBasePrompt: form.value.consumerBasePrompt || '',
    })
    saved.value = true
    if (savedTimer) clearTimeout(savedTimer)
    savedTimer = setTimeout(() => { saved.value = false }, 2000)
  } catch (e) {
    error.value = '保存失败: ' + (e.response?.data?.message || e.message || '未知错误')
  } finally {
    saving.value = false
  }
}

async function resetToDefaults() {
  if (!confirm('确定恢复为系统默认提示词？当前自定义内容将被覆盖。')) return
  resetting.value = true
  error.value = ''
  saved.value = false
  try {
    const data = await resetSettings()
    form.value.adminBasePrompt = data.adminBasePrompt || ''
    form.value.consumerBasePrompt = data.consumerBasePrompt || ''
    saved.value = true
    if (savedTimer) clearTimeout(savedTimer)
    savedTimer = setTimeout(() => { saved.value = false }, 2000)
  } catch (e) {
    error.value = '重置失败: ' + (e.response?.data?.message || e.message || '未知错误')
  } finally {
    resetting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
