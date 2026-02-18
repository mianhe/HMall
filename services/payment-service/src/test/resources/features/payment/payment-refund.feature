# language: zh-CN
@payment
功能: 退款
  Order 调用退款接口（orderId）；仅已支付可退，幂等。

  场景: 已支付的订单退款应成功并置为 REFUNDED
    假如 已存在订单 "100" 的支付单 金额 "5000" 分
    并且 以支付成功 回调 paymentId "lastPaymentId"
    当 以 orderId "100" 调用退款接口
    那么 应返回 200
    并且 按 orderId "100" 查询支付单 状态应为 "REFUNDED"

  场景: 同一 orderId 重复退款应幂等
    假如 已存在订单 "200" 的支付单 金额 "100" 分
    并且 以支付成功 回调 paymentId "lastPaymentId"
    当 以 orderId "200" 调用退款接口
    并且 再次以 orderId "200" 调用退款接口
    那么 应返回 200

  场景: 未支付的订单调用退款应返回 400
    假如 已存在订单 "300" 的支付单 金额 "200" 分
    当 以 orderId "300" 调用退款接口
    那么 应返回 400

  场景: 不存在的 orderId 退款应返回 404
    当 以 orderId "999999" 调用退款接口
    那么 应返回 404

  场景: 缺少 orderId 的退款请求应返回 400
    当 以缺少 orderId 的请求 调用退款接口
    那么 应返回 400
