const { test, expect } = require('../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../shared/pages/product-detail.page.cjs')
const { CheckoutPage } = require('../../shared/pages/checkout.page.cjs')

const PRODUCT_ID = Number(process.env.SMOKE_PRODUCT_ID || 1)

test('SMOKE-E2E-001 @P0 主链路: 详情 -> 立即购买 -> 提交订单成功', async ({ authedPage }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const checkout = new CheckoutPage(page)

  await test.step('Given 已登录用户打开商品详情页', async () => {
    await product.goto(PRODUCT_ID)
    await product.assertLoaded()
  })

  await test.step('When 用户选择必选规格并点击立即购买', async () => {
    await product.selectRequiredSpecsIfPresent()
    await expect(product.buyNowButton).toBeEnabled()
    await product.buyNow()
  })

  await test.step('Then 进入结账页并成功提交订单', async () => {
    await checkout.assertLoaded()
    await checkout.fillAddress()
    await checkout.submitOrder()
    await checkout.assertRedirectToOrderDetail()
  })
})
