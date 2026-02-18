# language: zh-CN
@payment
功能: 查询支付单
  按 paymentId 或 orderId 查询支付单，返回状态、金额、创建时间等。

  场景: 按 paymentId 查询应返回支付单详情
    假如 已存在订单 "100" 的支付单 金额 "8888" 分
    并且 记录该支付单的 paymentId 为 "lastPaymentId"
    当 按 paymentId "lastPaymentId" 查询支付单
    那么 应返回 200
    并且 响应包含 paymentId、orderId、amountCents "8888"、status "PENDING"、createdAt

  场景: 支付单不存在时返回 404
    当 按 paymentId "999999" 查询支付单
    那么 应返回 404
