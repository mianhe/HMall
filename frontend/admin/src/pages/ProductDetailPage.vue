<template>
  <div>
    <AppHeader />
    <main class="max-w-4xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <router-link
          to="/catalog"
          class="inline-flex items-center text-vmall-gray-text hover:text-vmall-red"
        >
          ← 返回 Catalog
        </router-link>
        <button
          v-if="product"
          type="button"
          class="px-3 py-1.5 rounded-lg border border-red-200 text-red-600 text-sm hover:bg-red-50"
          @click="doDeleteProduct"
        >
          删除商品
        </button>
      </div>

      <div v-if="loading && !product" class="text-vmall-gray-text">加载中…</div>
      <div v-else-if="error" class="text-vmall-red mb-4">{{ error }}</div>
      <template v-else-if="product">
        <div class="flex items-center gap-3 mb-2">
          <template v-if="!editingBasic">
            <h1 class="text-2xl font-bold text-gray-800">{{ product.name }}</h1>
            <button
              type="button"
              class="px-2 py-0.5 text-xs rounded border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              @click="startEditBasic"
            >
              编辑
            </button>
          </template>
          <template v-else>
            <input
              v-model.trim="editName"
              type="text"
              class="text-2xl font-bold text-gray-800 border border-vmall-gray-border rounded px-2 py-1 w-full max-w-md"
              placeholder="商品名称"
            />
            <button
              type="button"
              class="px-2 py-1 text-sm rounded bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
              :disabled="savingBasic || !editName"
              @click="saveBasic"
            >
              {{ savingBasic ? '保存中…' : '保存' }}
            </button>
            <button
              type="button"
              class="px-2 py-1 text-sm rounded border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
              :disabled="savingBasic"
              @click="cancelEditBasic"
            >
              取消
            </button>
          </template>
          <span
            v-if="product.productType === 'SERVICE'"
            class="px-2 py-0.5 text-xs rounded bg-blue-50 text-blue-600 font-medium"
          >SERVICE</span>
          <span
            v-else
            class="px-2 py-0.5 text-xs rounded bg-gray-100 text-gray-500 font-medium"
          >PHYSICAL</span>
          <span
            v-if="product.serviceKind === 'ENGRAVING'"
            class="px-2 py-0.5 text-xs rounded bg-amber-50 text-amber-700 font-medium"
          >镭雕</span>
        </div>
        <div class="mb-6">
          <template v-if="!editingBasic">
            <p v-if="product.description" class="text-vmall-gray-text">{{ product.description }}</p>
            <p v-else class="text-vmall-gray-text">—</p>
          </template>
          <template v-else>
            <textarea
              v-model.trim="editDescription"
              rows="3"
              class="w-full max-w-md mt-1 px-2 py-1.5 border border-vmall-gray-border rounded text-vmall-gray-text"
              placeholder="描述（可选）"
            />
          </template>
        </div>

        <!-- 规格维度与选项 -->
        <section class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">规格维度与选项</h2>
          <div v-if="dimensions.length" class="space-y-4">
            <div
              v-for="dim in dimensions"
              :key="'d-' + dim.id"
              class="border border-vmall-gray-border rounded-lg p-4"
            >
              <div class="flex items-center gap-2 mb-2">
                <span class="font-medium text-gray-800">{{ dim.name }}</span>
                <span v-if="dim.required" class="px-1.5 py-0.5 text-xs rounded bg-blue-50 text-blue-600">必填</span>
              </div>
              <div v-if="dim.options?.length" class="space-y-3 ml-2">
                <div
                  v-for="opt in dim.options"
                  :key="'o-' + opt.id"
                  class="flex flex-wrap items-center gap-2"
                >
                  <span class="text-sm text-gray-700 py-1 shrink-0">{{ opt.optionValue }}</span>
                  <button
                    type="button"
                    class="px-1.5 py-0.5 text-xs rounded border border-red-200 text-red-600 hover:bg-red-50"
                    :disabled="deletingOptionId === opt.id"
                    @click="doDeleteOption(dim.id, opt.id)"
                  >
                    {{ deletingOptionId === opt.id ? '删除中…' : '删除' }}
                  </button>
                  <div v-if="opt.images?.length" class="flex flex-wrap gap-2">
                    <div
                      v-for="img in opt.images"
                      :key="'oi-' + img.id"
                      class="relative group"
                    >
                      <img
                        :src="img.imageUrl"
                        :alt="opt.optionValue"
                        class="w-16 h-16 object-cover rounded border border-gray-200"
                      />
                      <button
                        type="button"
                        class="absolute -top-1 -right-1 w-5 h-5 flex items-center justify-center bg-red-500 text-white text-xs rounded-full opacity-0 group-hover:opacity-100"
                        :disabled="deleting === 'o-' + opt.id + '-' + img.id"
                        @click="doDeleteOptionImage(dim.id, opt.id, img.id)"
                      >
                        ×
                      </button>
                    </div>
                  </div>
                  <input
                    :ref="(el) => setOptionFileInput(el, dim.id, opt.id)"
                    type="file"
                    accept="image/*"
                    class="hidden"
                    @change="(e) => onOptionFileChange(e, dim.id, opt.id)"
                  />
                  <button
                    type="button"
                    class="px-2 py-1 rounded text-sm border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
                    :disabled="uploading"
                    @click="clickOptionUpload(dim.id, opt.id)"
                  >
                    {{ uploading ? '上传中…' : '添加展示图' }}
                  </button>
                </div>
              </div>
              <p v-else class="text-sm text-gray-400 italic ml-2">暂无选项</p>
              <div class="mt-2 ml-2 flex items-center gap-2">
                <input
                  v-model="newOptionValues[dim.id]"
                  type="text"
                  class="w-40 px-2 py-1 border border-vmall-gray-border rounded text-sm"
                  placeholder="新选项值"
                  @keydown.enter.prevent="doCreateOption(dim.id)"
                />
                <button
                  type="button"
                  class="px-2 py-1 rounded text-sm bg-vmall-red text-white hover:bg-vmall-red-hover disabled:opacity-50"
                  :disabled="creatingOptionDimId === dim.id || !newOptionValues[dim.id]?.trim()"
                  @click="doCreateOption(dim.id)"
                >
                  {{ creatingOptionDimId === dim.id ? '添加中…' : '+ 新增选项' }}
                </button>
              </div>
            </div>
          </div>
          <div class="mt-3 flex items-center gap-2">
            <template v-if="!showNewDimensionForm">
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
                @click="showNewDimensionForm = true"
              >
                + 新增维度
              </button>
            </template>
            <template v-else>
              <input
                v-model.trim="newDimensionName"
                type="text"
                class="w-48 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                placeholder="维度名称（如：颜色）"
              />
              <label class="flex items-center gap-1 text-sm text-gray-700">
                <input v-model="newDimensionRequired" type="checkbox" class="rounded" />
                必填
              </label>
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg bg-vmall-red text-white text-sm hover:bg-vmall-red-hover disabled:opacity-50"
                :disabled="creatingDimension || !newDimensionName"
                @click="doCreateDimension"
              >
                {{ creatingDimension ? '创建中…' : '创建' }}
              </button>
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
                :disabled="creatingDimension"
                @click="showNewDimensionForm = false; newDimensionName = ''; newDimensionRequired = false"
              >
                取消
              </button>
            </template>
          </div>
          <p v-if="!dimensions.length" class="text-vmall-gray-text">暂无规格维度，点击「+ 新增维度」添加。</p>
        </section>

        <!-- SKU 列表 -->
        <section class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">SKU</h2>
          <div v-if="skus.length" class="space-y-2 mb-4">
            <div
              v-for="sku in skus"
              :key="'s-' + sku.id"
              class="flex flex-wrap items-center gap-3 py-2 px-3 border border-vmall-gray-border rounded-lg text-sm group"
            >
              <template v-if="editingSkuId === sku.id">
                <span class="text-gray-700 shrink-0">{{ skuDisplay(sku) }}</span>
                <div class="flex items-center gap-1">
                  <span class="text-vmall-gray-text">¥</span>
                  <input
                    v-model.number="skuEditPrice[sku.id]"
                    type="number"
                    min="0"
                    step="0.01"
                    class="w-24 px-2 py-1 border border-vmall-gray-border rounded"
                  />
                </div>
                <input
                  v-model="skuEditDisplayName[sku.id]"
                  type="text"
                  class="w-40 px-2 py-1 border border-vmall-gray-border rounded text-vmall-gray-text"
                  placeholder="展示名（可选）"
                />
                <button
                  type="button"
                  class="px-2 py-1 rounded bg-vmall-red text-white text-xs hover:bg-vmall-red-hover disabled:opacity-50"
                  :disabled="savingSkuId === sku.id"
                  @click="doUpdateSku(sku)"
                >
                  {{ savingSkuId === sku.id ? '保存中…' : '保存' }}
                </button>
                <button
                  type="button"
                  class="px-2 py-1 rounded border border-vmall-gray-border text-vmall-gray-text text-xs hover:bg-vmall-gray-bg"
                  :disabled="savingSkuId === sku.id"
                  @click="editingSkuId = null"
                >
                  取消
                </button>
                <button
                  type="button"
                  class="px-2 py-1 rounded border border-red-200 text-red-600 text-xs hover:bg-red-50 disabled:opacity-50"
                  :disabled="deletingSkuId === sku.id"
                  @click="doDeleteSku(sku.id)"
                >
                  {{ deletingSkuId === sku.id ? '删除中…' : '删除' }}
                </button>
              </template>
              <template v-else>
                <span class="text-gray-700 shrink-0">{{ skuDisplay(sku) }}</span>
                <span class="text-vmall-gray-text">{{ (skuEditPrice[sku.id] != null ? skuEditPrice[sku.id] : sku.priceCents / 100).toFixed(2) }} 元</span>
                <span v-if="skuEditDisplayName[sku.id]" class="text-vmall-gray-text">{{ skuEditDisplayName[sku.id] }}</span>
                <div class="ml-auto flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    type="button"
                    class="px-2 py-0.5 text-xs rounded border border-vmall-gray-border text-vmall-gray-text hover:bg-vmall-gray-bg"
                    @click="editingSkuId = sku.id"
                  >
                    编辑
                  </button>
                  <button
                    type="button"
                    class="px-2 py-0.5 text-xs rounded border border-red-200 text-red-600 hover:bg-red-50 disabled:opacity-50"
                    :disabled="deletingSkuId === sku.id"
                    @click="doDeleteSku(sku.id)"
                  >
                    {{ deletingSkuId === sku.id ? '删除中…' : '删除' }}
                  </button>
                </div>
              </template>
            </div>
          </div>
          <div v-if="showNewSkuForm" class="border border-vmall-gray-border rounded-lg p-4 mb-3 space-y-3">
            <div v-for="dim in dimensions" :key="'ns-d-' + dim.id" class="flex items-center gap-2">
              <span class="text-sm text-gray-700 w-24 shrink-0">{{ dim.name }}</span>
              <select
                v-model="newSkuOptionIds[dim.id]"
                class="flex-1 max-w-xs px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
              >
                <option :value="null">请选择</option>
                <option
                  v-for="opt in dim.options"
                  :key="opt.id"
                  :value="opt.id"
                >
                  {{ opt.optionValue }}
                </option>
              </select>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-sm text-gray-700 w-24 shrink-0">价格（元）</span>
              <input
                v-model="newSkuPriceYuan"
                type="number"
                min="0"
                step="0.01"
                class="w-28 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
              />
            </div>
            <div class="flex items-center gap-2">
              <span class="text-sm text-gray-700 w-24 shrink-0">展示名</span>
              <input
                v-model="newSkuDisplayName"
                type="text"
                class="flex-1 max-w-xs px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                placeholder="可选"
              />
            </div>
            <div class="flex gap-2">
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg bg-vmall-red text-white text-sm hover:bg-vmall-red-hover disabled:opacity-50"
                :disabled="creatingSku || !canCreateSku"
                @click="doCreateSku"
              >
                {{ creatingSku ? '创建中…' : '创建' }}
              </button>
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
                :disabled="creatingSku"
                @click="closeNewSkuForm"
              >
                取消
              </button>
            </div>
          </div>
          <button
            v-if="!showNewSkuForm"
            type="button"
            class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
            @click="openNewSkuForm"
          >
            + 新增 SKU
          </button>
          <p v-if="!skus.length && !showNewSkuForm" class="text-vmall-gray-text text-sm">暂无 SKU，请先添加规格维度与选项，再添加 SKU。</p>
        </section>

        <!-- 产品级展示图 -->
        <section class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">产品级展示图</h2>
          <div v-if="productImages.length" class="flex flex-wrap gap-3 mb-3">
            <div
              v-for="img in productImages"
              :key="'pi-' + img.id"
              class="relative group"
            >
              <img
                :src="img.imageUrl"
                alt="产品图"
                class="w-24 h-24 object-cover rounded border border-gray-200"
              />
              <button
                type="button"
                class="absolute top-0 right-0 w-5 h-5 flex items-center justify-center bg-red-500 text-white text-xs rounded-full opacity-0 group-hover:opacity-100"
                :disabled="deleting === img.id"
                @click="doDeleteProductImage(img.id)"
              >
                ×
              </button>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <input
              ref="productFileInputRef"
              type="file"
              accept="image/*"
              class="hidden"
              @change="onProductFileChange"
            />
            <button
              type="button"
              class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
              :disabled="uploading"
              @click="productFileInputRef?.click()"
            >
              {{ uploading ? '上传中…' : '上传产品级展示图' }}
            </button>
          </div>
        </section>

        <!-- 可选服务（仅 PHYSICAL 类型商品） -->
        <section v-if="product.productType === 'PHYSICAL'" class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">可选服务</h2>
          <!-- 已绑定列表 -->
          <div v-if="availableServices.length" class="space-y-4 mb-4">
            <div
              v-for="svc in availableServices"
              :key="'as-' + svc.serviceSpuId"
              class="border border-vmall-gray-border rounded-lg p-4"
            >
              <h3 class="text-sm font-medium text-gray-700 mb-2">{{ svc.name }}</h3>
              <div class="space-y-2">
                <div
                  v-for="b in svc.bindings"
                  :key="'b-' + b.bindingId"
                  class="flex items-center justify-between py-2 px-3 bg-gray-50 rounded text-sm"
                >
                  <span class="text-vmall-gray-text">{{ bindingSpecDisplay(b) }}</span>
                  <span class="text-gray-700">¥{{ (b.priceCents / 100).toFixed(2) }}</span>
                  <button
                    type="button"
                    class="text-red-500 hover:text-red-700 text-xs"
                    :disabled="deletingPhysicalBindingId === b.bindingId"
                    @click="doDeletePhysicalBinding(b.serviceSkuId, b.bindingId)"
                  >
                    {{ deletingPhysicalBindingId === b.bindingId ? '删除中…' : '删除' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
          <p v-else class="text-sm text-gray-400 italic mb-4">暂无已绑定的可选服务。</p>
          <!-- + 绑定服务 -->
          <div class="border border-vmall-gray-border rounded-lg p-4 space-y-3">
            <div class="flex items-center gap-2 flex-wrap">
              <input
                v-model.trim="serviceSearchKeyword"
                type="text"
                class="w-48 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                placeholder="搜索服务名称"
                @keyup.enter="doSearchServices"
              />
              <button
                type="button"
                class="px-3 py-1.5 rounded-lg bg-vmall-red text-white text-sm hover:bg-vmall-red-hover"
                @click="doSearchServices"
              >
                搜索
              </button>
            </div>
            <div v-if="serviceSearchResults.length" class="space-y-1">
              <p class="text-xs text-vmall-gray-text mb-1">选择要绑定的服务：</p>
              <button
                v-for="p in serviceSearchResults"
                :key="p.id"
                type="button"
                class="block w-full text-left px-3 py-2 rounded border text-sm"
                :class="selectedServiceForBinding?.id === p.id ? 'border-vmall-red bg-red-50' : 'border-vmall-gray-border hover:bg-gray-50'"
                @click="selectServiceForBinding(p)"
              >
                {{ p.name }} <span class="text-vmall-gray-text">#{{ p.id }}</span>
              </button>
            </div>
            <div v-if="selectedServiceForBinding && serviceSkusForBinding.length" class="space-y-2 pt-2 border-t border-vmall-gray-border">
              <p class="text-xs text-vmall-gray-text">选择 SKU：</p>
              <select
                v-model="newBindingSkuId"
                class="w-full max-w-md px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
              >
                <option :value="null">请选择</option>
                <option
                  v-for="sku in serviceSkusForBinding"
                  :key="sku.id"
                  :value="sku.id"
                >
                  {{ skuDisplay(sku) }} — ¥{{ (sku.priceCents / 100).toFixed(2) }}
                </option>
              </select>
              <div class="flex items-center gap-2">
                <label class="text-sm text-vmall-gray-text">绑定价（元，留空继承）：</label>
                <input
                  v-model="newBindingPriceYuan"
                  type="text"
                  class="w-24 px-2 py-1.5 border border-vmall-gray-border rounded text-sm"
                  placeholder="可选"
                />
              </div>
              <div class="flex gap-2">
                <button
                  type="button"
                  class="px-3 py-1.5 rounded-lg bg-vmall-red text-white text-sm hover:bg-vmall-red-hover disabled:opacity-50"
                  :disabled="creatingPhysicalBinding || !newBindingSkuId"
                  @click="doCreatePhysicalBinding"
                >
                  {{ creatingPhysicalBinding ? '添加中…' : '添加绑定' }}
                </button>
                <button
                  type="button"
                  class="px-3 py-1.5 rounded-lg border border-vmall-gray-border text-sm text-vmall-gray-text hover:bg-vmall-gray-bg"
                  :disabled="creatingPhysicalBinding"
                  @click="clearServiceBindingForm"
                >
                  取消
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- 已绑定的实体商品（仅 SERVICE 类型商品，仅展示；绑定/解绑在实体商品详情页「可选服务」操作） -->
        <section v-if="product.productType === 'SERVICE' && skus.length" class="mb-8">
          <h2 class="text-lg font-semibold text-gray-800 mb-3">已绑定的实体商品</h2>
          <div v-if="bindingTableRows.length" class="overflow-x-auto border border-vmall-gray-border rounded-lg">
            <table class="w-full text-sm">
              <thead>
                <tr class="bg-gray-50 border-b border-vmall-gray-border">
                  <th class="text-left py-2 px-3 font-medium text-gray-700">服务 SKU</th>
                  <th class="text-left py-2 px-3 font-medium text-gray-700">实体商品</th>
                  <th class="text-left py-2 px-3 font-medium text-gray-700">绑定价</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in bindingTableRows"
                  :key="row.key"
                  class="border-b border-gray-100 last:border-b-0 hover:bg-gray-50/50"
                >
                  <td class="py-2 px-3 text-gray-700">{{ row.skuLabel }}</td>
                  <td class="py-2 px-3 text-gray-700">{{ row.targetName }}</td>
                  <td class="py-2 px-3 text-vmall-gray-text">{{ row.priceText }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="text-sm text-gray-400 italic">暂无绑定（可独立售卖）。需绑定时请到对应实体商品详情页「可选服务」中操作。</p>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '../shared/ui/AppHeader.vue'
import {
  getProduct,
  getDimensions,
  getProductImages,
  getSkus,
  getServiceBindings,
  getAvailableServices,
  searchProducts,
  createServiceBinding,
  deleteServiceBinding,
  uploadFile,
  addProductImage,
  addOptionImage,
  deleteProductImage,
  deleteOptionImage,
  updateProduct,
  deleteProduct as apiDeleteProduct,
  createDimension,
  createOption,
  deleteOption as apiDeleteOption,
  createSku,
  updateSku,
  deleteSku as apiDeleteSku,
} from '../shared/api/catalog.js'

const route = useRoute()
const router = useRouter()
const toast = inject('toast')
const confirm = inject('confirm')
const product = ref(null)
const editingBasic = ref(false)
const editName = ref('')
const editDescription = ref('')
const savingBasic = ref(false)
const dimensions = ref([])
const productImages = ref([])
const skus = ref([])
const loading = ref(true)
const error = ref('')
const uploading = ref(false)
const deleting = ref(null)
const productFileInputRef = ref(null)
const optionFileInputs = ref({}) // key: `${dimensionId}-${optionId}`
const bindingsMap = ref({})     // key: skuId → ServiceBinding[]（仅 SERVICE 展示用）
const showNewDimensionForm = ref(false)
const newDimensionName = ref('')
const newDimensionRequired = ref(false)
const creatingDimension = ref(false)
const newOptionValues = ref({})
const creatingOptionDimId = ref(null)
const deletingOptionId = ref(null)
const skuEditPrice = ref({})
const skuEditDisplayName = ref({})
const savingSkuId = ref(null)
const deletingSkuId = ref(null)
const editingSkuId = ref(null)
const showNewSkuForm = ref(false)
const newSkuOptionIds = ref({})
const newSkuPriceYuan = ref('')
const newSkuDisplayName = ref('')
const creatingSku = ref(false)
// PHYSICAL 可选服务
const availableServices = ref([])
const serviceSearchKeyword = ref('')
const serviceSearchResults = ref([])
const selectedServiceForBinding = ref(null)
const serviceSkusForBinding = ref([])
const newBindingSkuId = ref(null)
const newBindingPriceYuan = ref('')
const creatingPhysicalBinding = ref(false)
const deletingPhysicalBindingId = ref(null)

const spuId = computed(() => {
  const id = route.params.id
  return id != null && id !== '' ? Number(id) : null
})

const canCreateSku = computed(() => {
  const dims = dimensions.value
  if (dims.length > 0) {
    const allSelected = dims.every((d) => newSkuOptionIds.value[d.id])
    if (!allSelected) return false
  }
  const price = Number(newSkuPriceYuan.value)
  return newSkuPriceYuan.value !== '' && !Number.isNaN(price) && price >= 0
})

// SERVICE 详情页：已绑定实体商品表格行（扁平化 skus + bindingsMap）
const bindingTableRows = computed(() => {
  const rows = []
  const skuList = skus.value
  const map = bindingsMap.value
  skuList.forEach((sku) => {
    const bindings = map[sku.id] || []
    const skuLabel = `${skuDisplay(sku)}（标准价 ¥${(sku.priceCents / 100).toFixed(2)}）`
    bindings.forEach((b) => {
      rows.push({
        key: 'b-' + b.id,
        skuLabel,
        targetName: b.targetSpuName || `SPU#${b.targetSpuId}`,
        priceText: b.priceCents != null ? `¥${(b.priceCents / 100).toFixed(2)}` : '继承标准价',
      })
    })
  })
  return rows
})

function setOptionFileInput(el, dimensionId, optionId) {
  if (el) optionFileInputs.value[`${dimensionId}-${optionId}`] = el
}

function clickOptionUpload(dimensionId, optionId) {
  const el = optionFileInputs.value[`${dimensionId}-${optionId}`]
  if (el) el.click()
}

async function load() {
  if (!spuId.value) return
  loading.value = true
  error.value = ''
  try {
    const [p, dims, imgs, s] = await Promise.all([
      getProduct(spuId.value),
      getDimensions(spuId.value),
      getProductImages(spuId.value),
      getSkus(spuId.value),
    ])
    product.value = p
    dimensions.value = dims || []
    productImages.value = imgs || []
    skus.value = s || []
    const priceMap = {}
    const displayMap = {}
    ;(s || []).forEach((sku) => {
      priceMap[sku.id] = sku.priceCents / 100
      displayMap[sku.id] = sku.displayName ?? ''
    })
    skuEditPrice.value = priceMap
    skuEditDisplayName.value = displayMap
    if (p.productType === 'SERVICE') {
      await loadAllBindings(s || [])
    }
    if (p.productType === 'PHYSICAL') {
      availableServices.value = await getAvailableServices(spuId.value)
    } else {
      availableServices.value = []
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function onProductFileChange(e) {
  const file = e.target.files?.[0]
  if (!file || !spuId.value) return
  e.target.value = ''
  uploading.value = true
  error.value = ''
  try {
    const { url } = await uploadFile(file)
    await addProductImage(spuId.value, { imageUrl: url })
    productImages.value = await getProductImages(spuId.value)
    toast.showToast('产品图上传成功', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

async function onOptionFileChange(e, dimensionId, optionId) {
  const file = e.target.files?.[0]
  if (!file || !spuId.value) return
  e.target.value = ''
  uploading.value = true
  error.value = ''
  try {
    const { url } = await uploadFile(file)
    await addOptionImage(spuId.value, dimensionId, optionId, { imageUrl: url })
    dimensions.value = await getDimensions(spuId.value)
    toast.showToast('选项图上传成功', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

async function doDeleteProductImage(imageId) {
  if (!spuId.value) return
  deleting.value = imageId
  error.value = ''
  try {
    await deleteProductImage(spuId.value, imageId)
    productImages.value = await getProductImages(spuId.value)
    toast.showToast('已删除', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '删除失败', 'error')
  } finally {
    deleting.value = null
  }
}

async function doDeleteOptionImage(dimensionId, optionId, imageId) {
  if (!spuId.value) return
  deleting.value = 'o-' + optionId + '-' + imageId
  error.value = ''
  try {
    await deleteOptionImage(spuId.value, dimensionId, optionId, imageId)
    dimensions.value = await getDimensions(spuId.value)
    toast.showToast('已删除', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '删除失败', 'error')
  } finally {
    deleting.value = null
  }
}

// 并发限制 5，待后端提供 GET /api/service-bindings/batch?skuIds=... 后可改为单次请求
const BINDINGS_CONCURRENCY = 5
async function loadAllBindings(skuList) {
  const map = {}
  for (let i = 0; i < skuList.length; i += BINDINGS_CONCURRENCY) {
    const chunk = skuList.slice(i, i + BINDINGS_CONCURRENCY)
    const results = await Promise.all(
      chunk.map(async (sku) => {
        const bindings = await getServiceBindings(sku.id)
        return [sku.id, bindings]
      })
    )
    results.forEach(([id, bindings]) => { map[id] = bindings })
  }
  bindingsMap.value = map
}

function bindingSpecDisplay(binding) {
  if (binding.specValues?.length) {
    return binding.specValues.map((v) => v.optionValue).join(' · ')
  }
  return 'SKU#' + binding.serviceSkuId
}

async function doSearchServices() {
  error.value = ''
  try {
    const list = await searchProducts(serviceSearchKeyword.value)
    serviceSearchResults.value = (list || []).filter((p) => p.productType === 'SERVICE')
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '搜索失败'
    serviceSearchResults.value = []
  }
}

async function selectServiceForBinding(p) {
  selectedServiceForBinding.value = p
  newBindingSkuId.value = null
  newBindingPriceYuan.value = ''
  error.value = ''
  try {
    serviceSkusForBinding.value = await getSkus(p.id)
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '加载 SKU 失败'
    serviceSkusForBinding.value = []
  }
}

function clearServiceBindingForm() {
  selectedServiceForBinding.value = null
  serviceSkusForBinding.value = []
  newBindingSkuId.value = null
  newBindingPriceYuan.value = ''
}

async function doCreatePhysicalBinding() {
  if (!spuId.value || !newBindingSkuId.value) return
  creatingPhysicalBinding.value = true
  error.value = ''
  try {
    const priceCents =
      newBindingPriceYuan.value !== '' && newBindingPriceYuan.value != null
        ? Math.round(Number(newBindingPriceYuan.value) * 100)
        : null
    if (priceCents !== null && (Number.isNaN(priceCents) || priceCents < 0)) {
      toast.showToast('绑定价无效', 'error')
      return
    }
    await createServiceBinding(newBindingSkuId.value, {
      targetSpuId: spuId.value,
      priceCents: priceCents ?? undefined,
    })
    availableServices.value = await getAvailableServices(spuId.value)
    clearServiceBindingForm()
    toast.showToast('服务绑定成功', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '添加绑定失败', 'error')
  } finally {
    creatingPhysicalBinding.value = false
  }
}

async function doDeletePhysicalBinding(serviceSkuId, bindingId) {
  deletingPhysicalBindingId.value = bindingId
  error.value = ''
  try {
    await deleteServiceBinding(serviceSkuId, bindingId)
    availableServices.value = await getAvailableServices(spuId.value)
    toast.showToast('绑定已解除', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '删除绑定失败', 'error')
  } finally {
    deletingPhysicalBindingId.value = null
  }
}

async function doCreateDimension() {
  if (!spuId.value || !newDimensionName.value.trim()) return
  creatingDimension.value = true
  error.value = ''
  try {
    await createDimension(spuId.value, {
      name: newDimensionName.value.trim(),
      required: newDimensionRequired.value,
    })
    dimensions.value = await getDimensions(spuId.value)
    showNewDimensionForm.value = false
    newDimensionName.value = ''
    newDimensionRequired.value = false
    toast.showToast('维度创建成功', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '创建维度失败', 'error')
  } finally {
    creatingDimension.value = false
  }
}

async function doCreateOption(dimensionId) {
  const val = newOptionValues.value[dimensionId]?.trim()
  if (!spuId.value || !val) return
  creatingOptionDimId.value = dimensionId
  error.value = ''
  try {
    await createOption(spuId.value, dimensionId, { optionValue: val })
    dimensions.value = await getDimensions(spuId.value)
    newOptionValues.value[dimensionId] = ''
    toast.showToast('选项添加成功', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '创建选项失败', 'error')
  } finally {
    creatingOptionDimId.value = null
  }
}

async function doDeleteOption(dimensionId, optionId) {
  if (!spuId.value) return
  if (!(await confirm.confirm({ title: '删除选项', message: '删除该选项可能影响已有 SKU，确定删除？' }))) return
  deletingOptionId.value = optionId
  try {
    await apiDeleteOption(spuId.value, dimensionId, optionId)
    dimensions.value = await getDimensions(spuId.value)
    toast.showToast('选项已删除', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '删除选项失败', 'error')
  } finally {
    deletingOptionId.value = null
  }
}

function skuDisplay(sku) {
  if (sku.displayName) return sku.displayName
  if (sku.specValues?.length) return sku.specValues.map((v) => v.optionValue).join(' · ')
  return '#' + sku.id
}

function startEditBasic() {
  if (!product.value) return
  editName.value = product.value.name ?? ''
  editDescription.value = product.value.description ?? ''
  editingBasic.value = true
}

function cancelEditBasic() {
  editingBasic.value = false
}

async function saveBasic() {
  if (!spuId.value || !editName.value.trim()) return
  savingBasic.value = true
  error.value = ''
  try {
    await updateProduct(spuId.value, {
      name: editName.value.trim(),
      description: editDescription.value || undefined,
    })
    product.value = await getProduct(spuId.value)
    editingBasic.value = false
    toast.showToast('保存成功', 'success')
  } catch (e) {
    toast.showToast(e.response?.data?.message || e.message || '保存失败', 'error')
  } finally {
    savingBasic.value = false
  }
}

async function doDeleteProduct() {
  if (!spuId.value) return
  if (!(await confirm.confirm({ title: '删除商品', message: '确定删除该商品？此操作不可恢复。' }))) return
  try {
    await apiDeleteProduct(spuId.value)
    toast.showToast('商品已删除', 'success')
    router.push('/catalog')
  } catch (e) {
    toast.showToast(e.response?.data?.message || e.message || '删除失败', 'error')
  }
}

function openNewSkuForm() {
  showNewSkuForm.value = true
  newSkuOptionIds.value = {}
  newSkuPriceYuan.value = ''
  newSkuDisplayName.value = ''
}

function closeNewSkuForm() {
  showNewSkuForm.value = false
}

async function doCreateSku() {
  if (!spuId.value || !canCreateSku.value) return
  creatingSku.value = true
  error.value = ''
  try {
    const dims = dimensions.value
    const specOptionIds = dims.length > 0
      ? dims.map((d) => newSkuOptionIds.value[d.id]).filter(Boolean)
      : []
    const priceCents = Math.round(Number(newSkuPriceYuan.value) * 100)
    await createSku(spuId.value, {
      specOptionIds,
      priceCents,
      displayName: newSkuDisplayName.value?.trim() || undefined,
    })
    skus.value = await getSkus(spuId.value)
    const priceMap = {}
    const displayMap = {}
    skus.value.forEach((sku) => {
      priceMap[sku.id] = sku.priceCents / 100
      displayMap[sku.id] = sku.displayName ?? ''
    })
    skuEditPrice.value = priceMap
    skuEditDisplayName.value = displayMap
    if (product.value?.productType === 'SERVICE') await loadAllBindings(skus.value)
    closeNewSkuForm()
    toast.showToast('SKU 创建成功', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '创建 SKU 失败', 'error')
  } finally {
    creatingSku.value = false
  }
}

async function doUpdateSku(sku) {
  if (!spuId.value) return
  const priceCents = Math.round(Number(skuEditPrice.value[sku.id]) * 100)
  if (Number.isNaN(priceCents) || priceCents < 0) return
  savingSkuId.value = sku.id
  error.value = ''
  try {
    await updateSku(spuId.value, sku.id, {
      priceCents,
      displayName: skuEditDisplayName.value[sku.id]?.trim() || undefined,
    })
    skus.value = await getSkus(spuId.value)
    skuEditPrice.value[sku.id] = priceCents / 100
    skuEditDisplayName.value[sku.id] = skuEditDisplayName.value[sku.id]?.trim() ?? ''
    editingSkuId.value = null
    toast.showToast('SKU 保存成功', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '保存 SKU 失败', 'error')
  } finally {
    savingSkuId.value = null
  }
}

async function doDeleteSku(skuId) {
  if (!spuId.value) return
  if (!(await confirm.confirm({ title: '删除 SKU', message: '确定删除该 SKU？' }))) return
  deletingSkuId.value = skuId
  try {
    await apiDeleteSku(spuId.value, skuId)
    skus.value = await getSkus(spuId.value)
    const priceMap = {}
    const displayMap = {}
    skus.value.forEach((sku) => {
      priceMap[sku.id] = sku.priceCents / 100
      displayMap[sku.id] = sku.displayName ?? ''
    })
    skuEditPrice.value = priceMap
    skuEditDisplayName.value = displayMap
    if (product.value?.productType === 'SERVICE') await loadAllBindings(skus.value)
    toast.showToast('SKU 已删除', 'success')
  } catch (err) {
    toast.showToast(err.response?.data?.message || err.message || '删除 SKU 失败', 'error')
  } finally {
    deletingSkuId.value = null
  }
}

watch(spuId, load, { immediate: true })
</script>
