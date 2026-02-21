# language: zh-CN
@fulfillment
功能: 取消履约单
  作为 Order 服务，在订单取消补偿时调用取消接口，以便取消未发货的履约单。

  场景: CREATED 状态的履约单取消应成功且状态变为 CANCELLED
    假如 已存在 CREATED 状态的履约单 orderId 9001
    当 Order 调用取消履约单接口 orderId 9001
    那么 应返回 200
    并且 该履约单状态应为 CANCELLED

  场景: SHIPPED 或 DELIVERED 状态取消应失败并返回错误
    假如 已存在 SHIPPED 状态的履约单 orderId 9002
    当 Order 调用取消履约单接口 orderId 9002
    那么 应返回 200
    并且 返回的 cancelledCount 应为 0

  场景: 按 orderId 取消该订单的所有未发货履约单
    假如 已存在 CREATED 状态的履约单 orderId 9003
    当 Order 调用取消履约单接口 orderId 9003
    那么 应返回 200
    并且 返回的 cancelledCount 应为 1
    并且 该履约单状态应为 CANCELLED
