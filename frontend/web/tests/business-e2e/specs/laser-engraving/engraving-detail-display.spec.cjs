/**
 * BIZ-LE-007 订单与履约详情展示镭雕内容：用户可在订单详情页查看镭雕内容及完成状态
 *
 * 对应业务需求：镭雕迭代 3 - 订单与履约详情展示
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../../shared/pages/product-detail.page.cjs')
const { CheckoutPage } = require('../../../shared/pages/checkout.page.cjs')
const { findProductWithEngravingService } = require('../../../shared/helpers/catalog.helper.cjs')
const { advanceEngravingOrderToDelivered } = require('../../../shared/helpers/engraving-order.helper.cjs')

test('BIZ-LE-007 订单详情页应展示镭雕内容', async ({ authedPage, request }) => {
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

  await test.step('Then 订单详情页应展示镭雕内容', async () => {
    await page.goto(`/orders/${orderId}`)
    await page.waitForLoadState('networkidle')
    const resp = await request.get(`/api/orders/${orderId}`)
    expect(resp.ok()).toBeTruthy()
    const order = await resp.json()
    const hasEngravingAttrs = (i) => {
      const a = i.serviceAttributes || i.service_attributes
      if (!a) return false
      const pid = a.engravingPatternId ?? a.engraving_pattern_id
      const pname = a.engravingPatternName ?? a.engraving_pattern_name
      const text = a.engravingText ?? a.engraving_text
      return (pid != null || pname) || (text != null && String(text).trim() !== '')
    }
    const engravingItem = order.items?.find((i) => i.itemType === 'SERVICE' && hasEngravingAttrs(i))
    expect(engravingItem).toBeTruthy()
    expect(engravingItem.serviceAttributes || engravingItem.service_attributes).toBeTruthy()
    await expect(page.getByText(/镭雕：/)).toBeVisible()
  })
})
