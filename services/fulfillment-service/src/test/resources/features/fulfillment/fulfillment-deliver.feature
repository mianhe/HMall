# language: zh-CN
@fulfillment
功能: 签收确认
  作为物流系统回调，确认 SHIPPED 状态的履约单已签收，以便完成履约流程。

  场景: SHIPPED 状态的履约单确认签收应成功且状态变为 DELIVERED
    假如 已存在 SHIPPED 状态的履约单 orderId 9001
    当 对该履约单确认签收
    那么 应返回 200
    并且 该履约单状态应为 DELIVERED

  场景: 签收成功时应发布 FulfillmentDelivered 事件
    假如 已存在 SHIPPED 状态的履约单 orderId 9002
    当 对该履约单确认签收
    那么 应返回 200
    并且 应发布 FulfillmentDelivered 事件且 orderId 为 9002

  场景: 非 SHIPPED 状态确认签收应失败并返回错误
    假如 已存在 CREATED 状态的履约单 orderId 9003
    当 对该履约单确认签收
    那么 应返回 400

  场景: 履约单不存在时确认签收应返回 404
    当 对不存在的履约单 ID 999999 确认签收
    那么 应返回 404
