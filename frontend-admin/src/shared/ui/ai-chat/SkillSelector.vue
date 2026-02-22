<template>
  <div class="relative" ref="dropdownRef">
    <button
      @click="showDropdown = !showDropdown"
      class="flex items-center gap-1.5 px-2.5 py-1 text-xs rounded-lg border transition-colors"
      :class="selectedSkill
        ? 'border-vmall-red/30 bg-vmall-red/5 text-vmall-red'
        : 'border-gray-200 bg-gray-50 text-gray-500 hover:border-gray-300'"
    >
      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round"
              d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
      </svg>
      <span class="max-w-[100px] truncate">{{ selectedSkill ? selectedSkill.name : '无 Skill' }}</span>
      <svg class="w-3 h-3 transition-transform" :class="{ 'rotate-180': showDropdown }" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <Transition name="dropdown">
      <div
        v-if="showDropdown"
        class="absolute top-full left-0 mt-1 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50"
      >
        <button
          @click="selectSkill(null)"
          class="w-full px-3 py-2 text-left text-sm flex items-center gap-2 transition-colors"
          :class="!chat.selectedSkillId.value ? 'bg-vmall-red/5 text-vmall-red' : 'text-gray-600 hover:bg-gray-50'"
        >
          <span class="w-4 h-4 flex items-center justify-center">
            <svg v-if="!chat.selectedSkillId.value" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
          </span>
          <span>无 Skill（默认模式）</span>
        </button>

        <div v-if="chat.skills.value.length > 0" class="border-t border-gray-100 my-1" />

        <button
          v-for="skill in chat.skills.value"
          :key="skill.id"
          @click="selectSkill(skill.id)"
          class="w-full px-3 py-2 text-left text-sm flex items-center gap-2 transition-colors"
          :class="chat.selectedSkillId.value === skill.id ? 'bg-vmall-red/5 text-vmall-red' : 'text-gray-700 hover:bg-gray-50'"
        >
          <span class="w-4 h-4 flex items-center justify-center flex-shrink-0">
            <svg v-if="chat.selectedSkillId.value === skill.id" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
          </span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-1.5">
              <span class="truncate">{{ skill.name }}</span>
              <span v-if="skill.isDefault" class="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-vmall-red/10 text-vmall-red">默认</span>
            </div>
            <p v-if="skill.description" class="text-xs text-gray-400 truncate mt-0.5">{{ skill.description }}</p>
          </div>
        </button>

        <div class="border-t border-gray-100 my-1" />

        <button
          @click="$emit('manage'); showDropdown = false"
          class="w-full px-3 py-2 text-left text-sm text-gray-500 hover:bg-gray-50 flex items-center gap-2 transition-colors"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <span>管理 Skills</span>
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted, onUnmounted } from 'vue'

defineEmits(['manage'])

const chat = inject('aiChat')
const showDropdown = ref(false)
const dropdownRef = ref(null)

const selectedSkill = computed(() =>
  chat.skills.value.find(s => s.id === chat.selectedSkillId.value) || null
)

function selectSkill(id) {
  chat.selectedSkillId.value = id
  showDropdown.value = false
}

function handleClickOutside(e) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target)) {
    showDropdown.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.dropdown-enter-active, .dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-enter-from, .dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
