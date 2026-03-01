const { test, expect } = require('../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../shared/pages/product-detail.page.cjs')
const { CartPage } = require('../../shared/pages/cart.page.cjs')
const { CheckoutPage } = require('../../shared/pages/checkout.page.cjs')

const PRODUCT_ID = Number(process.env.SMOKE_PRODUCT_ID || 1)

test('SMOKE-E2E-003 @P0 购物车结算: 加购 -> 购物车 -> 结算 -> 提交订单成功', async ({ authedPage }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const cart = new CartPage(page)
  const checkout = new CheckoutPage(page)

  await test.step('Given 已登录用户打开商品详情页', async () => {
    await product.goto(PRODUCT_ID)
    await product.assertLoaded()
  })

  await test.step('When 选择规格后加入购物车', async () => {
    await product.selectRequiredSpecsIfPresent()
    const addToCartBtn = page.getByRole('button', { name: '加入购物车' })
    await expect(addToCartBtn).toBeEnabled()
    await addToCartBtn.click()
    await expect(page.getByText('已添加到购物车')).toBeVisible()
  })

  await test.step('And 进入购物车全选并结算', async () => {
    await cart.goto()
    await cart.assertLoaded()
    await cart.selectAll()
    await cart.goCheckout()
  })

  await test.step('Then 进入结账页并成功提交订单', async () => {
    await checkout.assertLoaded()
    await checkout.fillAddress()
    await checkout.submitOrder()
    await checkout.assertRedirectToOrderDetail()
  })
})
