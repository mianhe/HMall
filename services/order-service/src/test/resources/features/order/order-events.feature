# language: zh-CN
@order
功能: 事件驱动（Order 订阅）
  作为订单服务，当收到下游 BC 的事件时，应更新订单状态并触发相应动作。
  库存占用、支付创建、创建履约单已改为同步调用；Fulfillment Shipped/Delivered 事件仍为异步。

  场景: 4.1 收到 PaymentCompleted 后应同步创建履约单并保持 status 为 PAID
    假如 已存在用户 "alice" 密码 "secret123"
    并且 Catalog 已有商品 "iPhone 15" skuId 123 价格 599900 分
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902" 购买 "iPhone 15" 数量 1
    当 发布 PaymentCompleted 事件 针对该订单 paymentId 1001
    那么 订单 status 应为 PAID
    并且 应已发起创建履约单

  场景: 4.3 收到 FulfillmentOrderAllocated 后应将 status 置为 FULFILLING
    假如 已存在用户 "alice" 密码 "secret123"
    并且 Catalog 已有商品 "iPhone 15" skuId 123 价格 599900 分
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902" 购买 "iPhone 15" 数量 1
    当 发布 PaymentCompleted 事件 针对该订单 paymentId 1001
    当 发布 FulfillmentOrderAllocated 事件 针对该订单
    那么 订单 status 应为 FULFILLING

  场景: 4.4a 收到 PaymentFailed 后订单应保持 PENDING_PAYMENT（用户可重试支付）
    假如 已存在用户 "alice" 密码 "secret123"
    并且 Catalog 已有商品 "iPhone 15" skuId 123 价格 599900 分
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902" 购买 "iPhone 15" 数量 1
    当 发布 PaymentFailed 事件 针对该订单
    那么 订单 status 应为 PENDING_PAYMENT

  场景: 4.4b 收到 PaymentExpired 后应取消订单
    假如 已存在用户 "alice" 密码 "secret123"
    并且 Catalog 已有商品 "iPhone 15" skuId 123 价格 599900 分
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902" 购买 "iPhone 15" 数量 1
    当 发布 PaymentExpired 事件 针对该订单
    那么 订单 status 应为 CANCELLED

  场景: 4.6 收到 FulfillmentShipped 后应更新 status 为 SHIPPED
    假如 已存在订单状态为 FULFILLING
    当 发布 FulfillmentShipped 事件 针对该订单
    那么 订单 status 应为 SHIPPED

  场景: 4.7 收到 FulfillmentDelivered 后应将 status 置为 DELIVERED 并发布 OrderCompleted
    假如 已存在订单状态为 FULFILLING
    当 发布 FulfillmentShipped 事件 针对该订单
    当 发布 FulfillmentDelivered 事件 针对该订单
    那么 订单 status 应为 DELIVERED
    并且 应已发布 OrderCompleted

