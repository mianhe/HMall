# language: zh-CN
@fulfillment
功能: 查询履约单
  作为管理后台或 Order 服务，查询履约单详情或按订单查询，以便了解履约进度。

  场景: 按 fulfillmentOrderId 查询应返回履约单详情
    假如 已存在 CREATED 状态的履约单 orderId 9001
    当 按 fulfillmentOrderId 查询该履约单
    那么 应返回 200
    并且 返回结果包含履约单详情（含商品明细、状态、物流信息）

  场景: 按 orderId 查询应返回该订单的所有履约单
    假如 已存在 CREATED 状态的履约单 orderId 9002
    当 按 orderId 9002 查询履约单
    那么 应返回 200
    并且 返回结果包含该 orderId 的所有履约单

  场景: 履约单不存在时应返回 404
    当 按 fulfillmentOrderId 999999 查询履约单
    那么 应返回 404
