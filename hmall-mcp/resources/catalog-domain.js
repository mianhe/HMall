/**
 * Catalog 领域知识 Resource — 供 MCP Client 注入对话上下文。
 * 包含商品模型、服务绑定、定价规则等跨工具的业务知识。
 */

export const CATALOG_DOMAIN_URI = 'hmall://catalog/domain-knowledge'

export const CATALOG_DOMAIN_KNOWLEDGE = `## 商品目录（Catalog）领域知识

### 数据模型

- 类目为两级结构：一级类目（如「手机」「平板」）→ 二级类目（如「Mate 系列」「Nova 系列」）。商品只能挂在二级类目（叶子类目）下。
- 查找商品路径：先用 catalog_categories tree 获取完整类目树 → 定位一级类目 → 找到目标二级类目 → 用 categoryId 列出该类目下的商品。
- 商品(SPU) 分两种类型：PHYSICAL（实体商品，默认）和 SERVICE（服务类/虚拟商品）。创建商品时通过 productType 参数指定。
- 商品(SPU) 有多个规格维度（如颜色、版本），每个维度有多个选项，组合出 SKU。
- SKU = 选项组合 + 价格（单位分，如 599900 = ¥5999）+ 可选展示名。SKU.priceCents 始终必填，是标准价/参考价。

### 虚拟商品与服务绑定（ServiceBinding）

- SERVICE 类型商品的 SKU 可通过 ServiceBinding 绑定到 PHYSICAL 类型的 SPU，表示"该服务适用于该实体商品"。
- SKU.priceCents 是标准价，始终必填；ServiceBinding.priceCents 是可选的覆盖价格。
- 三种定价模式：
  1. 无 binding → 服务独立售卖，售价 = SKU.priceCents（如"上门回收服务 ¥99"）
  2. binding + priceCents 为空(null) → 限定适用范围，继承 SKU 标准价（如"镭雕服务 ¥199"对所有支持的商品统一价）
  3. binding + priceCents 非空 → 上下文定价，覆盖 SKU 标准价（如"Care+ 一年期"对 Mate 80 = ¥299，对 Pura 70 = ¥259）
- 最终售价 = binding.priceCents ?? sku.priceCents
- ⚠️ 修改绑定价格：要改某服务在某实体商品上的售价，必须用 catalog_service_bindings update(skuId, bindingId, priceCents)，不要去改 SKU.priceCents（那是标准价，改了影响所有 binding 继承的基础价格）。
- 用户说"价格为空"时，说明可能想设置服务绑定时不指定价格（继承标准价）。应引导：SKU 本身必须有标准价，binding 的价格才是可选的。

### 服务绑定操作指南

- 查看某实体商品的全部可选服务 → 用 catalog_available_services(spuId)，返回按服务 SPU 分组的列表，每项含 bindingId、serviceSkuId 和最终售价。
- 修改某服务在某实体商品上的价格 → 先用 catalog_available_services 查到 bindingId 和 serviceSkuId → 再用 catalog_service_bindings update(skuId=serviceSkuId, bindingId=bindingId, priceCents=新价格)。
- 查看某服务 SKU 绑定了哪些实体商品 → 用 catalog_service_bindings list(skuId=serviceSkuId)。
- 创建新绑定 → catalog_service_bindings create(skuId, targetSpuId, priceCents 可选)。
- 删除绑定 → catalog_service_bindings delete(skuId, bindingId)。

### 查询商品与 SKU

- 按 SKU 属性筛选（如查价格区间、规格组合）：catalog_products list + includeSkus=true，一次返回所有商品及其 SKU（含价格、specValues）。
- 查单个商品完整信息：catalog_products get detail=full。

### 新建商品流程

- 新建实体商品：确认类目 → catalog_products create（productType 不传或传 PHYSICAL）→ 添加维度与选项 → 用 optionId 创建 SKU。
- 新建服务类商品：确认类目 → catalog_products create productType=SERVICE → 添加维度与选项 → 创建 SKU（priceCents 为标准价，必填）→ 用 catalog_service_bindings 绑定到实体商品。

### 展示图

- 产品级图片不绑规格；选项级图片绑到具体选项（如颜色:黑色的图）。`

export function registerCatalogResources(server) {
  server.resource(
    'catalog-domain-knowledge',
    CATALOG_DOMAIN_URI,
    { description: '商品目录领域知识：数据模型（类目/SPU/SKU/ServiceBinding）、定价规则、服务绑定操作指南。', mimeType: 'text/plain' },
    async () => ({
      contents: [{ uri: CATALOG_DOMAIN_URI, mimeType: 'text/plain', text: CATALOG_DOMAIN_KNOWLEDGE }],
    }),
  )
}
