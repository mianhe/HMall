#!/usr/bin/env node
/**
 * 验证详情页库存展示：调用 product/skus/batchStock 链，确认数据可正常获取。
 * 用法: node scripts/verify-detail-stock.mjs [BASE_URL]
 * 默认 BASE_URL=http://localhost (需本地服务已起)
 */
const base = process.argv[2] || 'http://localhost'
const api = `${base.replace(/\/$/, '')}/api`

async function run() {
  console.log('验证详情页库存 API 链...')
  const productId = 3
  const productRes = await fetch(`${api}/products/${productId}`)
  if (!productRes.ok) throw new Error(`GET product ${productId}: ${productRes.status}`)
  const product = await productRes.json()
  console.log('  ✓ 商品:', product.name)

  const skusRes = await fetch(`${api}/products/${productId}/skus`)
  if (!skusRes.ok) throw new Error(`GET skus: ${skusRes.status}`)
  const skus = await skusRes.json()
  const skuIds = skus.map((s) => s.id)
  if (!skuIds.length) {
    console.log('  ⚠ 该商品无 SKU，跳过库存验证')
    return
  }
  console.log('  ✓ SKU 数量:', skuIds.length)

  const stockRes = await fetch(`${api}/inventory/stock/batch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(skuIds),
  })
  if (!stockRes.ok) throw new Error(`POST batch stock: ${stockRes.status}`)
  const stocks = await stockRes.json()
  const outOfStock = stocks.filter((s) => s.available === 0)
  console.log('  ✓ 库存记录数:', stocks.length)
  if (outOfStock.length) {
    console.log('  ✓ 缺货 SKU 数:', outOfStock.length, '(详情页应显示“该规格暂无库存”)')
  }
  console.log('验证通过')
}

run().catch((e) => {
  console.error('验证失败:', e.message)
  process.exit(1)
})
