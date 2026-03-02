# language: zh-CN
@inventory
功能: 占用库存
  作为 Order 服务，在创建订单时调用占用接口，以便锁定库存并获知是否成功。

  场景: Order 调用占用接口且各 skuId 库存充足时应占用成功并返回成功
    假如 skuId 1001 可用库存为 10
    并且 skuId 1002 可用库存为 5
    当 Order 调用占用接口 orderId 9001 且 items 为 skuId 1001 数量 3、skuId 1002 数量 2
    那么 占用应成功
    并且 应返回 200
    并且 skuId 1001 可用库存应为 7
    并且 skuId 1002 可用库存应为 3

  场景: Order 调用占用接口且任一 skuId 库存不足时应占用失败并返回错误
    假如 skuId 1001 可用库存为 2
    当 Order 调用占用接口 orderId 9002 且 items 为 skuId 1001 数量 5
    那么 占用应失败
    并且 应返回 400
    并且 错误信息包含 "库存不足"

  场景: 同一 orderId 重复调用占用接口时应幂等处理
    假如 skuId 1001 可用库存为 10
    当 Order 调用占用接口 orderId 9003 且 items 为 skuId 1001 数量 2
    那么 占用应成功
    当 Order 再次调用占用接口 orderId 9003 且 items 为 skuId 1001 数量 2
    那么 占用应成功
    并且 应返回 200
    并且 skuId 1001 可用库存应为 8

  场景: 入参缺失 orderId 或 items 时应返回 400
    当 Order 调用占用接口且 request body 为缺少 orderId
    那么 应返回 400
    当 Order 调用占用接口且 request body 为缺少 items
    那么 应返回 400

  场景: 占用成功时应发布 StockReserved 领域事件
    假如 skuId 1001 可用库存为 10
    当 Order 调用占用接口 orderId 9005 且 items 为 skuId 1001 数量 1
    那么 占用应成功
    并且 应发布 StockReserved 事件且 orderId 为 9005
