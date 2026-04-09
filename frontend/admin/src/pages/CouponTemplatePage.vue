<template>
  <div>
    <AppHeader />
    <main class="max-w-6xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">券模板管理</h1>
        <div class="flex gap-2">
          <button
            @click="openCreate"
            class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover"
          >
            新增模板
          </button>
          <button
            @click="load"
            :disabled="loading"
            class="px-4 py-2 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors disabled:opacity-50"
          >
            {{ loading ? '加载中…' : '刷新' }}
          </button>
        </div>
      </div>
      <div v-if="error" class="text-vmall-red mb-4">{{ error }}</div>

      <div v-if="list.length" class="bg-white rounded-lg border border-vmall-gray-border overflow-x-auto">
        <table class="w-full text-sm min-w-[900px]">
          <thead class="bg-gray-100 border-b border-vmall-gray-border">
            <tr>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-16">ID</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700">名称</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-24">类型</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-28">门槛</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-28">优惠</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-28">库存</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">限领</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">有效期</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-32">定向</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">状态</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in list"
              :key="row.id"
              class="border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg"
            >
              <td class="py-2 px-4 text-gray-800">{{ row.id }}</td>
              <td class="py-2 px-4 text-gray-800">{{ row.name }}</td>
              <td class="py-2 px-4">
                <span
                  :class="row.type === 'AMOUNT_OFF' ? 'bg-blue-50 text-blue-600' : 'bg-purple-50 text-purple-600'"
                  class="px-2 py-0.5 rounded text-xs font-medium"
                >
                  {{ row.type === 'AMOUNT_OFF' ? '满减' : '折扣' }}
                </span>
              </td>
              <td class="py-2 px-4 text-gray-700">{{ row.thresholdCents === 0 ? '无门槛' : '满 ' + formatYuan(row.thresholdCents) }}</td>
              <td class="py-2 px-4 text-gray-700">
                <template v-if="row.type === 'AMOUNT_OFF'">减 {{ formatYuan(row.discountCents) }}</template>
                <template v-else>{{ formatPercent(row.discountRate) }} 折<span v-if="row.maxDiscountCents">（上限 {{ formatYuan(row.maxDiscountCents) }}）</span></template>
              </td>
              <td class="py-2 px-4 text-gray-700">{{ row.issuedQuantity }} / {{ row.totalQuantity }}</td>
              <td class="py-2 px-4 text-gray-700">{{ row.perUserLimit }}</td>
              <td class="py-2 px-4 text-gray-700">{{ row.validDays }} 天</td>
              <td class="py-2 px-4 text-xs">
                <span v-if="hasTargetingRule(row.targetingRule)" class="px-2 py-0.5 rounded bg-blue-50 text-blue-700">定向券</span>
                <span v-else class="text-vmall-gray-text">全量</span>
              </td>
              <td class="py-2 px-4">
                <span
                  :class="row.status === 'ACTIVE' ? 'bg-green-50 text-green-600' : 'bg-gray-100 text-gray-500'"
                  class="px-2 py-0.5 rounded text-xs font-medium"
                >
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </span>
              </td>
              <td class="py-2 px-4 space-x-2">
                <button
                  v-if="row.status === 'ACTIVE'"
                  @click="openIssue(row)"
                  class="text-blue-600 hover:underline"
                >
                  发券
                </button>
                <button
                  v-if="row.status === 'ACTIVE'"
                  @click="doDeactivate(row)"
                  :disabled="deactivatingId === row.id"
                  class="text-red-600 hover:underline disabled:opacity-50"
                >
                  {{ deactivatingId === row.id ? '停用中…' : '停用' }}
                </button>
                <span v-if="row.status !== 'ACTIVE'" class="text-gray-400">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else-if="!loading && !error" class="text-vmall-gray-text">暂无券模板，点击「新增模板」创建。</p>

      <!-- 新增弹窗 -->
      <div
        v-if="formVisible"
        class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
        @click.self="closeForm"
      >
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">新增券模板</h2>
          <form @submit.prevent="submitForm" class="space-y-4">
            <label class="block">
              <span class="text-sm font-medium text-gray-700">模板名称</span>
              <input
                v-model.trim="form.name"
                type="text"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                placeholder="如：满100减20"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">类型</span>
              <select
                v-model="form.type"
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
              >
                <option value="AMOUNT_OFF">满减券</option>
                <option value="PERCENTAGE_OFF">折扣券</option>
              </select>
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">使用门槛（元，0 表示无门槛）</span>
              <input
                v-model.number="form.thresholdYuan"
                type="number"
                min="0"
                step="0.01"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
              />
            </label>
            <label v-if="form.type === 'AMOUNT_OFF'" class="block">
              <span class="text-sm font-medium text-gray-700">优惠金额（元）</span>
              <input
                v-model.number="form.discountYuan"
                type="number"
                min="0.01"
                step="0.01"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
              />
            </label>
            <template v-if="form.type === 'PERCENTAGE_OFF'">
              <label class="block">
                <span class="text-sm font-medium text-gray-700">折扣（如 0.85 表示 85 折）</span>
                <input
                  v-model.number="form.discountRate"
                  type="number"
                  min="0.01"
                  max="0.99"
                  step="0.01"
                  required
                  class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                />
              </label>
              <label class="block">
                <span class="text-sm font-medium text-gray-700">最高优惠上限（元，可选）</span>
                <input
                  v-model.number="form.maxDiscountYuan"
                  type="number"
                  min="0"
                  step="0.01"
                  class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                />
              </label>
            </template>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">发放总量</span>
              <input
                v-model.number="form.totalQuantity"
                type="number"
                min="1"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">每人限领</span>
              <input
                v-model.number="form.perUserLimit"
                type="number"
                min="1"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">领取后有效天数</span>
              <input
                v-model.number="form.validDays"
                type="number"
                min="1"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
              />
            </label>
            <div class="rounded border border-vmall-gray-border p-3 space-y-2">
              <p class="text-sm font-medium text-gray-700">定向规则（可选）</p>
              <p class="text-xs text-vmall-gray-text">留空表示全量用户；逗号分隔。</p>
              <div class="grid grid-cols-2 gap-2">
                <input v-model.trim="form.levelsInText" class="px-3 py-2 rounded border border-vmall-gray-border text-sm" placeholder="等级 L2,L3" />
                <input v-model.trim="form.tagsAnyText" class="px-3 py-2 rounded border border-vmall-gray-border text-sm" placeholder="任一标签 VIP,NEW_USER" />
                <input v-model.trim="form.tagsAllText" class="px-3 py-2 rounded border border-vmall-gray-border text-sm" placeholder="必须标签 MEMBER" />
                <input v-model.trim="form.excludeTagsText" class="px-3 py-2 rounded border border-vmall-gray-border text-sm" placeholder="排除标签 BLACKLIST" />
              </div>
            </div>
            <div v-if="formError" class="text-vmall-red text-sm">{{ formError }}</div>
            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                @click="closeForm"
                class="px-4 py-2 rounded-lg border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              >
                取消
              </button>
              <button
                type="submit"
                :disabled="submitting"
                class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
              >
                {{ submitting ? '创建中…' : '创建' }}
              </button>
            </div>
          </form>
        </div>
      </div>
      <!-- 发券弹窗 -->
      <div
        v-if="issueVisible"
        class="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
        @click.self="closeIssue"
      >
        <div class="bg-white rounded-lg shadow-xl max-w-sm w-full mx-4 p-6">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">发放优惠券 — {{ issueTemplate?.name }}</h2>
          <form @submit.prevent="submitIssue" class="space-y-4">
            <label class="block">
              <span class="text-sm font-medium text-gray-700">用户 ID</span>
              <input
                v-model.number="issueForm.userId"
                type="number"
                min="1"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
                placeholder="输入目标用户 ID"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-gray-700">发放数量</span>
              <input
                v-model.number="issueForm.quantity"
                type="number"
                min="1"
                required
                class="mt-1 w-full px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800"
              />
            </label>
            <div v-if="issueError" class="text-vmall-red text-sm">{{ issueError }}</div>
            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                @click="closeIssue"
                class="px-4 py-2 rounded-lg border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              >
                取消
              </button>
              <button
                type="submit"
                :disabled="issuing"
                class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
              >
                {{ issuing ? '发放中…' : '发放' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, inject, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import { getCouponTemplates, createCouponTemplate, deactivateCouponTemplate, issueCoupons } from '../shared/api/promotion.js'

const confirm = inject('confirm')
const list = ref([])
const loading = ref(false)
const error = ref('')
const formVisible = ref(false)
const formError = ref('')
const submitting = ref(false)
const deactivatingId = ref(null)

const form = reactive({
  name: '',
  type: 'AMOUNT_OFF',
  thresholdYuan: 0,
  discountYuan: null,
  discountRate: null,
  maxDiscountYuan: null,
  totalQuantity: 1000,
  perUserLimit: 3,
  validDays: 30,
  levelsInText: '',
  tagsAnyText: '',
  tagsAllText: '',
  excludeTagsText: '',
})

function formatYuan(cents) {
  return '¥' + (cents / 100).toFixed(2)
}

function formatPercent(rate) {
  return (rate * 10).toFixed(1)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await getCouponTemplates(0, 100)
    list.value = page.content || []
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.name = ''
  form.type = 'AMOUNT_OFF'
  form.thresholdYuan = 0
  form.discountYuan = null
  form.discountRate = null
  form.maxDiscountYuan = null
  form.totalQuantity = 1000
  form.perUserLimit = 3
  form.validDays = 30
  form.levelsInText = ''
  form.tagsAnyText = ''
  form.tagsAllText = ''
  form.excludeTagsText = ''
  formError.value = ''
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
}

function yuanToCents(yuan) {
  return Math.round((yuan || 0) * 100)
}

async function submitForm() {
  submitting.value = true
  formError.value = ''
  try {
    const body = {
      name: form.name,
      type: form.type,
      thresholdCents: yuanToCents(form.thresholdYuan),
      totalQuantity: form.totalQuantity,
      perUserLimit: form.perUserLimit,
      validDays: form.validDays,
    }
    const targetingRule = buildTargetingRule()
    if (targetingRule) {
      body.targetingRule = targetingRule
    }
    if (form.type === 'AMOUNT_OFF') {
      body.discountCents = yuanToCents(form.discountYuan)
    } else {
      body.discountRate = form.discountRate
      if (form.maxDiscountYuan) {
        body.maxDiscountCents = yuanToCents(form.maxDiscountYuan)
      }
    }
    await createCouponTemplate(body)
    closeForm()
    await load()
  } catch (e) {
    formError.value = e.response?.data?.message || e.message || '创建失败'
  } finally {
    submitting.value = false
  }
}

const issueVisible = ref(false)
const issueTemplate = ref(null)
const issueError = ref('')
const issuing = ref(false)
const issueForm = reactive({ userId: null, quantity: 1 })

function openIssue(row) {
  issueTemplate.value = row
  issueForm.userId = null
  issueForm.quantity = 1
  issueError.value = ''
  issueVisible.value = true
}

function closeIssue() {
  issueVisible.value = false
}

async function submitIssue() {
  issuing.value = true
  issueError.value = ''
  try {
    await issueCoupons(issueTemplate.value.id, issueForm.userId, issueForm.quantity)
    closeIssue()
    await load()
  } catch (e) {
    issueError.value = e.response?.data?.message || e.message || '发放失败'
  } finally {
    issuing.value = false
  }
}

async function doDeactivate(row) {
  if (!(await confirm.confirm({ title: '停用模板', message: `确定停用券模板「${row.name}」？停用后将不再发放新券。` }))) return
  deactivatingId.value = row.id
  error.value = ''
  try {
    await deactivateCouponTemplate(row.id)
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '停用失败'
  } finally {
    deactivatingId.value = null
  }
}

onMounted(load)

function parseCsvSet(text) {
  return [...new Set(
    (text || '')
      .split(',')
      .map(s => s.trim())
      .filter(Boolean)
  )]
}

function buildTargetingRule() {
  const levelsIn = parseCsvSet(form.levelsInText)
  const tagsAny = parseCsvSet(form.tagsAnyText)
  const tagsAll = parseCsvSet(form.tagsAllText)
  const excludeTags = parseCsvSet(form.excludeTagsText)
  if (!levelsIn.length && !tagsAny.length && !tagsAll.length && !excludeTags.length) {
    return null
  }
  return { levelsIn, tagsAny, tagsAll, excludeTags }
}

function hasTargetingRule(rule) {
  if (!rule) return false
  return (rule.levelsIn?.length || rule.tagsAny?.length || rule.tagsAll?.length || rule.excludeTags?.length) > 0
}
</script>
