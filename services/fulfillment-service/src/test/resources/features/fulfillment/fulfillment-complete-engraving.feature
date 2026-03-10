# language: zh-CN
@fulfillment
功能: 完成镭雕
  作为管理后台操作人员，对有镭雕内容的履约单执行完成镭雕，以便可发货。

  场景: engravingInfo 非空且 engravingCompletedAt 未设时执行完成镭雕应成功并发布 ServiceActivated
    假如 已存在含镭雕的 ALLOCATING 状态履约单 orderId 8010
    当 对该履约单执行完成镭雕
    那么 应返回 200
    并且 该履约单的 engravingCompletedAt 应已设置
    并且 应发布 ServiceActivated 事件且 orderId 为 8010

  场景: engravingInfo 为空时执行完成镭雕应失败
    假如 已存在 ALLOCATING 状态的履约单 orderId 8011
    当 对该履约单执行完成镭雕
    那么 应返回 400

  场景: engravingCompletedAt 已设时执行完成镭雕应失败
    假如 已存在已完成镭雕的 ALLOCATING 状态履约单 orderId 8012
    当 对该履约单执行完成镭雕
    那么 应返回 400

  场景: 履约单不存在时执行完成镭雕应返回 404
    当 对履约单 999999 执行完成镭雕
    那么 应返回 404
