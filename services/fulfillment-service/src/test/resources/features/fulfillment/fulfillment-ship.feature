# language: zh-CN
@fulfillment
功能: 发货
  作为管理后台操作人员，我希望对 ALLOCATING 状态的履约单执行发货，以便推进履约流程。

  场景: ALLOCATING 状态的履约单执行发货应成功且状态变为 SHIPPED
    假如 已存在 ALLOCATING 状态的履约单 orderId 9001
    当 对该履约单执行发货 承运商 "顺丰" 物流单号 "SF1234567890"
    那么 应返回 200
    并且 该履约单状态应为 SHIPPED

  场景: 发货成功时应发布 FulfillmentShipped 事件
    假如 已存在 ALLOCATING 状态的履约单 orderId 9002
    当 对该履约单执行发货 承运商 "顺丰" 物流单号 "SF1234567891"
    那么 应返回 200
    并且 应发布 FulfillmentShipped 事件且 orderId 为 9002

  场景: 非 ALLOCATING 状态（如 SHIPPED）执行发货应失败并返回错误
    假如 已存在 SHIPPED 状态的履约单 orderId 9004
    当 对该履约单执行发货 承运商 "顺丰" 物流单号 "SF1234567893"
    那么 应返回 400

  场景: 履约单不存在时执行发货应返回 404
    当 对不存在的履约单 ID 999999 执行发货 承运商 "顺丰" 物流单号 "SF1234567894"
    那么 应返回 404
