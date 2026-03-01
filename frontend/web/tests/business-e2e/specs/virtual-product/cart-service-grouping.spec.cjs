/**
 * BIZ-VP-002 购物车含服务：加购实体+服务 → 购物车可见 → 结算 → 提交订单成功
 *
 * 对应业务需求：虚拟商品迭代二 - Cart 展示可选服务与结算
 */
const { test, expect } = require('../../../shared/fixtures/auth.fixture.cjs')
const { ProductDetailPage } = require('../../../shared/pages/product-detail.page.cjs')
const { CartPage } = require('../../../shared/pages/cart.page.cjs')
const { CheckoutPage } = require('../../../shared/pages/checkout.page.cjs')
const { findProductWithServiceBindings } = require('../../../shared/helpers/catalog.helper.cjs')

const PRODUCT_ID = Number(process.env.SMOKE_VIRTUAL_BINDING_PRODUCT_ID || 1)

test('BIZ-VP-002 购物车含服务: 加购 → 购物车可见 → 结算成功', async ({ authedPage, request }) => {
  const { page } = authedPage
  const product = new ProductDetailPage(page)
  const cart = new CartPage(page)
  const checkout = new CheckoutPage(page)
  let productId = null

  await test.step('Given 存在可绑定服务的实体商品', async () => {
    productId = await findProductWithServiceBindings(request, PRODUCT_ID)
    expect(productId, '未找到可选服务绑定的商品').toBeTruthy()
  })

  await test.step('When 选择规格和服务后加入购物车', async () => {
    await product.goto(productId)
    await product.assertLoaded()
    await product.selectRequiredSpecsIfPresent()
    await product.selectFirstServiceOptionIfPresent()
    const addToCartBtn = page.getByRole('button', { name: '加入购物车' })
    await expect(addToCartBtn).toBeEnabled()
    await addToCartBtn.click()
    await expect(page.getByText('已添加到购物车')).toBeVisible()
  })

  await test.step('Then 购物车中可见服务项', async () => {
    await cart.goto()
    await cart.assertLoaded()
    await cart.assertHasItems(2)
  })

  await test.step('When 全选并结算提交订单', async () => {
    await cart.selectAll()
    await cart.goCheckout()
    await checkout.assertLoaded()
    await checkout.assertSummaryItemCountAtLeast(2)
    await checkout.fillAddress()
    await checkout.submitOrder()
    await checkout.assertRedirectToOrderDetail()
  })
})
