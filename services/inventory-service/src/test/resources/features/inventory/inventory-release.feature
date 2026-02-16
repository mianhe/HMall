# language: zh-CN
@inventory
功能: 释放库存
  作为 Order 服务，在取消订单时调用释放接口，以便释放该订单占用的库存。

  场景: Order 调用释放接口时应释放该订单的所有占用
    假如 skuId 1001 可用库存为 10
    并且 Order 已占用 orderId 9001 且 items 为 skuId 1001 数量 3
    当 Order 调用释放接口 orderId 9001
    那么 释放应成功
    并且 应返回 200
    并且 skuId 1001 可用库存应为 10

  场景: 该订单无占用记录时调用释放接口应幂等
    当 Order 调用释放接口 orderId 9999
    那么 释放应成功
    并且 应返回 200

  场景: 入参缺失 orderId 时应返回 400
    当 Order 调用释放接口且 request body 为缺少 orderId
    那么 应返回 400

  场景: 释放成功时应发布 StockReleased 领域事件
    假如 skuId 1001 可用库存为 10
    并且 Order 已占用 orderId 9005 且 items 为 skuId 1001 数量 1
    当 Order 调用释放接口 orderId 9005
    那么 释放应成功
    并且 应发布 StockReleased 事件且 orderId 为 9005
