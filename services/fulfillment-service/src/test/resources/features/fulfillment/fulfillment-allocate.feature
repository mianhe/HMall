# language: zh-CN
@fulfillment
功能: 开始配货
  作为管理后台操作人员，我希望对 CREATED 状态的履约单执行开始配货，以便进入配货流程并通知 Order 显示「正在配货」。

  场景: CREATED 状态的履约单执行开始配货应成功且状态变为 ALLOCATING
    假如 已存在 CREATED 状态的履约单 orderId 9001
    当 对该履约单执行开始配货
    那么 应返回 200
    并且 该履约单状态应为 ALLOCATING

  场景: 开始配货成功时应发布 FulfillmentOrderAllocated 事件
    假如 已存在 CREATED 状态的履约单 orderId 9002
    当 对该履约单执行开始配货
    那么 应返回 200
    并且 应发布 FulfillmentOrderAllocated 事件且 orderId 为 9002

  场景: 非 CREATED 状态执行开始配货应失败并返回错误
    假如 已存在 ALLOCATING 状态的履约单 orderId 9003
    当 对该履约单执行开始配货
    那么 应返回 400

  场景: 履约单不存在时执行开始配货应返回 404
    当 对不存在的履约单 ID 999999 执行开始配货
    那么 应返回 404
