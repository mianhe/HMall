const statusMap = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  FULFILLING: '履约中',
  SHIPPED: '已发货',
  DELIVERED: '已送达',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

export function statusText(status) {
  return statusMap[status] ?? status ?? ''
}

export function statusClass(status) {
  if (status === 'PENDING_PAYMENT') return 'bg-amber-100 text-amber-800'
  if (status === 'CANCELLED') return 'bg-gray-100 text-gray-600'
  if (['PAID', 'FULFILLING', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(status)) return 'bg-green-100 text-green-800'
  return 'bg-vmall-gray-bg text-vmall-gray-text'
}
