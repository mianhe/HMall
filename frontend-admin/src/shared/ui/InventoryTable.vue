<template>
  <div class="bg-white rounded-lg border border-vmall-gray-border overflow-hidden">
    <table class="w-full text-sm table-fixed">
      <colgroup>
        <col style="width: 10%">
        <col style="width: 10%">
        <col style="width: 15%">
        <col>
        <col style="width: 6rem">
        <col style="width: 4rem">
        <col style="width: 5rem">
      </colgroup>
      <thead class="bg-gray-100 border-b border-vmall-gray-border">
        <tr>
          <th class="text-left py-3 px-4 font-medium text-gray-700">一级类别</th>
          <th class="text-left py-3 px-4 font-medium text-gray-700">二级子类别</th>
          <th class="text-left py-3 px-4 font-medium text-gray-700">产品</th>
          <th class="text-left py-3 px-4 font-medium text-gray-700">SKU 名称</th>
          <th class="text-left py-3 px-4 font-medium text-gray-700">可用</th>
          <th class="text-left py-3 px-4 font-medium text-gray-700">已占用</th>
          <th class="text-left py-3 px-4 font-medium text-gray-700">操作</th>
        </tr>
      </thead>
      <tbody>
      <template v-for="(row, i) in rows" :key="'s-' + row.sku.id">
        <tr
          class="border-b border-vmall-gray-border last:border-b-0 hover:bg-vmall-gray-bg"
        >
          <td v-if="rowspanRoot[i]" :rowspan="rowspanRoot[i]" class="py-2 px-4 text-gray-800 align-top border-r border-vmall-gray-border/50 break-words">{{ row.rootName }}</td>
          <td v-if="rowspanSub[i]" :rowspan="rowspanSub[i]" class="py-2 px-4 text-gray-800 align-top border-r border-vmall-gray-border/50 break-words">{{ row.subName }}</td>
          <td v-if="rowspanProduct[i]" :rowspan="rowspanProduct[i]" class="py-2 px-4 text-gray-800 align-top border-r border-vmall-gray-border/50 break-words">{{ row.product.name }}</td>
          <td class="py-2 px-4 text-gray-700 break-words">{{ row.skuDisplayName }}</td>
          <td class="py-2 px-4">
            <template v-if="stockBySkuId[row.sku.id] === undefined">
              <span class="text-gray-400">加载中…</span>
            </template>
            <template v-else>
              <input
                v-model.number="editAvailable[row.sku.id]"
                type="number"
                min="0"
                class="w-20 px-2 py-1 rounded border border-vmall-gray-border text-gray-800"
              />
            </template>
          </td>
          <td class="py-2 px-4 text-gray-700">
            {{ stockBySkuId[row.sku.id]?.reserved ?? '—' }}
          </td>
          <td class="py-2 px-4">
            <div class="flex flex-col gap-1">
              <button
                v-if="stockBySkuId[row.sku.id] !== undefined"
                @click="saveStock(row.sku.id)"
                :disabled="saveLoading[row.sku.id]"
                class="min-w-[52px] whitespace-nowrap px-3 py-1 rounded bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50 text-sm self-start"
              >
                {{ saveLoading[row.sku.id] ? '保存中…' : '保存' }}
              </button>
              <span v-if="saveErrorBySku[row.sku.id]" class="text-vmall-red text-xs">{{ saveErrorBySku[row.sku.id] }}</span>
            </div>
          </td>
        </tr>
      </template>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  stockBySkuId: { type: Object, default: () => ({}) },
  onSaveStock: { type: Function, default: () => {} },
})

const editAvailable = reactive({})
const saveLoading = reactive({})
const saveErrorBySku = reactive({})

/** 一级类别：连续相同 rootId 合并，仅首行显示并设 rowspan */
const rowspanRoot = computed(() => {
  const list = props.rows || []
  const out = []
  for (let i = 0; i < list.length; i++) {
    const same = list[i].rootId != null && (i === 0 || String(list[i].rootId) !== String(list[i - 1].rootId))
    if (!same) {
      out[i] = 0
      continue
    }
    let n = 1
    while (i + n < list.length && String(list[i + n].rootId) === String(list[i].rootId)) n++
    out[i] = n
  }
  return out
})

/** 二级子类别：同一一级下连续相同 subId 合并 */
const rowspanSub = computed(() => {
  const list = props.rows || []
  const out = []
  for (let i = 0; i < list.length; i++) {
    const prev = i > 0 ? list[i - 1] : null
    const sameRoot = prev && String(list[i].rootId) === String(prev.rootId)
    const sameSub = prev && String(list[i].subId) === String(prev.subId)
    const same = !prev || !sameRoot || !sameSub
    if (!same) {
      out[i] = 0
      continue
    }
    let n = 1
    while (i + n < list.length) {
      const next = list[i + n]
      if (String(next.rootId) !== String(list[i].rootId)) break
      if (String(next.subId) !== String(list[i].subId)) break
      n++
    }
    out[i] = n
  }
  return out
})

/** 产品：同一二级下连续相同 product.id 合并 */
const rowspanProduct = computed(() => {
  const list = props.rows || []
  const out = []
  for (let i = 0; i < list.length; i++) {
    const prev = i > 0 ? list[i - 1] : null
    const sameRoot = prev && String(list[i].rootId) === String(prev.rootId)
    const sameSub = prev && String(list[i].subId) === String(prev.subId)
    const sameProduct = prev && String(list[i].product?.id) === String(prev.product?.id)
    const same = !prev || !sameRoot || !sameSub || !sameProduct
    if (!same) {
      out[i] = 0
      continue
    }
    let n = 1
    while (i + n < list.length) {
      const next = list[i + n]
      if (String(next.rootId) !== String(list[i].rootId)) break
      if (String(next.subId) !== String(list[i].subId)) break
      if (String(next.product?.id) !== String(list[i].product?.id)) break
      n++
    }
    out[i] = n
  }
  return out
})

watch(
  () => [props.rows, props.stockBySkuId],
  () => {
    for (const row of props.rows || []) {
      const s = props.stockBySkuId[row.sku?.id]
      if (s && editAvailable[row.sku.id] === undefined && s.available != null) {
        editAvailable[row.sku.id] = s.available
      }
    }
  },
  { deep: true }
)

async function saveStock(skuId) {
  const val = editAvailable[skuId]
  if (val == null || val < 0) return
  saveLoading[skuId] = true
  saveErrorBySku[skuId] = ''
  try {
    await props.onSaveStock(skuId, val)
    saveErrorBySku[skuId] = ''
  } catch (msg) {
    saveErrorBySku[skuId] = msg
  } finally {
    saveLoading[skuId] = false
  }
}
</script>
