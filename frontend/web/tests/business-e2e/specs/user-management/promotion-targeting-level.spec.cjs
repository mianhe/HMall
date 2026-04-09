const { test, expect } = require('@playwright/test')

function uniqueName(prefix) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000)}`
}

function isoWithOffset(minutesFromNow) {
  return new Date(Date.now() + minutesFromNow * 60 * 1000).toISOString()
}

test('BIZ-UM-001 调整用户等级后定向活动命中应实时变化', async ({ request }) => {
  const username = uniqueName('um-level')
  const createUserRes = await request.post('http://127.0.0.1:8085/api/users', {
    data: { username, password: 'secret123' },
  })
  expect(createUserRes.ok()).toBeTruthy()
  const createdUser = await createUserRes.json()
  const userId = createdUser.id
  const skuId = 990000 + Math.floor(Math.random() * 1000)

  const createActivityRes = await request.post('http://127.0.0.1:8085/api/promotion/activities', {
    data: {
      name: uniqueName('um-l2-activity'),
      type: 'SKU_AMOUNT_OFF',
      targetSkuIds: [skuId],
      discountCents: 500,
      mutexGroupCode: null,
      priority: 0,
      startAt: isoWithOffset(-30),
      endAt: isoWithOffset(120),
      targetingRule: {
        levelsIn: ['L2'],
        tagsAny: [],
        tagsAll: [],
        excludeTags: [],
      },
    },
  })
  expect(createActivityRes.ok()).toBeTruthy()
  const activity = await createActivityRes.json()

  const activateRes = await request.post(`http://127.0.0.1:8085/api/promotion/activities/${activity.id}/activate`)
  expect(activateRes.ok()).toBeTruthy()

  const beforeRes = await request.post('http://127.0.0.1:8085/api/promotion/calculate-price', {
    data: {
      userId,
      couponId: null,
      items: [{ skuId, quantity: 1, unitPriceCents: 10000 }],
    },
  })
  expect(beforeRes.ok()).toBeTruthy()
  const beforeBody = await beforeRes.json()
  expect(beforeBody.activityDiscountAmountCents).toBe(0)

  const updateLevelRes = await request.put(`http://127.0.0.1:8085/api/users/${userId}/level`, {
    data: { level: 'L2' },
  })
  expect(updateLevelRes.ok()).toBeTruthy()

  const afterRes = await request.post('http://127.0.0.1:8085/api/promotion/calculate-price', {
    data: {
      userId,
      couponId: null,
      items: [{ skuId, quantity: 1, unitPriceCents: 10000 }],
    },
  })
  expect(afterRes.ok()).toBeTruthy()
  const afterBody = await afterRes.json()
  expect(afterBody.activityDiscountAmountCents).toBe(500)
})
