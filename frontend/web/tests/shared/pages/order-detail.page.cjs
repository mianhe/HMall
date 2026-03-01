const { expect } = require('@playwright/test')

class OrderDetailPage {
  constructor(page) {
    this.page = page
  }

  async goto(orderId) {
    await this.page.goto(`/orders/${orderId}`, { waitUntil: 'domcontentloaded' })
  }

  async assertOnOrderDetail() {
    await expect(this.page).toHaveURL(/\/orders\/\d+/)
    await expect(this.page.getByText('订单号：')).toBeVisible()
  }

  async assertItemCount(minCount) {
    const section = this.page.getByText('商品明细').locator('..')
    const items = section.locator('div.flex.gap-4')
    await expect(async () => {
      const count = await items.count()
      expect(count).toBeGreaterThanOrEqual(minCount)
    }).toPass()
  }

  async assertJourneyStepReached(label) {
    const step = this.page.locator('p.text-xs.font-medium', { hasText: label })
    await expect(step).toHaveClass(/text-gray-900/)
  }

  async assertTimelineContains(description) {
    await expect(this.page.getByText(description)).toBeVisible()
  }

  async assertServiceActivated() {
    await expect(this.page.getByText('已激活', { exact: true })).toBeVisible()
  }
}

module.exports = { OrderDetailPage }
