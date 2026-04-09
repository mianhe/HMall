const { test, expect } = require('@playwright/test')

function uniqueName(prefix) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000)}`
}

test('BIZ-UM-002 标签更新后圈选预览结果应实时变化', async ({ page, request }) => {
  const username = uniqueName('um-tags')
  const createRes = await request.post('http://127.0.0.1:8085/api/users', {
    data: { username, password: 'secret123' },
  })
  expect(createRes.ok()).toBeTruthy()
  const createdUser = await createRes.json()
  const userId = createdUser.id
  const ruleName = uniqueName('vip-rule')

  await page.goto('/user-segmentation')
  await page.getByTestId('um-user-id-input').fill(String(userId))
  await page.getByTestId('um-load-user').click()
  await page.getByTestId('um-tags-input').fill('VIP')
  await page.getByTestId('um-save-tags').click()

  await page.getByTestId('um-rule-name').fill(ruleName)
  await page.locator('input[placeholder="tagsAny: VIP"]').fill('VIP')
  await page.getByTestId('um-create-rule').click()
  await page.getByTestId('um-preview-rule').click()
  await expect(page.getByTestId('um-preview-result')).toContainText('命中人数：1')

  await page.getByTestId('um-tags-input').fill('')
  await page.getByTestId('um-save-tags').click()
  await page.getByTestId('um-preview-rule').click()
  await expect(page.getByTestId('um-preview-result')).toContainText('命中人数：0')
})

test('BIZ-UM-003 规则无命中时激活应失败并给出原因', async ({ page, request }) => {
  const username = uniqueName('um-activate')
  const createRes = await request.post('http://127.0.0.1:8085/api/users', {
    data: { username, password: 'secret123' },
  })
  expect(createRes.ok()).toBeTruthy()
  const createdUser = await createRes.json()
  const userId = createdUser.id
  const ruleName = uniqueName('l3-only')

  await page.goto('/user-segmentation')
  await page.getByTestId('um-user-id-input').fill(String(userId))
  await page.getByTestId('um-load-user').click()

  await page.getByTestId('um-rule-name').fill(ruleName)
  await page.locator('input[placeholder="levelsIn: L2,L3"]').fill('L3')
  await page.getByTestId('um-create-rule').click()
  await page.getByTestId('um-preview-rule').click()
  await expect(page.getByTestId('um-preview-result')).toContainText('命中人数：0')

  await page.getByTestId('um-activate-rule').click()
  await expect(page.getByText('命中人数为 0，规则不可激活')).toBeVisible()
})
