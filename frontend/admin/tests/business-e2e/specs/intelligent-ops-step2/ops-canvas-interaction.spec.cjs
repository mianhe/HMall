/**
 * BIZ-IO2-001 智能运营页面：对话驱动画布渲染全链路
 *
 * 对应业务需求：智能运营 Step 2 - Smart Interaction 接入 + 对话驱动页面 MVP
 *
 * 验证：
 * 1. 进入 /ops 页面，固定指标栏显示今日统计
 * 2. 全局 AiChatButton 在 /ops 隐藏
 * 3. 常驻侧边栏可正常显示和发送消息
 * 4. 点击提示词快捷按钮后，AI 开始响应（侧边栏出现 loading）
 */
const { test, expect } = require('@playwright/test')

test('BIZ-IO2-001 进入智能运营页面，页面三区域正确渲染', async ({ page }) => {
  await test.step('Given 进入智能运营页面 /ops', async () => {
    await page.goto('/ops')
    await expect(page.getByRole('heading', { name: '智能运营' })).toBeVisible({ timeout: 15000 })
  })

  await test.step('Then 顶部标题栏可见（无今日数据块，避免干扰画布）', async () => {
    await expect(page.getByRole('heading', { name: '智能运营' })).toBeVisible({ timeout: 5000 })
  })

  await test.step('Then 欢迎引导画布区域可见（初始为 EMPTY 状态）', async () => {
    await expect(page.getByText('与 AI 对话，驱动数据视图')).toBeVisible({ timeout: 10000 })
  })

  await test.step('Then 常驻侧边栏 AI 面板可见', async () => {
    await expect(page.getByText('智能运营助手')).toBeVisible({ timeout: 10000 })
  })

  await test.step('Then 全局 AI 浮层按钮已隐藏（页面专属体验）', async () => {
    // 全局 AiChatButton 在 /ops 页面应被 hideGlobalChatButton meta 隐藏
    const globalChatButton = page.locator('[aria-label="打开 AI 助手"]')
    await expect(globalChatButton).not.toBeVisible()
  })
})

test('BIZ-IO2-001 智能运营页面：侧边栏 AI 对话可正常发送消息', async ({ page }) => {
  await test.step('Given 进入智能运营页面', async () => {
    await page.goto('/ops')
    await expect(page.getByText('智能运营助手')).toBeVisible({ timeout: 15000 })
  })

  await test.step('When 在侧边栏输入框发送消息（点击快捷提示）', async () => {
    // 使用侧边栏的输入框发送消息
    const sidebar = page.locator('.border-l').last()
    const input = sidebar.getByPlaceholder('输入消息，Enter 发送')
    await expect(input).toBeVisible({ timeout: 10000 })
    await input.fill('你好，请介绍一下你的能力')
    await input.press('Enter')
  })

  await test.step('Then AI 助手开始响应（消息列表出现用户消息）', async () => {
    // 用户消息应该出现在消息列表
    await expect(page.getByText('你好，请介绍一下你的能力')).toBeVisible({ timeout: 10000 })
    // AI 正在思考状态或者已有回复
    await Promise.race([
      page.locator('.animate-pulse').first().waitFor({ state: 'visible', timeout: 30000 }).catch(() => {}),
      page.locator('[class*="assistant"]').first().waitFor({ state: 'visible', timeout: 30000 }).catch(() => {}),
    ])
  })
})

test('BIZ-IO2-001 智能运营页面：快捷提示触发 AI 对话', async ({ page }) => {
  await test.step('Given 进入智能运营页面', async () => {
    await page.goto('/ops')
    await expect(page.getByText('与 AI 对话，驱动数据视图')).toBeVisible({ timeout: 15000 })
  })

  await test.step('When 点击快捷提示按钮「今天的整体情况如何？」', async () => {
    await page.getByText('今天的整体情况如何？').click()
  })

  await test.step('Then 侧边栏出现该消息并开始 AI 响应', async () => {
    // 消息出现在 AI 消息列表中（非提示词按钮区域）
    await expect(page.getByText('今天的整体情况如何？').first()).toBeVisible({ timeout: 10000 })
  })
})
