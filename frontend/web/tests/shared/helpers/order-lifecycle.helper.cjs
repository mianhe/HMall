/**
 * 通过 API 将一笔订单从 PENDING_PAYMENT 推进到 DELIVERED。
 * 用于 Business E2E 的前置数据准备。
 */

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function pollOrderStatus(request, orderId, targetStatus, maxRetries = 15) {
  for (let i = 0; i < maxRetries; i++) {
    const resp = await request.get(`/api/orders/${orderId}`)
    const order = await resp.json()
    if (order.status === targetStatus) return order
    await sleep(1000)
  }
  throw new Error(`Order ${orderId} did not reach ${targetStatus} within timeout`)
}

/**
 * 创建一笔实体商品订单并推进到 DELIVERED 状态。
 * @returns {{ orderId: number }} 已交付的订单
 */
async function createDeliveredOrder(request, userId, skuId) {
  const orderResp = await request.post('/api/orders', {
    data: {
      userId,
      items: [{ skuId, quantity: 1 }],
      shippingAddress: {
        recipientName: 'E2E测试',
        phone: '13800000000',
        province: '北京',
        city: '北京',
        district: '海淀',
        detail: 'E2E测试地址',
      },
    },
  })
  if (!orderResp.ok()) {
    throw new Error(`Create order failed: ${orderResp.status()} ${await orderResp.text()}`)
  }
  const order = await orderResp.json()
  const orderId = order.orderId

  const paymentResp = await request.get(`/api/payments/by-order/${orderId}`)
  if (!paymentResp.ok()) {
    throw new Error(`Get payment failed: ${paymentResp.status()}`)
  }
  const payment = await paymentResp.json()

  const callbackResp = await request.post('/api/payments/callback', {
    data: { paymentId: payment.paymentId, success: true },
  })
  if (!callbackResp.ok()) {
    throw new Error(`Payment callback failed: ${callbackResp.status()}`)
  }

  await pollOrderStatus(request, orderId, 'FULFILLING')

  const ffListResp = await request.get(`/api/fulfillment?orderId=${orderId}`)
  const ffList = await ffListResp.json()
  if (!Array.isArray(ffList) || ffList.length === 0) {
    throw new Error('No fulfillment orders found')
  }

  for (const ff of ffList) {
    if (ff.status === 'CREATED' || ff.status === 'ALLOCATING') {
      await request.post(`/api/fulfillment/${ff.fulfillmentOrderId}/ship`, {
        data: { carrier: 'E2E快递', trackingNumber: `E2E${Date.now()}` },
      })
    }
  }

  const ffListAfterShip = await request.get(`/api/fulfillment?orderId=${orderId}`)
  const ffAfterShip = await ffListAfterShip.json()
  for (const ff of ffAfterShip) {
    if (ff.status === 'SHIPPED') {
      await request.post(`/api/fulfillment/${ff.fulfillmentOrderId}/deliver`)
    }
  }

  const deliveredOrder = await pollOrderStatus(request, orderId, 'DELIVERED')
  return deliveredOrder
}

module.exports = { createDeliveredOrder, pollOrderStatus }
