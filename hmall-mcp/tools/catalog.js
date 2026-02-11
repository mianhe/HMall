/**
 * Catalog 模块的 MCP tools —— 调后端 REST API。
 */
import { z } from 'zod'

const API_BASE = process.env.HMALL_API_BASE || 'http://localhost:8080/api'

async function api(method, path, body) {
  const url = `${API_BASE}${path}`
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body !== undefined) {
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(url, opts)
  const text = await res.text()
  if (!res.ok) {
    let msg = `${res.status}`
    try { msg = JSON.parse(text).message || msg } catch {}
    throw new Error(msg)
  }
  return text ? JSON.parse(text) : null
}

function ok(text) {
  return { content: [{ type: 'text', text }] }
}

function err(e) {
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

export function registerCatalogTools(server) {

  // ── 类目 ──

  server.tool(
    'catalog_list_categories',
    '查询类目列表（不传 parentId 查根类目，传则查子类目）',
    { parentId: z.number().optional().describe('父类目 ID，不传则查根类目') },
    async ({ parentId }) => {
      try {
        const qs = parentId != null ? `?parentId=${parentId}` : ''
        const list = await api('GET', `/categories${qs}`)
        if (!list.length) return ok('暂无类目。')
        const header = '| ID | 名称 | 父类目ID | 描述 |\n|---|---|---|---|'
        const rows = list.map(c => `| ${c.id} | ${c.name} | ${c.parentId ?? '—'} | ${c.description || '—'} |`)
        return ok(`${header}\n${rows.join('\n')}`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_create_category',
    '创建类目（根类目不传 parentId，子类目传 parentId）',
    {
      name: z.string().describe('类目名称'),
      description: z.string().optional().describe('类目描述'),
      parentId: z.number().optional().describe('父类目 ID'),
    },
    async ({ name, description, parentId }) => {
      try {
        const c = await api('POST', '/categories', { name, description: description || null, parentId: parentId || null })
        return ok(`创建成功：ID=${c.id}，名称="${c.name}"，父类目=${c.parentId ?? '根'}`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_get_category',
    '按 ID 查询类目详情',
    { categoryId: z.number().describe('类目 ID') },
    async ({ categoryId }) => {
      try {
        const c = await api('GET', `/categories/${categoryId}`)
        return ok(`类目详情：\n- ID: ${c.id}\n- 名称: ${c.name}\n- 描述: ${c.description || '—'}\n- 父类目ID: ${c.parentId ?? '根'}`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_update_category',
    '修改类目（名称、描述）',
    {
      categoryId: z.number().describe('类目 ID'),
      name: z.string().describe('新名称'),
      description: z.string().optional().describe('新描述'),
    },
    async ({ categoryId, name, description }) => {
      try {
        const c = await api('PUT', `/categories/${categoryId}`, { name, description: description ?? null })
        return ok(`修改成功：ID=${c.id}，名称="${c.name}"，描述="${c.description || '—'}"`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_delete_category',
    '删除类目',
    { categoryId: z.number().describe('类目 ID') },
    async ({ categoryId }) => {
      try {
        await api('DELETE', `/categories/${categoryId}`)
        return ok(`删除成功：类目 ID=${categoryId} 已删除。`)
      } catch (e) { return err(e) }
    }
  )

  // ── 商品(SPU) ──

  server.tool(
    'catalog_list_products',
    '按类目查询商品列表',
    { categoryId: z.number().describe('类目 ID') },
    async ({ categoryId }) => {
      try {
        const list = await api('GET', `/products?categoryId=${categoryId}`)
        if (!list.length) return ok('该类目下暂无商品。')
        const header = '| ID | 名称 | 描述 |\n|---|---|---|'
        const rows = list.map(p => `| ${p.id} | ${p.name} | ${p.description || '—'} |`)
        return ok(`${header}\n${rows.join('\n')}`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_get_product',
    '查询商品详情（含基础信息）',
    { productId: z.number().describe('商品 ID') },
    async ({ productId }) => {
      try {
        const p = await api('GET', `/products/${productId}`)
        return ok(`商品详情：\n- ID: ${p.id}\n- 名称: ${p.name}\n- 描述: ${p.description || '—'}\n- 类目ID: ${p.categoryId}`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_create_product',
    '在叶子类目下创建商品',
    {
      categoryId: z.number().describe('类目 ID（须为叶子类目）'),
      name: z.string().describe('商品名称'),
      description: z.string().optional().describe('商品描述'),
    },
    async ({ categoryId, name, description }) => {
      try {
        const p = await api('POST', '/products', { categoryId, name, description: description || null })
        return ok(`创建成功：ID=${p.id}，名称="${p.name}"`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_update_product',
    '修改商品（名称、描述）',
    {
      productId: z.number().describe('商品 ID'),
      name: z.string().describe('新名称'),
      description: z.string().optional().describe('新描述'),
    },
    async ({ productId, name, description }) => {
      try {
        const p = await api('PUT', `/products/${productId}`, { name, description: description ?? null })
        return ok(`修改成功：ID=${p.id}，名称="${p.name}"，描述="${p.description || '—'}"`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_delete_product',
    '删除商品',
    { productId: z.number().describe('商品 ID') },
    async ({ productId }) => {
      try {
        await api('DELETE', `/products/${productId}`)
        return ok(`删除成功：商品 ID=${productId} 已删除。`)
      } catch (e) { return err(e) }
    }
  )

  // ── 规格维度与选项 ──

  server.tool(
    'catalog_list_dimensions',
    '查询某 SPU 的规格维度及选项列表',
    { spuId: z.number().describe('商品(SPU) ID') },
    async ({ spuId }) => {
      try {
        const dims = await api('GET', `/products/${spuId}/dimensions`)
        if (!dims.length) return ok('该商品暂无规格维度。')
        const lines = dims.map(d => {
          const opts = (d.options || []).map(o => o.optionValue).join('、') || '无'
          const tags = [d.required ? '必填' : '可选', d.affectsAppearance ? '影响外观' : ''].filter(Boolean).join(' ')
          return `- **${d.name}**（${tags}）：${opts}`
        })
        return ok(`SPU ${spuId} 的规格维度：\n${lines.join('\n')}`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_add_dimension',
    '为 SPU 添加规格维度',
    {
      spuId: z.number().describe('商品(SPU) ID'),
      name: z.string().describe('维度名称，如"容量""颜色"'),
      required: z.boolean().describe('创建 SKU 时是否必选'),
      affectsAppearance: z.boolean().optional().describe('是否影响外观（选项可带图片）'),
    },
    async ({ spuId, name, required, affectsAppearance }) => {
      try {
        const d = await api('POST', `/products/${spuId}/dimensions`, { name, required, affectsAppearance: affectsAppearance || false })
        return ok(`添加维度成功：ID=${d.id}，名称="${d.name}"`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_add_option',
    '为某维度添加可选项',
    {
      spuId: z.number().describe('商品(SPU) ID'),
      dimensionId: z.number().describe('维度 ID'),
      optionValue: z.string().describe('选项值，如"128G""黑色"'),
      sortOrder: z.number().optional().describe('排序（越小越靠前）'),
      image: z.string().optional().describe('图片链接（仅影响外观的维度可填）'),
    },
    async ({ spuId, dimensionId, optionValue, sortOrder, image }) => {
      try {
        const o = await api('POST', `/products/${spuId}/dimensions/${dimensionId}/options`, {
          optionValue,
          sortOrder: sortOrder ?? null,
          image: image || null,
        })
        return ok(`添加选项成功：ID=${o.id}，值="${o.optionValue}"`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_delete_option',
    '删除某维度的规格选项（若已被 SKU 使用则无法删除）',
    {
      spuId: z.number().describe('商品(SPU) ID'),
      dimensionId: z.number().describe('维度 ID'),
      optionId: z.number().describe('选项 ID'),
    },
    async ({ spuId, dimensionId, optionId }) => {
      try {
        await api('DELETE', `/products/${spuId}/dimensions/${dimensionId}/options/${optionId}`)
        return ok(`已删除选项 ID=${optionId}`)
      } catch (e) { return err(e) }
    }
  )

  // ── SKU ──

  server.tool(
    'catalog_list_skus',
    '查询某 SPU 下的 SKU 列表',
    { spuId: z.number().describe('商品(SPU) ID') },
    async ({ spuId }) => {
      try {
        const list = await api('GET', `/products/${spuId}/skus`)
        if (!list.length) return ok('该商品暂无 SKU。')
        const header = '| ID | 规格 | 价格(元) | 展示名 |\n|---|---|---|---|'
        const rows = list.map(s => {
          const spec = (s.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join(' · ') || '—'
          const price = (s.priceCents / 100).toFixed(2)
          return `| ${s.id} | ${spec} | ${price} | ${s.displayName || '—'} |`
        })
        return ok(`${header}\n${rows.join('\n')}`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_create_sku',
    '为 SPU 创建 SKU（选择各维度选项 + 价格）',
    {
      spuId: z.number().describe('商品(SPU) ID'),
      specOptionIds: z.array(z.number()).describe('所选选项 ID 列表（每个必填维度选一个）'),
      priceCents: z.number().min(0).describe('价格，单位：分'),
      displayName: z.string().optional().describe('展示名（可选）'),
    },
    async ({ spuId, specOptionIds, priceCents, displayName }) => {
      try {
        const s = await api('POST', `/products/${spuId}/skus`, {
          specOptionIds,
          priceCents,
          displayName: displayName || null,
        })
        const spec = (s.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join(' · ')
        return ok(`创建 SKU 成功：ID=${s.id}，规格=${spec}，价格=${(s.priceCents / 100).toFixed(2)}元`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_update_sku',
    '修改 SKU（价格、展示名）',
    {
      spuId: z.number().describe('商品(SPU) ID'),
      skuId: z.number().describe('SKU ID'),
      priceCents: z.number().min(0).optional().describe('新价格（分），不传则不变'),
      displayName: z.string().optional().describe('新展示名，不传则不变'),
    },
    async ({ spuId, skuId, priceCents, displayName }) => {
      try {
        const body = {}
        if (priceCents != null) body.priceCents = priceCents
        if (displayName !== undefined) body.displayName = displayName || null
        const s = await api('PUT', `/products/${spuId}/skus/${skuId}`, body)
        const spec = (s.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join(' · ')
        return ok(`修改成功：ID=${s.id}，规格=${spec}，价格=${(s.priceCents / 100).toFixed(2)}元`)
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'catalog_delete_sku',
    '删除 SKU',
    {
      spuId: z.number().describe('商品(SPU) ID'),
      skuId: z.number().describe('SKU ID'),
    },
    async ({ spuId, skuId }) => {
      try {
        await api('DELETE', `/products/${spuId}/skus/${skuId}`)
        return ok(`已删除 SKU ID=${skuId}`)
      } catch (e) { return err(e) }
    }
  )
}
