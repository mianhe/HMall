/**
 * BIZ-LE-005 含镭雕订单主流程：购买实体+镭雕 → 下单 → 支付 → 履约配货 → 完成镭雕 → 发货 → 签收 → OrderCompleted
 *
 * 对应业务需求：镭雕迭代 2 - 下单与履约
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../../shared/pages/product-detail.page.cjs')
const { CheckoutPage } = require('../../../shared/pages/checkout.page.cjs')
const { findProductWithEngravingService } = require('../../../shared/helpers/catalog.helper.cjs')
const { advanceEngravingOrderToDelivered } = require('../../../shared/helpers/engraving-order.helper.cjs')

test('BIZ-LE-005 含镭雕订单主流程: 选镭雕→下单→支付→完成镭雕→发货→签收', async ({ authedPage, request }) => {
  const { page, userId } = authedPage
  const product = new ProductDetailPage(page)
  const checkout = new CheckoutPage(page)
  let productId = null
  let orderId = null

  await test.step('Given 存在绑定镭雕服务的实体商品', async () => {
    productId = await findProductWithEngravingService(request, 1)
    if (!productId) {
      test.skip(true, '未找到镭雕服务商品，请通过 MCP 或 Admin 创建')
      return
    }
  })

  if (!productId) return

  await test.step('When 详情页选镭雕、图案后立即购买并提交订单', async () => {
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
      test.skip(true, '当前选中的非镭雕服务或无图案可选')
      return
    }
    await product.buyNow()
    await checkout.assertLoaded()
    await checkout.fillAddress()
    await checkout.submitOrder()
    await checkout.assertRedirectToOrderDetail()
    const match = page.url().match(/\/orders\/(\d+)/)
    orderId = match ? parseInt(match[1], 10) : null
  })

  if (!orderId || !userId) return

  await test.step('When 支付并完成镭雕→发货→签收', async () => {
    const order = await advanceEngravingOrderToDelivered(request, orderId, userId)
    expect(order.status).toBe('DELIVERED')
  })

  await test.step('Then 订单已完成', async () => {
    const resp = await request.get(`/api/orders/${orderId}`)
    expect(resp.ok()).toBeTruthy()
    const order = await resp.json()
    expect(['DELIVERED', 'COMPLETED']).toContain(order.status)
  })
})
