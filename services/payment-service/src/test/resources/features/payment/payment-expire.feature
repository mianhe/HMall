# language: zh-CN
@payment
功能: 超时检测
  未在超时时长内支付的 PENDING 支付单应被置为 EXPIRED 并发布 PaymentExpired；已终态不参与检测。

  场景: 已过期的 PENDING 支付单执行超时检测后应置为 EXPIRED 并发布 PaymentExpired
    假如 已存在订单 "100" 的支付单 金额 "100" 分
    并且 记录该支付单的 paymentId 为 "lastPaymentId"
    当 执行超时检测
    那么 应发布恰好 1 次 PaymentExpired 事件 含 orderId "100"
    并且 按 paymentId "lastPaymentId" 查询支付单 状态应为 "EXPIRED"

  场景: 已 COMPLETED 的支付单不参与超时检测
    假如 已存在订单 "200" 的支付单 金额 "200" 分
    并且 以支付成功 回调 paymentId "lastPaymentId"
    当 执行超时检测
    那么 应发布恰好 0 次 PaymentExpired 事件

  场景: 超时时长未配置时默认 30 分钟
    假如 尚无订单 "300" 的支付单
    当 以订单 "300" 金额 "500" 分 调用创建支付接口
    那么 应返回 201
    并且 响应中 status 为 "PENDING"
