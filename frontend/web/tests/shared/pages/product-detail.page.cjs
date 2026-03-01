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
    const serviceSection = this.page.locator('div.mt-6', { hasText: '保障服务' })
    if (await serviceSection.count() === 0) return false
    const firstButton = serviceSection.locator('button').first()
    await firstButton.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {})
    if (await firstButton.count() === 0) return false
    await firstButton.click()
    return true
  }

  async buyNow() {
    await this.buyNowButton.click()
  }
}

module.exports = { ProductDetailPage }
