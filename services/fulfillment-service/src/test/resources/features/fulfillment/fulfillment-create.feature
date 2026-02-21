# language: zh-CN
@fulfillment
功能: 创建履约单
  作为 Order 服务，在支付成功后调用创建履约单接口，以便启动履约流程。

  场景: Order 调用创建履约单接口应创建履约单并返回 fulfillmentOrderIds
    当 Order 调用创建履约单接口 orderId 9001 items 为 skuId 1001 数量 2、skuId 1002 数量 1 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902"
    那么 应返回 200
    并且 返回结果包含 orderId 9001
    并且 返回结果包含 fulfillmentOrderIds 列表且非空

  场景: 创建成功时应发布 FulfillmentOrderCreated 事件
    当 Order 调用创建履约单接口 orderId 9002 items 为 skuId 1001 数量 1 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902"
    那么 应返回 200
    并且 应发布 FulfillmentOrderCreated 事件且 orderId 为 9002

  场景: 同一 orderId 重复调用时应幂等处理
    当 Order 调用创建履约单接口 orderId 9003 items 为 skuId 1001 数量 1 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902"
    那么 应返回 200
    当 Order 再次调用创建履约单接口 orderId 9003 items 为 skuId 1001 数量 1 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902"
    那么 应返回 200
    并且 两次返回的 fulfillmentOrderIds 应一致

  场景: 入参缺失 orderId 或 items 为空时应返回 400
    当 Order 调用创建履约单接口且缺少 orderId
    那么 应返回 400
    当 Order 调用创建履约单接口且 items 为空
    那么 应返回 400
