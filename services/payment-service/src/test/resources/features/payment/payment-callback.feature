# language: zh-CN
@payment
功能: 支付网关回调（支付结果）
  网关回调成功/失败后更新支付单状态并发布领域事件；重复成功回调幂等。

  场景: 收到支付成功回调时应置为 COMPLETED 并发布 PaymentCompleted
    假如 已存在订单 "100" 的支付单 金额 "5000" 分
    并且 记录该支付单的 paymentId 为 "lastPaymentId"
    当 以支付成功 回调 paymentId "lastPaymentId"
    那么 应返回 200
    并且 应发布恰好 1 次 PaymentCompleted 事件 含 orderId "100" 与 paymentId "lastPaymentId"

  场景: 收到支付失败回调时应保持 PENDING 并发布 PaymentFailed
    假如 已存在订单 "200" 的支付单 金额 "100" 分
    并且 记录该支付单的 paymentId 为 "lastPaymentId"
    当 以支付失败 回调 paymentId "lastPaymentId"
    那么 应返回 200
    并且 应发布恰好 1 次 PaymentFailed 事件 含 orderId "200"
    并且 按 paymentId "lastPaymentId" 查询支付单 状态应为 "PENDING"

  场景: 多次支付失败回调应每次都发布 PaymentFailed 事件
    假如 已存在订单 "250" 的支付单 金额 "100" 分
    并且 记录该支付单的 paymentId 为 "lastPaymentId"
    当 以支付失败 回调 paymentId "lastPaymentId"
    当 以支付失败 回调 paymentId "lastPaymentId"
    那么 应返回 200
    并且 应发布恰好 2 次 PaymentFailed 事件 含 orderId "250"
    并且 按 paymentId "lastPaymentId" 查询支付单 状态应为 "PENDING"

  场景: 同一支付单重复成功回调应幂等
    假如 已存在订单 "300" 的支付单 金额 "200" 分
    并且 记录该支付单的 paymentId 为 "lastPaymentId"
    当 以支付成功 回调 paymentId "lastPaymentId"
    并且 再次以支付成功 回调 paymentId "lastPaymentId"
    那么 应返回 200
    并且 应发布恰好 1 次 PaymentCompleted 事件 含 orderId "300"

  场景: 回调不存在的 paymentId 应返回 404
    当 以支付成功 回调 paymentId "999999"
    那么 应返回 404
