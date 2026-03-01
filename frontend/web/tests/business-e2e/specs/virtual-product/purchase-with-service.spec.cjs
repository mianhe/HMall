/**
 * BIZ-VP-001 随购服务：详情页选服务 → 立即购买 → 结账含服务 → 提交订单 → 订单详情含服务明细
 *
 * 对应业务需求：虚拟商品迭代二 - 交易流程支持随购服务
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../../shared/pages/product-detail.page.cjs')
const { CheckoutPage } = require('../../../shared/pages/checkout.page.cjs')
const { OrderDetailPage } = require('../../../shared/pages/order-detail.page.cjs')
const { findProductWithServiceBindings } = require('../../../shared/helpers/catalog.helper.cjs')

const PRODUCT_ID = Number(process.env.SMOKE_VIRTUAL_BINDING_PRODUCT_ID || 1)

test('BIZ-VP-001 随购服务: 选服务 → 下单 → 订单含服务明细', async ({ authedPage, request }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const checkout = new CheckoutPage(page)
  const orderDetail = new OrderDetailPage(page)
  let productId = null

  await test.step('Given 存在可绑定服务的实体商品', async () => {
    productId = await findProductWithServiceBindings(request, PRODUCT_ID)
    expect(productId, '未找到可选服务绑定的商品，请检查测试数据').toBeTruthy()
  })

  await test.step('When 进入详情页选择规格与服务后立即购买', async () => {
    await product.goto(productId)
    await product.assertLoaded()
    await product.selectRequiredSpecsIfPresent()
    const selected = await product.selectFirstServiceOptionIfPresent()
    expect(selected, '详情页应有可选服务').toBeTruthy()
    await product.buyNow()
  })

  await test.step('Then 结账页含实体+服务明细并成功提交', async () => {
    await checkout.assertLoaded()
    await checkout.assertSummaryItemCountAtLeast(2)
    await checkout.fillAddress()
    await checkout.submitOrder()
  })

  await test.step('Then 订单详情含服务明细', async () => {
    await orderDetail.assertOnOrderDetail()
    await orderDetail.assertItemCount(2)
  })
})
