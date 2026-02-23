# language: zh-CN
@inventory
功能: 库存管理（管理后台）
  作为管理后台，需要按 skuId 初始化或更新库存，并查询 available、reserved。

  场景: 按 skuId 初始化或更新库存
    当 管理端 对 skuId 1001 设置可用库存为 20
    那么 应返回 200
    并且 skuId 1001 的库存应为 available 20、reserved 0
    当 管理端 对 skuId 1001 设置可用库存为 30
    那么 应返回 200
    并且 skuId 1001 的库存应为 available 30、reserved 0

  场景: 按 skuId 查询库存
    假如 skuId 1001 可用库存为 10
    并且 Order 已占用 orderId 8001 且 items 为 skuId 1001 数量 3
    当 管理端 查询 skuId 1001 的库存
    那么 应返回 200
    并且 skuId 1001 的库存应为 available 7、reserved 3

  场景: 查询全部库存列表
    假如 skuId 2001 可用库存为 50
    并且 skuId 2002 可用库存为 30
    当 管理端 查询全部库存列表
    那么 应返回 200
    并且 返回的库存列表应包含 skuId 2001 且 available 50
    并且 返回的库存列表应包含 skuId 2002 且 available 30
