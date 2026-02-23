<template>
  <div class="flex flex-col h-full bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-3 bg-white border-b border-gray-200">
      <div class="flex items-center gap-2">
        <button
          @click="$emit('back')"
          class="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h2 class="font-semibold text-gray-800">AI 配置</h2>
      </div>
    </div>

    <!-- Tab Bar -->
    <div class="flex bg-white border-b border-gray-200 px-4">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="activeTab = tab.id"
        :class="[
          'px-4 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px',
          activeTab === tab.id
            ? 'text-vmall-red border-vmall-red'
            : 'text-gray-500 border-transparent hover:text-gray-700 hover:border-gray-300'
        ]"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-hidden">
      <SkillManager v-if="activeTab === 'skills'" embedded class="h-full" />
      <SettingsPanel v-else-if="activeTab === 'settings'" embedded class="h-full" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SkillManager from './SkillManager.vue'
import SettingsPanel from './SettingsPanel.vue'

defineEmits(['back'])

const tabs = [
  { id: 'skills', label: 'Skill 管理' },
  { id: 'settings', label: '系统设置' },
]

const activeTab = ref('skills')
</script>
