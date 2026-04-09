<template>
  <div>
    <AppHeader />
    <main class="max-w-6xl mx-auto px-4 py-8 space-y-6">
      <h1 class="text-2xl font-bold text-gray-800">用户分群管理</h1>

      <p v-if="error" class="px-3 py-2 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{{ error }}</p>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <section class="bg-white border border-vmall-gray-border rounded-lg p-4 space-y-3">
          <h2 class="text-lg font-semibold text-gray-800">用户画像编辑</h2>
          <div class="flex gap-2">
            <input
              v-model.number="userIdInput"
              type="number"
              min="1"
              class="flex-1 border rounded px-3 py-2"
              placeholder="输入用户 ID"
              data-testid="um-user-id-input"
            />
            <button class="px-3 py-2 rounded bg-vmall-red text-white hover:bg-vmall-red-hover" @click="loadUser" data-testid="um-load-user">
              查询
            </button>
          </div>
          <template v-if="selectedUser">
            <p class="text-sm text-gray-600">用户：#{{ selectedUser.id }} / {{ selectedUser.username }}</p>
            <div class="grid grid-cols-2 gap-2">
              <div>
                <label class="block text-xs text-gray-700 mb-1">等级</label>
                <select v-model="userLevel" class="w-full border rounded px-3 py-2" data-testid="um-level-select">
                  <option value="L1">L1</option>
                  <option value="L2">L2</option>
                  <option value="L3">L3</option>
                </select>
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">标签（逗号分隔）</label>
                <input v-model.trim="userTagsText" class="w-full border rounded px-3 py-2" placeholder="VIP,NEW_USER" data-testid="um-tags-input" />
              </div>
            </div>
            <div class="flex gap-2">
              <button class="px-3 py-2 rounded border" @click="saveLevel" data-testid="um-save-level">保存等级</button>
              <button class="px-3 py-2 rounded border" @click="saveTags" data-testid="um-save-tags">保存标签</button>
            </div>
          </template>
        </section>

        <section class="bg-white border border-vmall-gray-border rounded-lg p-4 space-y-3">
          <h2 class="text-lg font-semibold text-gray-800">圈选规则</h2>
          <input v-model.trim="ruleDraft.name" class="w-full border rounded px-3 py-2" placeholder="规则名称" data-testid="um-rule-name" />
          <div class="grid grid-cols-2 gap-2">
            <input v-model.trim="ruleDraft.levelsInText" class="border rounded px-3 py-2" placeholder="levelsIn: L2,L3" />
            <input v-model.trim="ruleDraft.tagsAnyText" class="border rounded px-3 py-2" placeholder="tagsAny: VIP" />
            <input v-model.trim="ruleDraft.tagsAllText" class="border rounded px-3 py-2" placeholder="tagsAll: MEMBER" />
            <input v-model.trim="ruleDraft.excludeTagsText" class="border rounded px-3 py-2" placeholder="excludeTags: BLACKLIST" />
          </div>
          <div class="flex gap-2">
            <button class="px-3 py-2 rounded bg-vmall-red text-white hover:bg-vmall-red-hover" @click="createRule" data-testid="um-create-rule">
              创建规则
            </button>
            <button class="px-3 py-2 rounded border" :disabled="!selectedRuleId" @click="previewRule" data-testid="um-preview-rule">
              预览规则
            </button>
            <button class="px-3 py-2 rounded border" :disabled="!selectedRuleId" @click="activateRule" data-testid="um-activate-rule">
              激活规则
            </button>
          </div>

          <div v-if="previewResult" class="rounded border border-vmall-gray-border p-3 text-sm space-y-1" data-testid="um-preview-result">
            <p>命中人数：<span class="font-medium">{{ previewResult.hitCount }}</span></p>
            <p>示例用户：{{ previewResult.sampleUserIds?.join(', ') || '-' }}</p>
            <p class="text-xs text-gray-600">
              未命中原因：{{ previewResult.reasonStats?.map(x => `${x.reason}:${x.count}`).join(' / ') || '无' }}
            </p>
          </div>
        </section>
      </div>

      <section class="bg-white border border-vmall-gray-border rounded-lg overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-gray-700">
            <tr>
              <th class="text-left px-3 py-2">ID</th>
              <th class="text-left px-3 py-2">名称</th>
              <th class="text-left px-3 py-2">状态</th>
              <th class="text-left px-3 py-2">最近命中</th>
              <th class="text-left px-3 py-2">更新时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.ruleId" class="border-t border-vmall-gray-border">
              <td class="px-3 py-2">{{ rule.ruleId }}</td>
              <td class="px-3 py-2">{{ rule.name }}</td>
              <td class="px-3 py-2">{{ rule.status }}</td>
              <td class="px-3 py-2">{{ rule.lastPreviewCount ?? '-' }}</td>
              <td class="px-3 py-2 text-xs">{{ formatTime(rule.updatedAt) }}</td>
            </tr>
            <tr v-if="!rules.length">
              <td colspan="5" class="px-3 py-6 text-center text-vmall-gray-text">暂无规则</td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import {
  activateSegmentRule,
  createSegmentRule,
  getUserById,
  listSegmentRules,
  previewSegmentRule,
  updateUserLevel,
  updateUserTags,
} from '../shared/api/user.js'

