/**
 * BIZ-LE-001 镭雕图案库：Admin 可配置图案，列表展示、新增、编辑、删除
 *
 * 对应业务需求：镭雕需求迭代 0 - 图案库（Catalog）
 */
const { test, expect } = require('@playwright/test')
const { EngravingPatternPage } = require('../../pages/engraving-pattern.page.cjs')

const PLACEHOLDER_IMAGE = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='

function uniqueName() {
  return `图案_E2E_${Date.now()}`
}

test('BIZ-LE-001 图案库: 进入页面 -> 新增图案 -> 列表展示', async ({ page }) => {
  const pattern = new EngravingPatternPage(page)
  const name = uniqueName()

  await test.step('Given 进入镭雕图案库页', async () => {
    await pattern.goto()
    await pattern.assertLoaded()
  })

  await test.step('When 点击新增图案并填写表单保存', async () => {
    await pattern.clickAdd()
    await pattern.fillForm({
      name,
      imageUrl: PLACEHOLDER_IMAGE,
      sortOrder: 0,
      enabled: true,
    })
    await pattern.submitForm()
  })

  await test.step('Then 列表中展示新图案', async () => {
    await pattern.assertPatternInTable(name)
  })
})

test('BIZ-LE-002 图案库: 编辑图案 -> 列表更新', async ({ page }) => {
  const pattern = new EngravingPatternPage(page)
  const name = uniqueName()
  const newName = uniqueName()

  await test.step('Given 已存在一个图案', async () => {
    await pattern.goto()
    await pattern.assertLoaded()
    await pattern.clickAdd()
    await pattern.fillForm({
      name,
      imageUrl: PLACEHOLDER_IMAGE,
      sortOrder: 0,
      enabled: true,
    })
    await pattern.submitForm()
    await pattern.assertPatternInTable(name)
  })

  await test.step('When 编辑该图案并修改名称保存', async () => {
    await pattern.clickEdit(name)
    await pattern.fillForm({
      name: newName,
      imageUrl: PLACEHOLDER_IMAGE,
      sortOrder: 0,
      enabled: true,
    })
    await pattern.submitForm()
  })

  await test.step('Then 列表展示新名称，旧名称消失', async () => {
    await pattern.assertPatternInTable(newName)
    await pattern.assertPatternNotInTable(name)
  })
})

test('BIZ-LE-003 图案库: 删除图案 -> 列表移除', async ({ page }) => {
  const pattern = new EngravingPatternPage(page)
  const name = uniqueName()

  await test.step('Given 已存在一个图案', async () => {
    await pattern.goto()
    await pattern.assertLoaded()
    await pattern.clickAdd()
    await pattern.fillForm({
      name,
      imageUrl: PLACEHOLDER_IMAGE,
      sortOrder: 0,
      enabled: true,
    })
    await pattern.submitForm()
    await pattern.assertPatternInTable(name)
  })

  await test.step('When 删除该图案并确认', async () => {
    await pattern.deleteAndConfirm(name)
  })

  await test.step('Then 列表中不再展示该图案', async () => {
    await pattern.assertPatternNotInTable(name)
  })
})
