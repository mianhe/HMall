const { expect } = require('@playwright/test')

class CheckoutPage {
  constructor(page) {
    this.page = page
    this.submitButton = page.getByRole('button', { name: '提交订单' })
  }

  async assertLoaded() {
    await expect(this.page).toHaveURL(/\/checkout/)
    await expect(this.page.getByText('商品信息')).toBeVisible()
    await expect(this.submitButton).toBeVisible()
  }

  async assertSummaryItemCountAtLeast(count) {
    const rows = this.page.locator('p.text-vmall-red.font-medium.mt-1')
    await expect(async () => {
      const actual = await rows.count()
      expect(actual).toBeGreaterThanOrEqual(count)
    }).toPass()
  }

  async fillAddress() {
    await this.page.getByPlaceholder('请输入收件人姓名').fill('冒烟测试用户')
    await this.page.getByPlaceholder('请输入手机号').fill('13800138000')
    await this.page.getByPlaceholder('如：广东省').fill('广东省')
    await this.page.getByPlaceholder('如：深圳市').fill('深圳市')
    await this.page.getByPlaceholder('如：南山区').fill('南山区')
    await this.page.getByPlaceholder('如：科技园路 1 号').fill('科技园 1 号')
  }

  async submitOrder() {
    await this.submitButton.click()
  }

  async assertRedirectToOrderDetail() {
    await expect(this.page).toHaveURL(/\/orders\/\d+/)
    await expect(this.page.getByText('订单号：')).toBeVisible()
  }

  /** 断言结账页镭雕输入区可见（镭雕服务时必现） */
  async assertEngravingFormVisible() {
    await expect(this.page.getByText('镭雕内容')).toBeVisible()
    await expect(this.page.getByPlaceholder('请输入雕刻文字')).toBeVisible()
  }

  /** 填写镭雕：选第一个图案（若有）或填文字 */
  async fillEngravingContent() {
    const patternBtn = this.page.locator('button').filter({ has: this.page.locator('img') }).first()
    if (await patternBtn.count() > 0 && await patternBtn.isVisible()) {
      await patternBtn.click()
    } else {
      await this.page.getByPlaceholder('请输入雕刻文字').fill('E2E测试镭雕')
    }
  }
}

module.exports = { CheckoutPage }