const error = ref('')
const userIdInput = ref(null)
const selectedUser = ref(null)
const userLevel = ref('L1')
const userTagsText = ref('')
const rules = ref([])
const selectedRuleId = ref(null)
const previewResult = ref(null)

const ruleDraft = ref({
  name: '',
  levelsInText: '',
  tagsAnyText: '',
  tagsAllText: '',
  excludeTagsText: '',
})

function parseCsvSet(text) {
  return [...new Set(
    (text || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
  )]
}

function buildRuleCondition() {
  return {
    levelsIn: parseCsvSet(ruleDraft.value.levelsInText),
    tagsAny: parseCsvSet(ruleDraft.value.tagsAnyText),
    tagsAll: parseCsvSet(ruleDraft.value.tagsAllText),
    excludeTags: parseCsvSet(ruleDraft.value.excludeTagsText),
  }
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

async function loadRules() {
  rules.value = await listSegmentRules()
}

async function loadUser() {
  error.value = ''
  previewResult.value = null
  try {
    const user = await getUserById(userIdInput.value)
    selectedUser.value = user
    userLevel.value = user.level || 'L1'
    userTagsText.value = (user.tags || []).join(',')
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '查询用户失败'
  }
}

async function saveLevel() {
  if (!selectedUser.value) return
  error.value = ''
  try {
    const user = await updateUserLevel(selectedUser.value.id, userLevel.value)
    selectedUser.value = user
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '保存等级失败'
  }
}

async function saveTags() {
  if (!selectedUser.value) return
  error.value = ''
  try {
    const user = await updateUserTags(selectedUser.value.id, parseCsvSet(userTagsText.value))
    selectedUser.value = user
    userTagsText.value = (user.tags || []).join(',')
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '保存标签失败'
  }
}

async function createRule() {
  error.value = ''
  previewResult.value = null
  try {
    const created = await createSegmentRule({
      name: ruleDraft.value.name,
      conditions: buildRuleCondition(),
    })
    selectedRuleId.value = created.ruleId
    await loadRules()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '创建规则失败'
  }
}

async function previewRule() {
  if (!selectedRuleId.value) return
  error.value = ''
  try {
    previewResult.value = await previewSegmentRule(selectedRuleId.value, 20)
    await loadRules()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '预览失败'
  }
}

async function activateRule() {
  if (!selectedRuleId.value) return
  error.value = ''
  try {
    await activateSegmentRule(selectedRuleId.value)
    await loadRules()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '激活失败'
  }
}

onMounted(loadRules)
</script>
