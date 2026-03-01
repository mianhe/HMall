const { test: base } = require('@playwright/test')

function decodeUserIdFromToken(token) {
  try {
    const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString('utf8'))
    return payload.userId
  } catch {
    return null
  }
}

async function registerAndLogin(request, username, password) {
  await request.post('/api/users', {
    data: { username, password },
    failOnStatusCode: false,
  })
  const loginResp = await request.post('/api/login', {
    data: { username, password },
  })
  if (!loginResp.ok()) {
    throw new Error(`login failed: ${loginResp.status()} ${await loginResp.text()}`)
  }
  const loginData = await loginResp.json()
  if (!loginData || !loginData.token) {
    throw new Error('login response missing token')
  }
  return loginData.token
}

const test = base.extend({
  authedPage: async ({ page, request }, use) => {
    const seed = Date.now()
    const username = `smoke_u_${seed}`
    const password = 'Test@123456'
    const token = await registerAndLogin(request, username, password)
    const userId = decodeUserIdFromToken(token)

    await page.addInitScript((t) => {
      window.localStorage.setItem('token', t)
    }, token)
    await page.goto('/', { waitUntil: 'domcontentloaded' })

    await use({ page, token, userId, username })
  },
})

module.exports = { test, expect: test.expect }
