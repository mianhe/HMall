<template>
  <div class="flex flex-col h-full bg-gray-50">
    <!-- Standalone Header (hidden when embedded in ConfigPanel) -->
    <div v-if="!embedded" class="flex items-center justify-between px-4 py-3 bg-white border-b border-gray-200">
      <div class="flex items-center gap-2">
        <button
          @click="editingSkill ? cancelEdit() : $emit('back')"
          class="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h2 class="font-semibold text-gray-800">{{ editingSkill ? (editingSkill.id ? '编辑 Skill' : '新建 Skill') : 'Skill 管理' }}</h2>
      </div>
      <button
        v-if="!editingSkill"
        @click="startCreate()"
        class="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-white bg-vmall-red hover:bg-vmall-red-dark rounded-lg transition-colors"
      >
        <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
        </svg>
        新建
      </button>
    </div>

    <!-- Embedded Sub-header: only when editing inside ConfigPanel -->
    <div v-if="embedded && editingSkill" class="flex items-center justify-between px-4 py-2 bg-white border-b border-gray-100">
      <div class="flex items-center gap-2">
        <button @click="cancelEdit()" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h3 class="text-sm font-medium text-gray-700">{{ editingSkill.id ? '编辑 Skill' : '新建 Skill' }}</h3>
      </div>
    </div>

    <!-- Embedded List Toolbar: "新建" button when in list mode inside ConfigPanel -->
    <div v-if="embedded && !editingSkill" class="flex items-center justify-end px-4 py-2">
      <button
        @click="startCreate()"
        class="flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-white bg-vmall-red hover:bg-vmall-red-dark rounded-lg transition-colors"
      >
        <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
        </svg>
        新建
      </button>
    </div>

    <!-- Skill Form -->
    <div v-if="editingSkill" class="flex-1 overflow-y-auto p-4 space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">名称 <span class="text-red-400">*</span></label>
        <input
          v-model="editingSkill.name"
          type="text"
          placeholder="如：库存管理助手"
          class="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red/50"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
        <input
          v-model="editingSkill.description"
          type="text"
          placeholder="简要说明此 Skill 的用途"
          class="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red/50"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">System Prompt</label>
        <textarea
          v-model="editingSkill.systemPrompt"
          rows="8"
          placeholder="定义 AI 助手的角色、能力和行为规范……"
          class="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red/50"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">适用端</label>
        <select
          v-model="editingSkill.audience"
          class="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red/50 bg-white"
        >
          <option value="all">全部（admin + consumer）</option>
          <option value="admin">仅管理后台</option>
          <option value="consumer">仅消费者前台</option>
        </select>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          允许的工具
          <span class="text-xs text-gray-400 font-normal ml-1">（逗号分隔，支持通配符 *）</span>
        </label>
        <input
          v-model="editingSkill.allowedToolsStr"
          type="text"
          placeholder="如：inventory_*, catalog_list_skus 或 * 表示全部"
          class="w-full px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-vmall-red/30 focus:border-vmall-red/50"
        />
        <p class="text-xs text-gray-400 mt-1">留空或输入 * 表示允许所有工具</p>
      </div>

      <div v-if="formError" class="text-sm text-red-500 bg-red-50 px-3 py-2 rounded-lg">{{ formError }}</div>

      <div class="flex gap-2 pt-2">
        <button
          @click="saveSkill()"
          :disabled="saving"
          class="flex-1 px-4 py-2 text-sm font-medium text-white bg-vmall-red hover:bg-vmall-red-dark disabled:opacity-50 rounded-lg transition-colors"
        >
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <button
          @click="cancelEdit()"
          class="px-4 py-2 text-sm text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
        >
          取消
        </button>
      </div>
    </div>

    <!-- Skill List -->
    <div v-else class="flex-1 overflow-y-auto">
      <div v-if="chat.skills.value.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400">
        <svg class="w-12 h-12 mb-3 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round"
                d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
        </svg>
        <p class="text-sm">还没有 Skill</p>
        <p class="text-xs mt-1">点击右上角"新建"创建第一个 Skill</p>
      </div>

      <div v-else class="divide-y divide-gray-100">
        <div
          v-for="skill in chat.skills.value"
          :key="skill.id"
          class="px-4 py-3 hover:bg-white transition-colors"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <h3 class="text-sm font-medium text-gray-800 truncate">{{ skill.name }}</h3>
                <span v-if="skill.isDefault" class="flex-shrink-0 text-[10px] px-1.5 py-0.5 rounded-full bg-vmall-red/10 text-vmall-red font-medium">默认</span>
                <span v-if="skill.audience && skill.audience !== 'all'" class="flex-shrink-0 text-[10px] px-1.5 py-0.5 rounded-full bg-blue-50 text-blue-600 font-medium">{{ skill.audience === 'admin' ? '管理端' : '消费端' }}</span>
              </div>
              <p v-if="skill.description" class="text-xs text-gray-400 mt-0.5 truncate">{{ skill.description }}</p>
              <div v-if="skill.allowedTools && skill.allowedTools.length > 0" class="flex flex-wrap gap-1 mt-1.5">
                <span
                  v-for="tool in skill.allowedTools.slice(0, 3)"
                  :key="tool"
                  class="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-500"
                >{{ tool }}</span>
                <span v-if="skill.allowedTools.length > 3" class="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-400">
                  +{{ skill.allowedTools.length - 3 }}
                </span>
              </div>
            </div>
            <div class="flex items-center gap-0.5 ml-2 flex-shrink-0">
              <button
                v-if="!skill.isDefault"
                @click="handleSetDefault(skill.id)"
                class="p-1.5 text-gray-400 hover:text-vmall-red rounded-lg hover:bg-gray-100 transition-colors"
                title="设为默认"
              >
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                </svg>
              </button>
              <button
                @click="startEdit(skill)"
                class="p-1.5 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
                title="编辑"
              >
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button
                @click="handleDelete(skill)"
                class="p-1.5 text-gray-400 hover:text-red-500 rounded-lg hover:bg-gray-100 transition-colors"
                title="删除"
              >
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue'

