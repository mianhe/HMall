/**
 * BIZ-LE-006 镭雕购物车结算：详情选镭雕→加入购物车→去结算→结账页出现镭雕输入区→填写并提交
 *
 * 验收：购物车结算时，结账页应展示镭雕输入区供用户填写
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../../shared/pages/product-detail.page.cjs')
const { CartPage } = require('../../../shared/pages/cart.page.cjs')
const { CheckoutPage } = require('../../../shared/pages/checkout.page.cjs')
const { findProductWithEngravingService } = require('../../../shared/helpers/catalog.helper.cjs')

test('BIZ-LE-006 镭雕购物车结算: 结账页应出现镭雕输入区', async ({ authedPage, request }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const cart = new CartPage(page)
  const checkout = new CheckoutPage(page)
  let productId = null

  await test.step('Given 存在绑定镭雕服务的实体商品', async () => {
    productId = await findProductWithEngravingService(request, 1)
    if (!productId) {
      test.skip(true, '未找到镭雕服务商品')
      return
    }
  })

  if (!productId) return

  await test.step('When 详情页选镭雕、图案后加入购物车', async () => {
    await product.goto(productId)
    await product.assertLoaded()
    await product.selectRequiredSpecsIfPresent()
    await product.selectEngravingServiceOptionIfPresent()
    const engravingSelected = await product.selectEngravingAndPatternIfPresent()
    if (!engravingSelected) {
      test.skip(true, '当前选中的非镭雕服务或无镭雕输入区')
      return
    }
    await product.addToCart()
    await page.waitForTimeout(500)
  })

  await test.step('When 进入购物车去结算', async () => {
    await cart.goto()
    await cart.assertLoaded()
    await cart.selectAll()
    await cart.goCheckout()
  })

  await test.step('Then 结账页出现镭雕输入区', async () => {
    await checkout.assertLoaded()
    await checkout.assertEngravingFormVisible()
  })

  await test.step('When 填写镭雕内容并提交订单', async () => {
    await checkout.fillEngravingContent()
    await checkout.fillAddress()
    await checkout.submitOrder()
  })

  await test.step('Then 跳转到订单详情', async () => {
    await checkout.assertRedirectToOrderDetail()
  })
})
