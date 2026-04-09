<template>
  <div class="max-w-5xl mx-auto px-4 py-6">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-2xl font-semibold text-gray-800">促销活动管理</h1>
      <button
        type="button"
        class="px-4 py-2 rounded bg-vmall-red text-white hover:bg-vmall-red-hover"
        @click="openCreate"
      >
        新建活动
      </button>
    </div>

    <p v-if="error" class="mb-4 px-3 py-2 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{{ error }}</p>

    <div class="bg-white border border-vmall-gray-border rounded-lg overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 text-gray-700">
          <tr>
            <th class="text-left px-3 py-2">ID</th>
            <th class="text-left px-3 py-2">名称</th>
            <th class="text-left px-3 py-2">类型</th>
            <th class="text-left px-3 py-2">规则</th>
            <th class="text-left px-3 py-2">互斥组</th>
            <th class="text-left px-3 py-2">有效期</th>
            <th class="text-left px-3 py-2">状态</th>
            <th class="text-left px-3 py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in rows" :key="a.id" class="border-t border-vmall-gray-border">
            <td class="px-3 py-2">{{ a.id }}</td>
            <td class="px-3 py-2">{{ a.name }}</td>
            <td class="px-3 py-2">{{ typeLabel(a.type) }}</td>
            <td class="px-3 py-2 text-xs">
              <div class="flex flex-wrap gap-1">
                <span v-if="hasTargetingRule(a.targetingRule)" class="px-2 py-0.5 rounded bg-blue-50 text-blue-700">定向</span>
                <span v-if="a.pieceRule" class="px-2 py-0.5 rounded bg-purple-50 text-purple-700">满件</span>
                <span v-if="!hasTargetingRule(a.targetingRule) && !a.pieceRule" class="text-vmall-gray-text">基础</span>
              </div>
            </td>
            <td class="px-3 py-2">{{ a.mutexGroupCode || '-' }}</td>
            <td class="px-3 py-2 text-xs text-gray-600">{{ fmtTime(a.startAt) }} ~ {{ fmtTime(a.endAt) }}</td>
            <td class="px-3 py-2">
              <span class="px-2 py-0.5 rounded text-xs" :class="statusClass(a.status)">{{ a.status }}</span>
            </td>
            <td class="px-3 py-2">
              <button
                v-if="a.status !== 'ACTIVE'"
                class="text-green-600 hover:underline mr-2"
                @click="activate(a.id)"
              >
                上线
              </button>
              <button
                v-if="a.status === 'ACTIVE'"
                class="text-amber-600 hover:underline"
                @click="deactivate(a.id)"
              >
                下线
              </button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="8" class="px-3 py-8 text-center text-vmall-gray-text">暂无活动</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showCreate" class="fixed inset-0 bg-black/30 flex items-center justify-center p-4 z-40">
      <div class="bg-white rounded-lg w-full max-w-xl p-5">
        <h2 class="text-lg font-semibold mb-4">创建促销活动</h2>
        <form class="space-y-3" @submit.prevent="submitCreate">
          <div>
            <label class="block text-sm text-gray-700 mb-1">名称</label>
            <input v-model.trim="form.name" required class="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-1">类型</label>
            <select v-model="form.type" class="w-full border rounded px-3 py-2">
              <option value="SKU_AMOUNT_OFF">单品直降</option>
              <option value="ORDER_AMOUNT_OFF">订单满减</option>
            </select>
          </div>
          <div v-if="form.type === 'SKU_AMOUNT_OFF'">
            <label class="block text-sm text-gray-700 mb-1">目标 SKU（逗号分隔）</label>
            <input v-model.trim="form.targetSkuIdsText" required class="w-full border rounded px-3 py-2" placeholder="101,102" />
          </div>
          <div v-else>
            <label class="block text-sm text-gray-700 mb-1">门槛金额（分）</label>
            <input v-model.number="form.thresholdCents" type="number" min="0" required class="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-1">优惠金额（分）</label>
            <input v-model.number="form.discountCents" type="number" min="1" required class="w-full border rounded px-3 py-2" />
          </div>
          <div class="rounded border border-vmall-gray-border p-3 space-y-2">
            <p class="text-sm font-medium text-gray-800">定向规则（可选）</p>
            <p class="text-xs text-vmall-gray-text">留空表示全量用户可用；逗号分隔。</p>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-700 mb-1">会员等级（levelsIn）</label>
                <input v-model.trim="form.levelsInText" class="w-full border rounded px-3 py-2" placeholder="L2,L3" />
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">任一标签（tagsAny）</label>
                <input v-model.trim="form.tagsAnyText" class="w-full border rounded px-3 py-2" placeholder="VIP,NEW_USER" />
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">必须包含标签（tagsAll）</label>
                <input v-model.trim="form.tagsAllText" class="w-full border rounded px-3 py-2" placeholder="MEMBER" />
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">排除标签（excludeTags）</label>
                <input v-model.trim="form.excludeTagsText" class="w-full border rounded px-3 py-2" placeholder="BLACKLIST" />
              </div>
            </div>
          </div>
          <div class="rounded border border-vmall-gray-border p-3 space-y-2">
            <label class="inline-flex items-center gap-2 text-sm font-medium text-gray-800">
              <input v-model="form.enablePieceRule" type="checkbox" />
              启用满件规则
            </label>
            <div v-if="form.enablePieceRule" class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-700 mb-1">范围类型</label>
                <select v-model="form.pieceScopeType" class="w-full border rounded px-3 py-2">
                  <option value="ORDER">整单</option>
                  <option value="SKU">指定 SKU</option>
                </select>
              </div>
              <div v-if="form.pieceScopeType === 'SKU'">
                <label class="block text-xs text-gray-700 mb-1">范围 SKU（逗号分隔）</label>
                <input v-model.trim="form.pieceScopeIdsText" class="w-full border rounded px-3 py-2" placeholder="101,102" />
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">满件门槛</label>
                <input v-model.number="form.pieceMinQuantity" type="number" min="1" class="w-full border rounded px-3 py-2" />
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">优惠类型</label>
                <select v-model="form.pieceDiscountType" class="w-full border rounded px-3 py-2">
                  <option value="AMOUNT_OFF">固定减</option>
                  <option value="PERCENTAGE_OFF">比例减（%）</option>
                </select>
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">优惠值</label>
                <input v-model.number="form.pieceDiscountValue" type="number" min="1" class="w-full border rounded px-3 py-2" />
              </div>
              <div>
                <label class="block text-xs text-gray-700 mb-1">最高减免（可选，分）</label>
                <input v-model.number="form.pieceMaxDiscountCents" type="number" min="0" class="w-full border rounded px-3 py-2" />
              </div>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-sm text-gray-700 mb-1">开始时间</label>
              <input v-model="form.startAt" type="datetime-local" required class="w-full border rounded px-3 py-2" />
            </div>
            <div>
              <label class="block text-sm text-gray-700 mb-1">结束时间</label>
              <input v-model="form.endAt" type="datetime-local" required class="w-full border rounded px-3 py-2" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-sm text-gray-700 mb-1">互斥组（可选）</label>
              <input v-model.trim="form.mutexGroupCode" class="w-full border rounded px-3 py-2" placeholder="MOBILE_PROMO" />
            </div>
            <div>
              <label class="block text-sm text-gray-700 mb-1">优先级</label>
              <input v-model.number="form.priority" type="number" min="0" class="w-full border rounded px-3 py-2" />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-2">
            <button type="button" class="px-4 py-2 border rounded" @click="showCreate = false">取消</button>
            <button type="submit" class="px-4 py-2 bg-vmall-red text-white rounded hover:bg-vmall-red-hover" :disabled="submitting">
              {{ submitting ? '创建中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  activatePromotionActivity,
  createPromotionActivity,
  deactivatePromotionActivity,
  getPromotionActivities,
} from '../shared/api/promotion.js'

const rows = ref([])
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const submitting = ref(false)
const form = ref(defaultForm())

function defaultForm() {
  const now = new Date()
  const start = new Date(now.getTime() + 5 * 60 * 1000)
  const end = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  return {
    name: '',
    type: 'SKU_AMOUNT_OFF',
    targetSkuIdsText: '',
    thresholdCents: 0,
    discountCents: 100,
    mutexGroupCode: '',
    priority: 0,
    startAt: toDatetimeLocal(start),
    endAt: toDatetimeLocal(end),
    levelsInText: '',
    tagsAnyText: '',
    tagsAllText: '',
    excludeTagsText: '',
    enablePieceRule: false,
    pieceScopeType: 'ORDER',
    pieceScopeIdsText: '',
    pieceMinQuantity: 2,
    pieceDiscountType: 'AMOUNT_OFF',
    pieceDiscountValue: 100,
    pieceMaxDiscountCents: null,
  }
}

function toDatetimeLocal(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toIso(localDatetime) {
  return new Date(localDatetime).toISOString()
}

function fmtTime(v) {
  if (!v) return '-'
  const d = new Date(v)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function typeLabel(type) {
  return type === 'SKU_AMOUNT_OFF' ? '单品直降' : '订单满减'
}

function statusClass(status) {
  if (status === 'ACTIVE') return 'bg-green-100 text-green-700'
  if (status === 'DRAFT') return 'bg-gray-100 text-gray-700'
  return 'bg-amber-100 text-amber-700'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await getPromotionActivities(0, 50)
    rows.value = page.content || []
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载活动失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = defaultForm()
  showCreate.value = true
}

async function submitCreate() {
  submitting.value = true
  error.value = ''
  try {
    const payload = {
      name: form.value.name,
      type: form.value.type,
      discountCents: Number(form.value.discountCents),
      mutexGroupCode: form.value.mutexGroupCode || null,
      priority: Number(form.value.priority || 0),
      startAt: toIso(form.value.startAt),
      endAt: toIso(form.value.endAt),
    }
    const targetingRule = buildTargetingRule()
    if (targetingRule) payload.targetingRule = targetingRule
    if (form.value.enablePieceRule) {
      payload.pieceRule = {
        scopeType: form.value.pieceScopeType,
        minQuantity: Number(form.value.pieceMinQuantity),
        discountType: form.value.pieceDiscountType,
        discountValue: Number(form.value.pieceDiscountValue),
      }
      if (form.value.pieceScopeType === 'SKU') {
        payload.pieceRule.scopeIds = parseCsvNumbers(form.value.pieceScopeIdsText)
      }
      if (form.value.pieceMaxDiscountCents != null && form.value.pieceMaxDiscountCents !== '') {
        payload.pieceRule.maxDiscountCents = Number(form.value.pieceMaxDiscountCents)
      }
    }
    if (form.value.type === 'SKU_AMOUNT_OFF') {
      payload.targetSkuIds = parseCsvNumbers(form.value.targetSkuIdsText)
    } else {
      payload.thresholdCents = Number(form.value.thresholdCents)
      payload.targetSkuIds = []
    }
    await createPromotionActivity(payload)
    showCreate.value = false
    await load()
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '创建失败'
  } finally {
    submitting.value = false
  }
}

function parseCsvNumbers(text) {
  return (text || '')
    .split(',')
    .map(s => Number(s.trim()))
    .filter(n => Number.isFinite(n) && n > 0)
}

function parseCsvSet(text) {
  return [...new Set(
    (text || '')
      .split(',')
      .map(s => s.trim())
      .filter(Boolean)
  )]
}

function buildTargetingRule() {
  const levelsIn = parseCsvSet(form.value.levelsInText)
  const tagsAny = parseCsvSet(form.value.tagsAnyText)
  const tagsAll = parseCsvSet(form.value.tagsAllText)
  const excludeTags = parseCsvSet(form.value.excludeTagsText)
  if (!levelsIn.length && !tagsAny.length && !tagsAll.length && !excludeTags.length) {
    return null
  }
  return { levelsIn, tagsAny, tagsAll, excludeTags }
}

function hasTargetingRule(rule) {
  if (!rule) return false
  return (rule.levelsIn?.length || rule.tagsAny?.length || rule.tagsAll?.length || rule.excludeTags?.length) > 0
}

async function activate(id) {
  try {
    await activatePromotionActivity(id)
    await load()
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '上线失败'
  }
}

async function deactivate(id) {
  try {
    await deactivatePromotionActivity(id)
    await load()
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '下线失败'
  }
}

onMounted(load)
</script>