defineProps({ embedded: { type: Boolean, default: false } })
defineEmits(['back'])

const chat = inject('aiChat')

const editingSkill = ref(null)
const formError = ref('')
const saving = ref(false)

function startCreate() {
  editingSkill.value = {
    id: null,
    name: '',
    description: '',
    systemPrompt: '',
    allowedToolsStr: '',
    audience: 'all',
  }
  formError.value = ''
}

function startEdit(skill) {
  editingSkill.value = {
    id: skill.id,
    name: skill.name,
    description: skill.description || '',
    systemPrompt: skill.systemPrompt || '',
    allowedToolsStr: (skill.allowedTools || []).join(', '),
    audience: skill.audience || 'all',
  }
  formError.value = ''
}

function cancelEdit() {
  editingSkill.value = null
  formError.value = ''
}

function parseAllowedTools(str) {
  if (!str || !str.trim()) return []
  return str.split(',').map(s => s.trim()).filter(Boolean)
}

async function saveSkill() {
  const skill = editingSkill.value
  if (!skill.name.trim()) {
    formError.value = '名称不能为空'
    return
  }

  saving.value = true
  formError.value = ''

  try {
    const payload = {
      name: skill.name.trim(),
      description: skill.description.trim() || null,
      systemPrompt: skill.systemPrompt.trim() || null,
      allowedTools: parseAllowedTools(skill.allowedToolsStr),
      audience: skill.audience || 'all',
    }

    if (skill.id) {
      await chat.updateSkill(skill.id, payload)
    } else {
      await chat.createSkill(payload)
    }
    editingSkill.value = null
  } catch (e) {
    formError.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleSetDefault(id) {
  try {
    await chat.setDefaultSkill(id)
  } catch (e) {
    console.warn('Failed to set default skill:', e)
  }
}

async function handleDelete(skill) {
  if (!confirm(`确定要删除 Skill「${skill.name}」吗？`)) return
  try {
    await chat.removeSkill(skill.id)
  } catch (e) {
    console.warn('Failed to delete skill:', e)
  }
}
</script>
