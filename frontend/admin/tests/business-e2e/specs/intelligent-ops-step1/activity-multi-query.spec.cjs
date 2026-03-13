/**
 * BIZ-IO1-001 活动监控多维查询：按 orderId / userId / spuId / skuId 查询活动
 *
 * 对应业务需求：智能运营 Step 1 - 多维事件基座
 */
const { test, expect } = require('@playwright/test')

test('BIZ-IO1-001 活动页多维查询: 输入 orderId 查询 -> 展示结果或空态', async ({ page }) => {
  await test.step('Given 进入活动监控页', async () => {
    await page.goto('/activity')
    await expect(page.getByRole('heading', { name: '活动监控' })).toBeVisible({ timeout: 15000 })
  })

  await test.step('When 在多维查询区输入 orderId 并点击查询', async () => {
    await page.getByTestId('activity-query-orderId').fill('1')
    await page.getByTestId('activity-query-submit').click()
  })

  await test.step('Then 请求成功且展示结果表格或无匹配提示', async () => {
    await Promise.race([
      page.locator('table tbody tr').first().waitFor({ state: 'visible', timeout: 15000 }).catch(() => {}),
      page.getByText('无匹配记录').waitFor({ state: 'visible', timeout: 15000 }).catch(() => {}),
    ])
    await expect(page.getByText('查询失败')).not.toBeVisible()
    const hasTable = (await page.locator('table tbody tr').count()) > 0
    const hasEmpty = await page.getByText('无匹配记录').isVisible().catch(() => false)
    expect(hasTable || hasEmpty).toBeTruthy()
  })
})
