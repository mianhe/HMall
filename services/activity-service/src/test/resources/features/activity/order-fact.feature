# language: zh-CN
@activity
功能: 订单事实投影
  事件写入后自动投影为 OrderFact 和 OrderItemFact，支持多维运营分析。

  场景: 5.1 OrderCreated 事件生成初始 OrderFact
    假如 清空订单 501 的活动和事实记录
    当 记录 OrderCreated 事件 orderId 501 userId 1 totalAmountCents 299900 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 10    | 1     | 2        | 99900          |
      | 20    | 1     | 1        | 100100         |
    那么 订单 501 的 OrderFact 应存在
    并且 OrderFact 字段应为:
      | field            | value   |
      | currentStage     | CREATED |
      | userId           | 1       |
      | totalAmountCents | 299900  |
      | itemCount        | 2       |
      | totalQuantity    | 3       |
      | hasEngraving     | false   |
      | hasWarranty      | false   |
      | isAbnormal       | false   |
    并且 订单 501 应有 2 条 OrderItemFact

  场景: 5.2 PaymentCompleted 推进阶段到 PAID
    假如 清空订单 502 的活动和事实记录
    当 记录 OrderCreated 事件 orderId 502 userId 2 totalAmountCents 199900 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 30    | 2     | 1        | 199900         |
    并且 记录事件 eventType "PaymentCompleted" orderId 502 payload '{"orderId":502,"paymentId":5020,"amountCents":199900}'
    那么 订单 502 的 OrderFact 应存在
    并且 OrderFact 字段应为:
      | field        | value |
      | currentStage | PAID  |
    并且 OrderFact 的 paidAt 不为空

  场景: 5.3 EngravingCompleted 标记含镭雕
    假如 清空订单 503 的活动和事实记录
    当 记录 OrderCreated 事件 orderId 503 userId 3 totalAmountCents 399900 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 40    | 3     | 1        | 399900         |
    并且 记录事件 eventType "PaymentCompleted" orderId 503 payload '{"orderId":503,"paymentId":5030,"amountCents":399900}'
    并且 记录事件 eventType "EngravingCompleted" orderId 503 payload '{"orderId":503,"skuId":40}'
    那么 OrderFact 字段应为:
      | field        | value |
      | hasEngraving | true  |
      | hasWarranty  | false |
      | currentStage | PAID  |

  场景: 5.4 OrderCancelled 标记取消及原因
    假如 清空订单 504 的活动和事实记录
    当 记录 OrderCreated 事件 orderId 504 userId 4 totalAmountCents 99900 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 50    | 4     | 1        | 99900          |
    并且 记录事件 eventType "PaymentExpired" orderId 504 payload '{"orderId":504}'
    并且 记录事件 eventType "OrderCancelled" orderId 504 payload '{"orderId":504,"reason":"TIMEOUT"}'
    那么 OrderFact 字段应为:
      | field        | value     |
      | currentStage | CANCELLED |
      | cancelReason | TIMEOUT   |
      | isAbnormal   | true      |

  场景: 5.5 完整生命周期投影含 ServiceActivated
    假如 清空订单 505 的活动和事实记录
    当 记录 OrderCreated 事件 orderId 505 userId 5 totalAmountCents 599900 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 60    | 5     | 1        | 599900         |
    并且 记录事件 eventType "PaymentCompleted" orderId 505 payload '{"orderId":505,"paymentId":5050,"amountCents":599900}'
    并且 记录事件 eventType "ServiceActivated" orderId 505 payload '{"orderId":505,"skuId":60,"warrantyMonths":24}'
    并且 记录事件 eventType "FulfillmentOrderCreated" orderId 505 payload '{"orderId":505}'
    并且 记录事件 eventType "FulfillmentShipped" orderId 505 payload '{"orderId":505}'
    并且 记录事件 eventType "FulfillmentDelivered" orderId 505 payload '{"orderId":505}'
    并且 记录事件 eventType "OrderCompleted" orderId 505 payload '{"orderId":505}'
    那么 OrderFact 字段应为:
      | field                  | value     |
      | currentStage           | COMPLETED |
      | hasEngraving           | false     |
      | hasWarranty            | true      |
      | isAbnormal             | false     |
    并且 OrderFact 的 paidAt 不为空
    并且 OrderFact 的 shippedAt 不为空
    并且 OrderFact 的 deliveredAt 不为空
    并且 OrderFact 的 completedAt 不为空
    并且 OrderFact 的 paymentDurationSec 不为空
    并且 OrderFact 的 fulfillmentDurationSec 不为空

  场景: 5.6 stats 查询返回聚合统计
    假如 清空全部活动和事实记录
    当 记录 OrderCreated 事件 orderId 601 userId 61 totalAmountCents 100000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 10    | 1     | 1        | 100000         |
    并且 记录事件 eventType "PaymentCompleted" orderId 601 payload '{"orderId":601,"paymentId":6010,"amountCents":100000}'
    并且 记录事件 eventType "EngravingCompleted" orderId 601 payload '{"orderId":601,"skuId":10}'
    并且 记录事件 eventType "OrderCompleted" orderId 601 payload '{"orderId":601}'
    当 记录 OrderCreated 事件 orderId 602 userId 62 totalAmountCents 200000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 20    | 2     | 2        | 100000         |
    并且 记录事件 eventType "PaymentExpired" orderId 602 payload '{"orderId":602}'
    并且 记录事件 eventType "OrderCancelled" orderId 602 payload '{"orderId":602}'
    当 查询 stats from "2025-03-01" to "2025-03-02"
    那么 stats 响应字段应为:
      | field               | value  |
      | totalOrders         | 2      |
      | completedOrders     | 1      |
      | cancelledOrders     | 1      |
      | ordersWithEngraving | 1      |
      | ordersWithAnyVas    | 1      |
      | cancelByTimeout     | 1      |
      | totalRevenueCents   | 300000 |

  场景: 5.7 stats_daily 查询返回按日统计
    假如 清空全部活动和事实记录
    当 记录 OrderCreated 事件 orderId 701 userId 71 totalAmountCents 150000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 10    | 1     | 1        | 150000         |
    当 查询 stats_daily from "2025-03-01" to "2025-03-01"
    那么 stats_daily 应有 1 天记录
    并且 第 1 天的 totalOrders 为 1

  场景: 5.8 product_ranking 查询返回商品排名
    假如 清空全部活动和事实记录
    当 记录 OrderCreated 事件 orderId 801 userId 81 totalAmountCents 300000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 10    | 1     | 2        | 100000         |
      | 20    | 2     | 1        | 100000         |
    当 记录 OrderCreated 事件 orderId 802 userId 82 totalAmountCents 200000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 10    | 1     | 2        | 100000         |
    当 查询 product_ranking rankBy "revenue" from "2025-03-01" to "2025-03-02"
    那么 product_ranking 排名第 1 的 spuId 为 1
    并且 product_ranking 排名第 1 的 totalQuantity 为 4

  场景: 5.9 列表查询支持筛选
    假如 清空全部活动和事实记录
    当 记录 OrderCreated 事件 orderId 901 userId 91 totalAmountCents 100000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 10    | 1     | 1        | 100000         |
    并且 记录事件 eventType "EngravingCompleted" orderId 901 payload '{"orderId":901,"skuId":10}'
    当 记录 OrderCreated 事件 orderId 902 userId 92 totalAmountCents 200000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 20    | 2     | 1        | 200000         |
    当 查询 order-facts hasEngraving true from "2025-03-01" to "2025-03-02"
    那么 列表应返回 1 条记录
    并且 列表第 1 条的 orderId 为 901

  场景: 5.10 批量重建投影
    假如 清空全部活动和事实记录
    当 记录 OrderCreated 事件 orderId 510 userId 10 totalAmountCents 100000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 70    | 6     | 1        | 100000         |
    并且 记录 OrderCreated 事件 orderId 511 userId 11 totalAmountCents 200000 items:
      | skuId | spuId | quantity | unitPriceCents |
      | 80    | 7     | 2        | 100000         |
    并且 清空全部事实记录
    当 调用重建订单事实投影
    那么 订单 510 的 OrderFact 应存在
    并且 订单 511 的 OrderFact 应存在
