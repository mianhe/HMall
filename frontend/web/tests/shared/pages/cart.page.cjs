const { expect } = require('@playwright/test')

class CartPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/cart', { waitUntil: 'domcontentloaded' })
  }

  async assertLoaded() {
    await expect(this.page).toHaveURL(/\/cart/)
  }

  async assertHasItems(minCount) {
    const checkboxes = this.page.locator('input[type="checkbox"]').filter({ hasNot: this.page.getByText('全选') })
    await expect(async () => {
      const count = await checkboxes.count()
      expect(count).toBeGreaterThanOrEqual(minCount)
    }).toPass()
  }

  async selectAll() {
    const selectAllCheckbox = this.page.getByText('全选').locator('..')
    await selectAllCheckbox.locator('input[type="checkbox"]').check()
  }

  async goCheckout() {
    await this.page.getByRole('button', { name: '去结算' }).click()
  }
}

module.exports = { CartPage }
