/**
 * 将含镭雕的订单从 PENDING_PAYMENT 推进到 DELIVERED。
 * 流程：支付回调 → 对含镭雕的 PHYSICAL 履约单执行完成镭雕 → 发货 → 签收
 */

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function pollOrderStatus(request, orderId, targetStatuses, maxRetries = 20) {
  for (let i = 0; i < maxRetries; i++) {
    const resp = await request.get(`/api/orders/${orderId}`)
    const order = await resp.json()
    if (Array.isArray(targetStatuses) ? targetStatuses.includes(order.status) : order.status === targetStatuses) {
      return order
    }
    await sleep(1000)
  }
  throw new Error(`Order ${orderId} did not reach ${targetStatuses} within timeout`)
}

async function advanceEngravingOrderToDelivered(request, orderId, userId) {
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

  await pollOrderStatus(request, orderId, ['PAID', 'FULFILLING'], 15)
  await sleep(1500)

  const ffListResp = await request.get(`/api/fulfillment?orderId=${orderId}`)
  const ffList = await ffListResp.json()
  if (!Array.isArray(ffList) || ffList.length === 0) {
    throw new Error('No fulfillment orders found')
  }

  for (const ff of ffList) {
    if (ff.fulfillmentType !== 'PHYSICAL') continue
    if (ff.status === 'CREATED' || ff.status === 'ALLOCATING') {
      if (ff.engravingInfo && !ff.engravingCompletedAt) {
        await request.post(`/api/fulfillment/${ff.fulfillmentOrderId}/complete-engraving`)
      }
      await request.post(`/api/fulfillment/${ff.fulfillmentOrderId}/ship`, {
        data: { carrier: 'E2E快递', trackingNumber: `E2E${Date.now()}` },
      })
    }
  }

  await sleep(500)
  const ffListAfter = await request.get(`/api/fulfillment?orderId=${orderId}`)
  const ffAfter = await ffListAfter.json()
  for (const ff of ffAfter) {
    if (ff.status === 'SHIPPED') {
      await request.post(`/api/fulfillment/${ff.fulfillmentOrderId}/deliver`)
    }
  }

  const order = await pollOrderStatus(request, orderId, ['DELIVERED', 'COMPLETED'], 15)
  return order
}

module.exports = { advanceEngravingOrderToDelivered, pollOrderStatus }
