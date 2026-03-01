/**
 * BIZ-VP-003 混合订单完整交易生命周期：
 *   下单 → 模拟支付 → 服务激活 → 实体发货签收 → 订单完成
 *
 * 验证虚拟商品交易流程中，服务激活与物理履约并行完成后，
 * 订单正确进入"已完成"状态，事件回放包含 OrderCompleted。
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../../shared/pages/product-detail.page.cjs')
const { CheckoutPage } = require('../../../shared/pages/checkout.page.cjs')
const { OrderDetailPage } = require('../../../shared/pages/order-detail.page.cjs')
const { findProductWithServiceBindings } = require('../../../shared/helpers/catalog.helper.cjs')

const PRODUCT_ID = Number(process.env.SMOKE_VIRTUAL_BINDING_PRODUCT_ID || 1)

test('BIZ-VP-003 混合订单完整生命周期: 下单→支付→服务激活→发货→签收→订单完成', async ({ authedPage, request }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const checkout = new CheckoutPage(page)
  const orderDetail = new OrderDetailPage(page)
  let productId = null
  let orderId = null

  await test.step('Given 选择实体+服务商品并提交订单', async () => {
    productId = await findProductWithServiceBindings(request, PRODUCT_ID)
    expect(productId, '未找到可绑定服务的商品').toBeTruthy()

    await product.goto(productId)
    await product.assertLoaded()
    await product.selectRequiredSpecsIfPresent()
    const selected = await product.selectFirstServiceOptionIfPresent()
    expect(selected, '详情页应有可选服务').toBeTruthy()
    await product.buyNow()

    await checkout.assertLoaded()
    await checkout.assertSummaryItemCountAtLeast(2)
    await checkout.fillAddress()
    await checkout.submitOrder()
    await checkout.assertRedirectToOrderDetail()

    const url = page.url()
    orderId = Number(url.match(/\/orders\/(\d+)/)[1])
    expect(orderId).toBeGreaterThan(0)
  })

  await test.step('When 模拟支付成功', async () => {
    const paymentResp = await request.get(`/api/payments/by-order/${orderId}`)
    expect(paymentResp.ok(), '应能查到支付单').toBeTruthy()
    const payment = await paymentResp.json()

    const callbackResp = await request.post('/api/payments/callback', {
      data: { paymentId: payment.paymentId, success: true },
    })
    expect(callbackResp.ok(), '支付回调应成功').toBeTruthy()
  })

  await test.step('When 等待履约单创建并驱动物理履约到签收', async () => {
    let physicalFulfillmentId = null

    await expect(async () => {
      const resp = await request.get(`/api/fulfillment?orderId=${orderId}`)
      expect(resp.ok()).toBeTruthy()
      const orders = await resp.json()
      const physical = orders.find(o => o.fulfillmentType === 'PHYSICAL')
      expect(physical, '应有 PHYSICAL 履约单').toBeTruthy()
      physicalFulfillmentId = physical.fulfillmentOrderId
    }).toPass({ timeout: 10000 })

    const allocateResp = await request.post(`/api/fulfillment/${physicalFulfillmentId}/allocate`)
    expect(allocateResp.ok(), `配货应成功: ${allocateResp.status()}`).toBeTruthy()

    const shipResp = await request.post(`/api/fulfillment/${physicalFulfillmentId}/ship`, {
      data: { carrier: 'SF Express', trackingNumber: 'SF' + Date.now() },
    })
    expect(shipResp.ok(), '发货应成功').toBeTruthy()

    const deliverResp = await request.post(`/api/fulfillment/${physicalFulfillmentId}/deliver`)
    expect(deliverResp.ok(), '签收应成功').toBeTruthy()
  })

  await test.step('Then 订单详情显示已完成，事件回放包含 OrderCompleted', async () => {
    await expect(async () => {
      await orderDetail.goto(orderId)
      await orderDetail.assertOnOrderDetail()
      await orderDetail.assertJourneyStepReached('已完成')
    }).toPass({ timeout: 15000 })

    await orderDetail.assertTimelineContains('订单已完成')
    await orderDetail.assertServiceActivated()
    await orderDetail.assertItemCount(2)
  })
})
