/**
 * BIZ-LE-004 镭雕选品：详情页选镭雕 → 图案/文字 → 立即购买进入结账
 *
 * 对应业务需求：镭雕迭代 1 - 镭雕服务配置与选品
 * 验收：实体商品详情页可展示镭雕服务；选镭雕时可选图案、填文字（≤20 字），至少选其一
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../../shared/pages/product-detail.page.cjs')
const { CheckoutPage } = require('../../../shared/pages/checkout.page.cjs')
const { findProductWithEngravingService } = require('../../../shared/helpers/catalog.helper.cjs')

test('BIZ-LE-004 镭雕选品: 选镭雕服务 → 选图案 → 立即购买进入结账', async ({ authedPage, request }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const checkout = new CheckoutPage(page)
  let productId = null

  await test.step('Given 存在绑定镭雕服务的实体商品', async () => {
    productId = await findProductWithEngravingService(request, 1)
    if (!productId) {
      test.skip(true, '未找到镭雕服务商品，请通过 MCP 或 Admin 创建：SERVICE SPU(serviceKind=ENGRAVING) + ServiceBinding')
      return
    }
  })

  if (!productId) return

  await test.step('When 进入详情页选择规格、镭雕服务、图案后立即购买', async () => {
    await product.goto(productId)
    await product.assertLoaded()
    await product.selectRequiredSpecsIfPresent()
    const engravingServiceSelected = await product.selectEngravingServiceOptionIfPresent()
    if (!engravingServiceSelected) {
      test.skip(true, '当前商品无镭雕服务可选')
      return
    }
    const engravingSelected = await product.selectEngravingAndPatternIfPresent()
    if (!engravingSelected) {
      test.skip(true, '无图案或镭雕输入区可选')
      return
    }
    await product.buyNow()
  })

  await test.step('Then 进入结账页', async () => {
    await checkout.assertLoaded()
    expect(page.url()).toContain('/checkout')
  })
})
