<template>
  <div>
    <AppHeader />
    <main class="max-w-[1600px] mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">履约管理</h1>
        <button
          @click="load"
          :disabled="loading"
          class="px-4 py-2 rounded-lg bg-white border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg transition-colors disabled:opacity-50"
        >
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>
      <div v-if="error" class="text-vmall-red mb-4">{{ error }}</div>

      <!-- 过滤 -->
      <div class="flex flex-wrap items-end gap-4 mb-4 p-4 bg-gray-50 rounded-lg border border-vmall-gray-border">
        <label class="flex flex-col gap-1">
          <span class="text-sm font-medium text-gray-700">订单 ID</span>
          <input
            v-model.trim="filterOrderId"
            type="text"
            placeholder="留空查全部"
            class="min-w-[120px] px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
          />
        </label>
        <label class="flex flex-col gap-1">
          <span class="text-sm font-medium text-gray-700">状态</span>
          <select
            v-model="filterStatus"
            class="min-w-[140px] px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
          >
            <option value="">全部</option>
            <option value="CREATED">已创建</option>
            <option value="ALLOCATING">配货中</option>
            <option value="SHIPPED">已发货</option>
            <option value="DELIVERED">已签收</option>
            <option value="ACTIVATED">已激活</option>
            <option value="CANCELLED">已取消</option>
          </select>
        </label>
        <label class="flex flex-col gap-1">
          <span class="text-sm font-medium text-gray-700">履约类型</span>
          <select
            v-model="filterType"
            class="min-w-[140px] px-3 py-2 rounded-lg border border-vmall-gray-border text-gray-800 bg-white"
          >
            <option value="">全部</option>
            <option value="PHYSICAL">实体</option>
            <option value="VIRTUAL">虚拟</option>
          </select>
        </label>
        <button
          @click="load"
          :disabled="loading"
          class="px-4 py-2 rounded-lg bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
        >
          查询
        </button>
      </div>

      <div v-if="list.length" class="bg-white rounded-lg border border-vmall-gray-border overflow-x-auto">
        <table class="w-full text-sm min-w-[1200px]">
          <thead class="bg-gray-100 border-b border-vmall-gray-border">
            <tr>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-24">履约单ID</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">订单ID</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-16">类型</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">状态</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-28">镭雕</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700">商品摘要</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">收货人</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700">收货地址</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-20">承运商</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-28">物流单号</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-32">发货时间</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-32">签收时间</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-32">创建时间</th>
              <th class="text-left py-3 px-4 font-medium text-gray-700 w-40">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in list"
              :key="row.fulfillmentOrderId"
              class="border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg"
            >
              <td class="py-2 px-4 text-gray-800">{{ row.fulfillmentOrderId }}</td>
              <td class="py-2 px-4 text-gray-800">{{ row.orderId }}</td>
              <td class="py-2 px-4">
                <span :class="typeClass(row.fulfillmentType)" class="px-2 py-0.5 rounded text-xs font-medium">
                  {{ typeLabel(row.fulfillmentType) }}
                </span>
              </td>
              <td class="py-2 px-4">
                <span :class="statusClass(row.status)" class="px-2 py-0.5 rounded text-xs font-medium">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
              <td class="py-2 px-4 text-gray-700 text-xs">
                <template v-if="row.engravingInfo">
                  <span class="block truncate max-w-[140px]" :title="engravingPreview(row.engravingInfo)">
                    {{ engravingPreview(row.engravingInfo) }}
                  </span>
                  <span v-if="row.engravingCompletedAt" class="text-green-600">✓ 已完成</span>
                  <span v-else class="text-amber-600">待完成</span>
                </template>
                <span v-else>—</span>
              </td>
              <td class="py-2 px-4 text-gray-700">{{ itemsSummary(row.items) }}</td>
              <td class="py-2 px-4 text-gray-700">{{ row.shippingAddress?.recipientName ?? '—' }}</td>
              <td class="py-2 px-4 text-gray-700 max-w-[180px] truncate" :title="fullAddress(row)">{{ shortAddress(row) }}</td>
              <td class="py-2 px-4 text-gray-700">{{ row.shippingInfo?.carrier ?? '—' }}</td>
              <td class="py-2 px-4 text-gray-700">{{ row.shippingInfo?.trackingNumber ?? '—' }}</td>
              <td class="py-2 px-4 text-gray-600">{{ formatTime(row.shippingInfo?.shippedAt) }}</td>
              <td class="py-2 px-4 text-gray-600">{{ formatTime(row.shippingInfo?.deliveredAt) }}</td>
              <td class="py-2 px-4 text-gray-600">{{ formatTime(row.createdAt) }}</td>
              <td class="py-2 px-4">
                <div class="flex flex-wrap gap-2 items-center">
                  <!-- 虚拟履约单无需物流操作 -->
                  <template v-if="row.fulfillmentType === 'VIRTUAL'">
                    <span class="text-xs text-gray-400">{{ row.status === 'ACTIVATED' ? '已自动激活' : '—' }}</span>
                  </template>
                  <template v-else-if="row.status === 'CREATED'">
                    <button
                      @click="doAllocate(row)"
                      :disabled="actionLoading[row.fulfillmentOrderId]"
                      class="whitespace-nowrap px-3 py-1 rounded bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50 text-sm"
                    >
                      {{ actionLoading[row.fulfillmentOrderId] ? '处理中…' : '开始配货' }}
                    </button>
                  </template>
                  <template v-else-if="row.status === 'ALLOCATING'">
                    <button
                      v-if="hasEngravingPending(row)"
                      @click="doCompleteEngraving(row)"
                      :disabled="actionLoading[row.fulfillmentOrderId]"
                      class="whitespace-nowrap px-3 py-1 rounded bg-amber-600 text-white hover:bg-amber-700 disabled:opacity-50 text-sm"
                    >
                      {{ actionLoading[row.fulfillmentOrderId] ? '处理中…' : '完成镭雕' }}
                    </button>
                    <input
                      v-model="shipForm[row.fulfillmentOrderId].carrier"
                      placeholder="承运商"
                      class="w-20 px-2 py-1 rounded border border-vmall-gray-border text-gray-800 text-xs"
                    />
                    <input
                      v-model="shipForm[row.fulfillmentOrderId].trackingNumber"
                      placeholder="物流单号"
                      class="w-24 px-2 py-1 rounded border border-vmall-gray-border text-gray-800 text-xs"
                    />
                    <button
                      @click="doShip(row)"
                      :disabled="actionLoading[row.fulfillmentOrderId] || !canShip(row)"
                      :title="hasEngravingPending(row) ? '须先完成镭雕' : ''"
                      class="whitespace-nowrap px-3 py-1 rounded bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50 text-sm"
                    >
                      {{ actionLoading[row.fulfillmentOrderId] ? '处理中…' : '发货' }}
                    </button>
                  </template>
                  <template v-else-if="row.status === 'SHIPPED'">
                    <button
                      @click="doDeliver(row)"
                      :disabled="actionLoading[row.fulfillmentOrderId]"
                      class="whitespace-nowrap px-3 py-1 rounded bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50 text-sm"
                    >
                      {{ actionLoading[row.fulfillmentOrderId] ? '处理中…' : '签收' }}
                    </button>
                  </template>
                  <template v-else>
                    <span class="text-gray-400 text-xs">—</span>
                  </template>
                  <span v-if="actionError[row.fulfillmentOrderId]" class="text-vmall-red text-xs">{{ actionError[row.fulfillmentOrderId] }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else-if="!loading && !error && list.length === 0" class="text-vmall-gray-text">暂无履约单或当前筛选无数据，请确认 BFF 与 fulfillment-service 已启动。</p>
      <p v-else-if="!loading && !error" class="text-vmall-gray-text">暂无履约单，或请确认 BFF 与 fulfillment-service 已启动。</p>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import AppHeader from '../shared/ui/AppHeader.vue'
import {
  listFulfillmentOrders,
  allocateFulfillmentOrder,
  completeEngravingFulfillmentOrder,
  shipFulfillmentOrder,
  deliverFulfillmentOrder,
} from '../shared/api/fulfillment.js'

const list = ref([])
const loading = ref(false)
const error = ref('')
const filterOrderId = ref('')
const filterStatus = ref('')
const filterType = ref('')
const actionLoading = reactive({})
const actionError = reactive({})
const shipForm = reactive({})

const STATUS_LABELS = {
  CREATED: '已创建',
  ALLOCATING: '配货中',
  SHIPPED: '已发货',
  DELIVERED: '已签收',
  ACTIVATED: '已激活',
  CANCELLED: '已取消',
}

const TYPE_LABELS = { PHYSICAL: '实体', VIRTUAL: '虚拟' }

function typeLabel(t) { return TYPE_LABELS[t] ?? t }

function typeClass(t) {
  return t === 'VIRTUAL' ? 'bg-blue-100 text-blue-800' : 'bg-gray-200 text-gray-700'
}

function statusLabel(s) {
  return STATUS_LABELS[s] ?? s
}

function statusClass(s) {
  const map = {
    CREATED: 'bg-gray-200 text-gray-700',
    ALLOCATING: 'bg-amber-100 text-amber-800',
    SHIPPED: 'bg-blue-100 text-blue-800',
    DELIVERED: 'bg-green-100 text-green-800',
    ACTIVATED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-gray-100 text-gray-500',
  }
  return map[s] ?? 'bg-gray-100 text-gray-700'
}

function itemsSummary(items) {
  if (!items?.length) return '—'
  const total = items.reduce((sum, i) => sum + (i.quantity ?? 0), 0)
  if (items.length === 1) {
    return `SKU ${items[0].skuId} × ${items[0].quantity}`
  }
  return `${items.length} 项共 ${total} 件`
}

function shortAddress(row) {
  const a = row.shippingAddress
  if (!a) return '—'
  return [a.province, a.city, a.district, a.detail].filter(Boolean).join(' ') || '—'
}

function fullAddress(row) {
  const a = row.shippingAddress
  if (!a) return ''
  return [a.recipientName, a.phone, a.province, a.city, a.district, a.detail].filter(Boolean).join(' ')
}

function formatTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short' })
}

