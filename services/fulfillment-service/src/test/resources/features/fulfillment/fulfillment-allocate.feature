# language: zh-CN
@fulfillment
功能: 开始配货
  物理履约单在创建时自动分配（ALLOCATING），无需手动触发；手动配货接口仅做幂等保护。

  场景: 创建物理履约单后应自动分配为 ALLOCATING 状态
    假如 已存在 CREATED 状态的履约单 orderId 9001
    那么 该履约单状态应为 ALLOCATING

  场景: 创建物理履约单时应自动发布 FulfillmentOrderAllocated 事件
    假如 已存在 CREATED 状态的履约单 orderId 9002
    那么 应发布 FulfillmentOrderAllocated 事件且 orderId 为 9002

  场景: 已分配的履约单再次执行配货应返回 400
    假如 已存在 ALLOCATING 状态的履约单 orderId 9003
    当 对该履约单执行开始配货
    那么 应返回 400

  场景: 履约单不存在时执行开始配货应返回 404
    当 对不存在的履约单 ID 999999 执行开始配货
    那么 应返回 404
