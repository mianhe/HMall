/**
 * Catalog 模块的 MCP tools（收敛版）：7 个 resource-level tools + 1 个 upload，共 8 个。
 * 调后端 REST API。设计见 hmall-mcp/docs/TOOLS.md。
 */
import { z } from 'zod'
import { readFile } from 'node:fs/promises'
import { basename } from 'node:path'

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

async function uploadFile(localPath) {
  const fileBuffer = await readFile(localPath)
  const fileName = basename(localPath)
  const blob = new Blob([fileBuffer])
  const formData = new FormData()
  formData.append('file', blob, fileName)
  const res = await fetch(`${API_BASE}/files/upload`, { method: 'POST', body: formData })
  const text = await res.text()
  if (!res.ok) {
    let msg = `${res.status}`
    try { msg = JSON.parse(text).message || msg } catch {}
    throw new Error(msg)
  }
  return JSON.parse(text)
}

function ok(text) {
  return { content: [{ type: 'text', text }] }
}

function err(e) {
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

export function registerCatalogTools(server) {
  const actionList = z.enum(['list', 'tree', 'get', 'create', 'update', 'delete'])
  const actionProducts = z.enum(['list', 'get', 'create', 'update', 'delete'])
  const actionDims = z.enum(['list', 'add_dimension', 'add_option', 'delete_option'])
  const actionSkus = z.enum(['list', 'create', 'update', 'delete'])
  const actionImages = z.enum(['list', 'add', 'delete'])

  // ── 1. catalog_categories ──
  server.tool(
    'catalog_categories',
    '类目（分类）查询与管理。树形结构：根类目 → 子类目 → 叶子类目，商品只能挂在叶子类目下。action=list 查类目列表（传 parentId 查子类目，不传查根类目）；tree 返回完整类目树；get/create/update/delete 按 categoryId 操作。',
    {
      action: actionList.describe('list|tree|get|create|update|delete'),
      parentId: z.number().optional().describe('list 时：不传查根类目，传则查子类目'),
      categoryId: z.number().optional().describe('get/update/delete 时必填'),
      name: z.string().optional().describe('create/update 时必填'),
      description: z.string().optional(),
    },
    async ({ action, parentId, categoryId, name, description }) => {
      try {
        if (action === 'list') {
          const qs = parentId != null ? `?parentId=${parentId}` : ''
          const list = await api('GET', `/categories${qs}`)
          if (!list.length) return ok('暂无类目。')
          const lines = list.map(c => `[${c.id}] ${c.name}${c.description ? ' - ' + c.description : ''}${c.parentId ? ' (父:' + c.parentId + ')' : ''}`)
          return ok(lines.join('\n'))
        }
        if (action === 'tree') {
          const tree = await api('GET', '/categories/tree')
          if (!tree.length) return ok('暂无类目。')
          const lines = []
          function walk(nodes, indent) {
            for (const n of nodes) {
              lines.push(`${'  '.repeat(indent)}[${n.id}] ${n.name}${n.description ? ' - ' + n.description : ''}`)
              if (n.children?.length) walk(n.children, indent + 1)
            }
          }
          walk(tree, 0)
          return ok(lines.join('\n'))
        }
        if (action === 'get') {
          const c = await api('GET', `/categories/${categoryId}`)
          return ok(`类目详情：\n- ID: ${c.id}\n- 名称: ${c.name}\n- 描述: ${c.description || '—'}\n- 父类目ID: ${c.parentId ?? '根'}`)
        }
        if (action === 'create') {
          const c = await api('POST', '/categories', { name, description: description ?? null, parentId: parentId ?? null })
          return ok(`创建成功：ID=${c.id}，名称="${c.name}"，父类目=${c.parentId ?? '根'}`)
        }
        if (action === 'update') {
          const c = await api('PUT', `/categories/${categoryId}`, { name, description: description ?? null })
          return ok(`修改成功：ID=${c.id}，名称="${c.name}"，描述="${c.description || '—'}"`)
        }
        if (action === 'delete') {
          await api('DELETE', `/categories/${categoryId}`)
          return ok(`删除成功：类目 ID=${categoryId} 已删除。`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  // ── 2. catalog_products ──
  server.tool(
    'catalog_products',
    '商品(SPU)查询与管理。action=list：按 categoryId 过滤类目下商品，或按 keyword 模糊搜索商品名称（仅名称匹配，不支持语义搜索；搜不到时建议换同义词或改用类目浏览）。get + detail=full：返回完整信息含规格维度、选项和全部 SKU 列表（含价格，单位：分，如 599900=¥5999）。create/update/delete：管理操作。create 时可传 productType（PHYSICAL 默认 / SERVICE 服务类商品）。',
    {
      action: actionProducts,
      categoryId: z.number().optional().describe('list 时按类目过滤；create 时必填'),
      keyword: z.string().optional().describe('list 时按关键词搜索'),
      productId: z.number().optional().describe('get/update/delete 时必填'),
      detail: z.enum(['basic', 'full']).optional().describe('get 时：full 含规格与 SKU'),
      name: z.string().optional().describe('create/update 时必填'),
      description: z.string().optional(),
      productType: z.enum(['PHYSICAL', 'SERVICE']).optional().describe('create 时：PHYSICAL(默认) 或 SERVICE'),
    },
    async ({ action, categoryId, keyword, productId, detail, name, description, productType }) => {
      try {
        if (action === 'list') {
          if (keyword != null && keyword !== '') {
            const list = await api('GET', `/products/search?keyword=${encodeURIComponent(keyword)}`)
            if (!list.length) return ok(`未找到包含"${keyword}"的商品。`)
            const lines = list.map(p => `[${p.id}] ${p.name} [${p.productType || 'PHYSICAL'}]${p.description ? ' - ' + p.description : ''} (类目:${p.categoryId})`)
            return ok(`共 ${list.length} 个商品：\n${lines.join('\n')}`)
          }
          const path = categoryId != null ? `/products?categoryId=${categoryId}` : '/products/search'
          const list = await api('GET', path)
          if (!list.length) return ok(categoryId != null ? '该类目下暂无商品。' : '暂无商品。')
          const lines = list.map(p => `[${p.id}] ${p.name} [${p.productType || 'PHYSICAL'}]${p.description ? ' - ' + p.description : ''}${p.categoryId != null ? ' (类目:' + p.categoryId + ')' : ''}`)
          return ok(lines.join('\n'))
        }
        if (action === 'get') {
          const p = await api('GET', `/products/${productId}`)
          if (detail === 'full') {
            const [dims, skus] = await Promise.all([
              api('GET', `/products/${productId}/dimensions`),
              api('GET', `/products/${productId}/skus`),
            ])
            const lines = [`商品 [${p.id}] ${p.name} [${p.productType || 'PHYSICAL'}]${p.description ? ' - ' + p.description : ''} (类目:${p.categoryId})`]
            if (dims.length) {
              lines.push('\n规格维度：')
              for (const d of dims) {
                const opts = (d.options || []).map(o => `${o.optionValue}(id:${o.id})`).join(', ')
                lines.push(`  [${d.id}] ${d.name}${d.required ? '(必填)' : '(可选)'}：${opts || '无选项'}`)
              }
            }
            if (skus.length) {
              lines.push('\nSKU列表：')
              for (const s of skus) {
                const spec = (s.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join(', ')
                lines.push(`  [${s.id}] ${spec} ¥${(s.priceCents / 100).toFixed(2)}${s.displayName ? ' ' + s.displayName : ''}`)
              }
            }
            return ok(lines.join('\n'))
          }
          return ok(`[${p.id}] ${p.name} [${p.productType || 'PHYSICAL'}]${p.description ? ' - ' + p.description : ''} (类目:${p.categoryId})`)
        }
        if (action === 'create') {
          const body = { categoryId, name, description: description ?? null }
          if (productType) body.productType = productType
          const p = await api('POST', '/products', body)
          return ok(`创建成功：ID=${p.id}，名称="${p.name}"，类型=${p.productType || 'PHYSICAL'}`)
        }
        if (action === 'update') {
          const p = await api('PUT', `/products/${productId}`, { name, description: description ?? null })
          return ok(`修改成功：ID=${p.id}，名称="${p.name}"，描述="${p.description || '—'}"`)
        }
        if (action === 'delete') {
          await api('DELETE', `/products/${productId}`)
          return ok(`删除成功：商品 ID=${productId} 已删除。`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  // ── 3. catalog_dimensions ──
  server.tool(
    'catalog_dimensions',
    '商品的规格维度与选项管理。每个 SPU 可有多个维度（如颜色、版本），每个维度下有多个选项（如黑色、512GB）。创建 SKU 时需要指定各维度的 optionId。action=list 查维度及选项；add_dimension/add_option 添加；delete_option 删除。',
    {
      action: actionDims,
      spuId: z.number().describe('商品(SPU) ID'),
      dimensionId: z.number().optional().describe('add_option/delete_option 时必填'),
      optionId: z.number().optional().describe('delete_option 时必填'),
      name: z.string().optional().describe('add_dimension 时必填，如容量/颜色'),
      required: z.boolean().optional().describe('add_dimension 时必填'),
      optionValue: z.string().optional().describe('add_option 时必填'),
      sortOrder: z.number().optional(),
    },
    async ({ action, spuId, dimensionId, optionId, name, required, optionValue, sortOrder }) => {
      try {
        if (action === 'list') {
          const dims = await api('GET', `/products/${spuId}/dimensions`)
          if (!dims.length) return ok('该商品暂无规格维度。')
          const lines = dims.map(d => {
            const opts = (d.options || []).map(o => `${o.optionValue}(id:${o.id})`).join(', ') || '无'
            return `[${d.id}] ${d.name}${d.required ? '(必填)' : '(可选)'}：${opts}`
          })
          return ok(lines.join('\n'))
        }
        if (action === 'add_dimension') {
          const d = await api('POST', `/products/${spuId}/dimensions`, { name, required })
          return ok(`添加维度成功：ID=${d.id}，名称="${d.name}"`)
        }
        if (action === 'add_option') {
          const o = await api('POST', `/products/${spuId}/dimensions/${dimensionId}/options`, { optionValue, sortOrder: sortOrder ?? null })
          return ok(`添加选项成功：ID=${o.id}，值="${o.optionValue}"`)
        }
        if (action === 'delete_option') {
          await api('DELETE', `/products/${spuId}/dimensions/${dimensionId}/options/${optionId}`)
          return ok(`已删除选项 ID=${optionId}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  // ── 4. catalog_skus ──
  server.tool(
    'catalog_skus',
    'SKU 查询与管理。SKU = 各规格维度选项的组合 + 价格（priceCents，单位：分）。action=list 查某 SPU 下所有 SKU；create 需传 specOptionIds（各维度选项 ID 列表）+ priceCents；update 可改价格或展示名；delete 删除。',
    {
      action: actionSkus,
      spuId: z.number().describe('商品(SPU) ID'),
      skuId: z.number().optional().describe('update/delete 时必填'),
      specOptionIds: z.array(z.number()).optional().describe('create 时必填'),
      priceCents: z.number().min(0).optional().describe('create 必填，update 可选'),
      displayName: z.string().optional(),
    },
    async ({ action, spuId, skuId, specOptionIds, priceCents, displayName }) => {
      try {
        if (action === 'list') {
          const list = await api('GET', `/products/${spuId}/skus`)
          if (!list.length) return ok('该商品暂无 SKU。')
          const lines = list.map(s => {
            const spec = (s.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join(', ') || '—'
            return `[${s.id}] ${spec} ¥${(s.priceCents / 100).toFixed(2)}${s.displayName ? ' ' + s.displayName : ''}`
          })
          return ok(lines.join('\n'))
        }
        if (action === 'create') {
          const s = await api('POST', `/products/${spuId}/skus`, { specOptionIds, priceCents, displayName: displayName ?? null })
          const spec = (s.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join(' · ')
          return ok(`创建 SKU 成功：ID=${s.id}，规格=${spec}，价格=${(s.priceCents / 100).toFixed(2)}元`)
        }
        if (action === 'update') {
          const body = {}
          if (priceCents != null) body.priceCents = priceCents
          if (displayName !== undefined) body.displayName = displayName ?? null
          const s = await api('PUT', `/products/${spuId}/skus/${skuId}`, body)
          const spec = (s.specValues || []).map(v => `${v.dimensionName}:${v.optionValue}`).join(' · ')
          return ok(`修改成功：ID=${s.id}，规格=${spec}，价格=${(s.priceCents / 100).toFixed(2)}元`)
        }
        if (action === 'delete') {
          await api('DELETE', `/products/${spuId}/skus/${skuId}`)
          return ok(`已删除 SKU ID=${skuId}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  // ── 5. catalog_upload_image ──
  server.tool(
    'catalog_upload_image',
    '上传本地图片到服务器，返回可访问 URL（可用于展示图的 imageUrl 参数）。需后端 MinIO 已启动。',
    { localPath: z.string().describe('本地图片文件的绝对路径') },
    async ({ localPath }) => {
      try {
        const result = await uploadFile(localPath)
        return ok(`上传成功：${result.url}`)
      } catch (e) {
        if (e.message === '404') {
          return err(new Error('上传接口不可用(404)。请确认 catalog-service 已配置 minio.enabled=true 且已启动。'))
        }
        return err(e)
      }
    }
  )

  // ── 6. catalog_product_images ──
  server.tool(
    'catalog_product_images',
    '产品级展示图管理（不绑定具体规格选项）。action=list 查看；add 添加（imageUrl 或 localPath 二选一，传 localPath 时内部自动上传）；delete 删除。',
    {
      action: actionImages,
      spuId: z.number().describe('商品(SPU) ID'),
      imageUrl: z.string().optional().describe('add 时与 localPath 二选一'),
      localPath: z.string().optional().describe('add 时与 imageUrl 二选一，内部先上传再添加'),
      sortOrder: z.number().optional(),
      imageId: z.number().optional().describe('delete 时必填'),
    },
    async ({ action, spuId, imageUrl, localPath, sortOrder, imageId }) => {
      try {
        if (action === 'list') {
          const images = await api('GET', `/products/${spuId}/images`)
          if (!images.length) return ok('该产品暂无产品级展示图。')
          const lines = images.map(i => `[${i.id}] ${i.imageUrl} (排序:${i.sortOrder ?? '—'})`)
          return ok(lines.join('\n'))
        }
        if (action === 'add') {
          let url = imageUrl
          if (localPath != null && localPath !== '') {
            const result = await uploadFile(localPath)
            url = result.url
          }
          const img = await api('POST', `/products/${spuId}/images`, { imageUrl: url, sortOrder: sortOrder ?? null })
          return ok(`添加产品级展示图成功：ID=${img.id}，URL=${img.imageUrl}`)
        }
        if (action === 'delete') {
          await api('DELETE', `/products/${spuId}/images/${imageId}`)
          return ok(`已删除产品级展示图 ID=${imageId}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  // ── 7. catalog_option_images ──
  server.tool(
    'catalog_option_images',
    '选项级展示图管理（绑定到具体维度选项，如「颜色:黑色」的图片）。action=list 查看；add 添加（imageUrl 或 localPath 二选一）；delete 删除。',
    {
      action: actionImages,
      spuId: z.number().describe('商品(SPU) ID'),
      dimensionId: z.number().describe('维度 ID'),
      optionId: z.number().describe('选项 ID'),
      imageUrl: z.string().optional(),
      localPath: z.string().optional(),
      sortOrder: z.number().optional(),
      imageId: z.number().optional().describe('delete 时必填'),
    },
    async ({ action, spuId, dimensionId, optionId, imageUrl, localPath, sortOrder, imageId }) => {
      try {
        const base = `/products/${spuId}/dimensions/${dimensionId}/options/${optionId}`
        if (action === 'list') {
          const images = await api('GET', `${base}/images`)
          if (!images.length) return ok('该选项暂无展示图。')
          const lines = images.map(i => `[${i.id}] ${i.imageUrl} (排序:${i.sortOrder ?? '—'})`)
          return ok(lines.join('\n'))
        }
        if (action === 'add') {
          let url = imageUrl
          if (localPath != null && localPath !== '') {
            const result = await uploadFile(localPath)
            url = result.url
          }
          const img = await api('POST', `${base}/images`, { imageUrl: url, sortOrder: sortOrder ?? null })
          return ok(`添加展示图成功：ID=${img.id}，URL=${img.imageUrl}`)
        }
        if (action === 'delete') {
          await api('DELETE', `${base}/images/${imageId}`)
          return ok(`已删除展示图 ID=${imageId}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  // ── 8. catalog_service_bindings ──
  const actionBindings = z.enum(['list', 'create', 'delete'])
  server.tool(
    'catalog_service_bindings',
    '服务绑定(ServiceBinding)管理。将 SERVICE 类型商品的 SKU 绑定到 PHYSICAL 类型商品(SPU)，并指定价格。三种定价模式：(1)无 binding → 服务独立售卖，价格=SKU.priceCents；(2)binding+priceCents 为空 → 限定适用范围，继承 SKU 标准价；(3)binding+priceCents 非空 → 上下文定价，覆盖 SKU 标准价。action=list 查某服务 SKU 的全部 binding；create 创建 binding；delete 删除 binding。',
    {
      action: actionBindings.describe('list|create|delete'),
      skuId: z.number().describe('服务 SKU ID'),
      bindingId: z.number().optional().describe('delete 时必填'),
      targetSpuId: z.number().optional().describe('create 时必填：绑定到的实体商品(SPU) ID'),
      priceCents: z.number().min(0).nullable().optional().describe('create 时可选：绑定价格（分），null 表示继承 SKU 标准价'),
    },
    async ({ action, skuId, bindingId, targetSpuId, priceCents }) => {
      try {
        if (action === 'list') {
          const list = await api('GET', `/skus/${skuId}/service-bindings`)
          if (!list.length) return ok('该服务 SKU 暂无绑定。')
          const lines = list.map(b => {
            const price = b.priceCents != null ? `¥${(b.priceCents / 100).toFixed(2)}` : '继承SKU标准价'
            return `[${b.id}] → 目标SPU:${b.targetSpuId}${b.targetSpuName ? '(' + b.targetSpuName + ')' : ''} 价格:${price}`
          })
          return ok(lines.join('\n'))
        }
        if (action === 'create') {
          const body = { targetSpuId, priceCents: priceCents ?? null }
          const b = await api('POST', `/skus/${skuId}/service-bindings`, body)
          const price = b.priceCents != null ? `¥${(b.priceCents / 100).toFixed(2)}` : '继承SKU标准价'
          return ok(`创建绑定成功：ID=${b.id}，服务SKU=${b.serviceSkuId} → 目标SPU=${b.targetSpuId}，价格=${price}`)
        }
        if (action === 'delete') {
          await api('DELETE', `/skus/${skuId}/service-bindings/${bindingId}`)
          return ok(`已删除绑定 ID=${bindingId}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
