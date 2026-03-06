const { expect } = require('@playwright/test')

class EngravingPatternPage {
  constructor(page) {
    this.page = page
    this.addButton = page.getByRole('button', { name: '新增图案' })
    this.refreshButton = page.getByRole('button', { name: '刷新' })
    this.saveButton = page.getByRole('button', { name: '保存' })
    this.cancelButton = page.getByRole('button', { name: '取消' })
  }

  async goto() {
    await this.page.goto('/engraving-patterns', { waitUntil: 'domcontentloaded' })
  }

  async assertLoaded() {
    await expect(this.page.getByRole('heading', { name: '镭雕图案库' })).toBeVisible()
    await expect(this.addButton).toBeVisible()
  }

  async clickAdd() {
    await this.addButton.click()
    await this.page.getByRole('heading', { name: '新增图案' }).waitFor({ state: 'visible' })
  }

  async fillForm({ name, imageUrl, sortOrder, enabled = true }) {
    const form = this.page.locator('form')
    const nameInput = form.getByPlaceholder('图案名称')
    await nameInput.click()
    await nameInput.press('Control+a')
    await nameInput.pressSequentially(name)
    await form.getByPlaceholder('上传或输入图片 URL').fill(imageUrl || '')
    if (sortOrder != null) {
      await form.getByPlaceholder('数字越小越靠前').fill(String(sortOrder))
    }
    const enabledCheckbox = form.getByRole('checkbox', { name: '启用' })
    if (enabled) {
      await enabledCheckbox.check()
    } else {
      await enabledCheckbox.uncheck()
    }
  }

  async submitForm() {
    await this.page.locator('form').getByRole('button', { name: '保存' }).click()
  }

  async cancelForm() {
    await this.cancelButton.click()
  }

  async assertPatternInTable(name) {
    await expect(this.page.getByText(name, { exact: true })).toBeVisible()
  }

  async assertPatternNotInTable(name) {
    await expect(this.page.getByText(name, { exact: true })).not.toBeVisible()
  }

  async clickEdit(name) {
    const row = this.page.locator('tr', { has: this.page.getByText(name, { exact: true }) })
    await row.getByRole('button', { name: '编辑' }).click()
    await this.page.getByRole('heading', { name: '编辑图案' }).waitFor({ state: 'visible' })
  }

  async clickDelete(name) {
    const row = this.page.locator('tr', { has: this.page.getByText(name, { exact: true }) })
    await row.getByRole('button', { name: '删除' }).click()
  }

  async deleteAndConfirm(name) {
    this.page.once('dialog', (d) => d.accept())
    await this.clickDelete(name)
  }
}

module.exports = { EngravingPatternPage }
