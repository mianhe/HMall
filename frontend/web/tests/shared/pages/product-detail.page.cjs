const { expect } = require('@playwright/test')

class ProductDetailPage {
  constructor(page) {
    this.page = page
    this.buyNowButton = page.getByRole('button', { name: '立即购买' })
  }

  async goto(productId) {
    await this.page.goto(`/products/${productId}`, { waitUntil: 'domcontentloaded' })
  }

  async assertLoaded() {
    await expect(this.page.locator('h1')).toBeVisible()
    await expect(this.buyNowButton).toBeVisible()
  }

  async selectRequiredSpecsIfPresent() {
    const requiredDims = this.page.locator('div.mb-5:has(span:text("*"))')
    const requiredCount = await requiredDims.count()
    for (let i = 0; i < requiredCount; i++) {
      const firstOption = requiredDims.nth(i).locator('button').first()
      await firstOption.click()
    }
  }

  async selectFirstServiceOptionIfPresent() {
    const serviceSection = this.page.locator('div.mt-6', { hasText: '增值服务' })
    if (await serviceSection.count() === 0) return false
    const firstButton = serviceSection.locator('button').first()
    await firstButton.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {})
    if (await firstButton.count() === 0) return false
    await firstButton.click()
    return true
  }

  /** 选镭雕服务（点击名称含「镭雕」的增值服务按钮），供 BIZ-LE-005 等镭雕流程使用 */
  async selectEngravingServiceOptionIfPresent() {
    const serviceSection = this.page.locator('div.mt-6', { hasText: '增值服务' })
    if (await serviceSection.count() === 0) return false
    const engravingBtn = serviceSection.locator('button').filter({ hasText: '镭雕' })
    await engravingBtn.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {})
    if (await engravingBtn.count() === 0) return false
    await engravingBtn.click()
    return true
  }

  /** 选镭雕服务并选第一个图案（当镭雕内容区域出现时） */
  async selectEngravingAndPatternIfPresent() {
    const engravingSection = this.page.locator('div.rounded-lg.border').filter({ hasText: '镭雕内容' }).filter({ hasText: '选择图案' })
    if (await engravingSection.count() === 0) return false
    await engravingSection.first().waitFor({ state: 'visible', timeout: 5000 })
    await this.page.waitForTimeout(1500)
    const section = engravingSection.first()
    const patternBtn = section.locator('button').filter({ has: this.page.locator('img') }).first()
    await patternBtn.waitFor({ state: 'visible', timeout: 8000 }).catch(() => {})
    if (await patternBtn.count() > 0 && await patternBtn.isVisible()) {
      await patternBtn.click()
      return true
    }
    const textInput = section.getByPlaceholder('请输入雕刻文字')
    if (await textInput.count() > 0) {
      await textInput.fill('E2E测试')
      return true
    }
    return false
  }

  async buyNow() {
    await this.buyNowButton.click()
  }

  async addToCart() {
    await this.page.getByRole('button', { name: '加入购物车' }).click()
  }
}

module.exports = { ProductDetailPage }
