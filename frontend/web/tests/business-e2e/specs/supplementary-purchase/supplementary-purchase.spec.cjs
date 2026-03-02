/**
 * BIZ-SP-001 补购服务：已交付订单详情页 → 查看可补购服务 → 点击补购 → 创建纯服务订单
 *
 * 对应业务需求：保障服务补购 - 迭代 1
 * 对应路径：N2O-5
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { OrderDetailPage } = require('../../../shared/pages/order-detail.page.cjs')
const { findProductWithServiceBindings } = require('../../../shared/helpers/catalog.helper.cjs')
const { createDeliveredOrder } = require('../../../shared/helpers/order-lifecycle.helper.cjs')

const PRODUCT_ID = Number(process.env.SMOKE_VIRTUAL_BINDING_PRODUCT_ID || 1)

test('BIZ-SP-001 补购服务: 已交付订单 → 可补购服务列表 → 补购下单', async ({ authedPage, request }) => {
  const { page, userId } = authedPage
  const orderDetail = new OrderDetailPage(page)
  let deliveredOrderId = null

  await test.step('Given 存在有服务绑定的实体商品', async () => {
    const productId = await findProductWithServiceBindings(request, PRODUCT_ID)
    expect(productId, '未找到可选服务绑定的商品').toBeTruthy()
  })

  await test.step('Given 用户有一笔已交付的实体商品订单', async () => {
    const skuResp = await request.get(`/api/products/${PRODUCT_ID}/skus`)
    const skus = await skuResp.json()
    const physicalSku = skus.find((s) => s.productType === 'PHYSICAL' || !s.productType)
    expect(physicalSku, '未找到实体 SKU').toBeTruthy()

    const deliveredOrder = await createDeliveredOrder(request, userId, physicalSku.id)
    deliveredOrderId = deliveredOrder.orderId
    expect(deliveredOrderId).toBeTruthy()
  })

  await test.step('When 用户打开已交付订单详情页', async () => {
    await orderDetail.goto(deliveredOrderId)
    await orderDetail.assertOnOrderDetail()
  })

  await test.step('Then 页面显示可补购服务区域', async () => {
    await orderDetail.assertPurchasableServicesVisible()
  })

  await test.step('When 用户点击补购按钮', async () => {
    const originalUrl = page.url()
    await orderDetail.clickFirstPurchaseButton()
    await page.waitForURL((url) => url.toString() !== originalUrl, { timeout: 15000 })
    const newOrderId = orderDetail.getOrderIdFromUrl()
    const originalOrderId = String(deliveredOrderId)
    expect(newOrderId).not.toEqual(originalOrderId)
  })

  await test.step('Then 跳转到补购订单详情页', async () => {
    await orderDetail.assertOnOrderDetail()
  })
})
