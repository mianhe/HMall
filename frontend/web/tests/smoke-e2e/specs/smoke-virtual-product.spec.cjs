const { test, expect } = require('../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../shared/pages/product-detail.page.cjs')
const { CheckoutPage } = require('../../shared/pages/checkout.page.cjs')
const { findProductWithServiceBindings } = require('../../shared/helpers/catalog.helper.cjs')

const PRODUCT_ID = Number(process.env.SMOKE_VIRTUAL_BINDING_PRODUCT_ID || 1)

test('SMOKE-E2E-002 @P1 虚拟商品交易: 实体+服务一起下单', async ({ authedPage, request }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const checkout = new CheckoutPage(page)
  let productId = null

  await test.step('Given 存在可绑定服务的实体商品并进入详情页', async () => {
    productId = await findProductWithServiceBindings(request, PRODUCT_ID)
    expect(productId, '未找到可选服务绑定的商品，请检查测试数据').toBeTruthy()
    await product.goto(productId)
    await product.assertLoaded()
  })

  await test.step('When 用户选择规格与服务并点击立即购买', async () => {
    await product.selectRequiredSpecsIfPresent()
    const selected = await product.selectFirstServiceOptionIfPresent()
    expect(selected).toBeTruthy()
    await expect(product.buyNowButton).toBeEnabled()
    await product.buyNow()
  })

  await test.step('Then 结账页出现实体+服务明细并成功提交订单', async () => {
    await checkout.assertLoaded()
    await checkout.assertSummaryItemCountAtLeast(2)
    await checkout.fillAddress()
    await checkout.submitOrder()
    await checkout.assertRedirectToOrderDetail()
  })
})