function errorMessage(e) {
  const msg = e.response?.data?.message || e.message || ''
  if (e.response?.status === 404) return msg || '未找到'
  if (!e.response && (e.code === 'ERR_NETWORK' || e.message?.includes('Network'))) {
    return '无法连接后端，请确认 BFF（8085）与 fulfillment-service（8088）已启动'
  }
  return msg || '操作失败'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = {}
    const oid = filterOrderId.value?.trim()
    if (oid) {
      const n = Number(oid)
      if (!Number.isNaN(n)) params.orderId = n
    }
    if (filterStatus.value) params.status = filterStatus.value
    let rawList = await listFulfillmentOrders(params)
    if (filterType.value) {
      rawList = rawList.filter((r) => r.fulfillmentType === filterType.value)
    }
    list.value = rawList
    list.value.forEach((r) => {
      if (!shipForm[r.fulfillmentOrderId]) {
        shipForm[r.fulfillmentOrderId] = { carrier: '', trackingNumber: '' }
      }
    })
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

function hasEngravingPending(row) {
  return row.engravingInfo && !row.engravingCompletedAt
}

function engravingPreview(info) {
  if (!info) return ''
  const parts = []
  if (info.patternName) parts.push(`图案: ${info.patternName}`)
  if (info.text) parts.push(`文字: ${info.text}`)
  return parts.join(' | ') || '—'
}

function canShip(row) {
  if (hasEngravingPending(row)) return false
  const f = shipForm[row.fulfillmentOrderId]
  return f && f.carrier?.trim() && f.trackingNumber?.trim()
}

async function doCompleteEngraving(row) {
  const id = row.fulfillmentOrderId
  actionLoading[id] = true
  actionError[id] = ''
  try {
    await completeEngravingFulfillmentOrder(id)
    await load()
  } catch (e) {
    actionError[id] = errorMessage(e)
  } finally {
    actionLoading[id] = false
  }
}

async function doAllocate(row) {
  const id = row.fulfillmentOrderId
  actionLoading[id] = true
  actionError[id] = ''
  try {
    await allocateFulfillmentOrder(id)
    await load()
  } catch (e) {
    actionError[id] = errorMessage(e)
  } finally {
    actionLoading[id] = false
  }
}

async function doShip(row) {
  const id = row.fulfillmentOrderId
  const f = shipForm[id]
  if (!f?.carrier?.trim() || !f?.trackingNumber?.trim()) return
  actionLoading[id] = true
  actionError[id] = ''
  try {
    await shipFulfillmentOrder(id, { carrier: f.carrier.trim(), trackingNumber: f.trackingNumber.trim() })
    await load()
  } catch (e) {
    actionError[id] = errorMessage(e)
  } finally {
    actionLoading[id] = false
  }
}

async function doDeliver(row) {
  const id = row.fulfillmentOrderId
  actionLoading[id] = true
  actionError[id] = ''
  try {
    await deliverFulfillmentOrder(id)
    await load()
  } catch (e) {
    actionError[id] = errorMessage(e)
  } finally {
    actionLoading[id] = false
  }
}

onMounted(() => load())
</script>
