# language: zh-CN
@activity
功能: 统计与仪表盘
  基于事件聚合的统计，支持时间范围查询。

  背景:
    假如 清空所有活动记录

  场景: 查询今日统计
    假如 今日存在以下活动记录:
      | eventId                              | eventType        | topic                    | orderId |
      | s0000001-0000-0000-0000-000000000001 | OrderCreated     | order.created            | 501     |
      | s0000001-0000-0000-0000-000000000002 | OrderCreated     | order.created            | 502     |
      | s0000001-0000-0000-0000-000000000003 | OrderCancelled   | order.cancelled          | 503     |
      | s0000001-0000-0000-0000-000000000004 | PaymentCompleted | payment.completed        | 504     |
      | s0000001-0000-0000-0000-000000000005 | PaymentFailed    | payment.failed           | 505     |
      | s0000001-0000-0000-0000-000000000006 | PaymentExpired   | payment.expired          | 506     |
      | s0000001-0000-0000-0000-000000000007 | StockReserved    | inventory.stock.reserved | 507     |
    当 查询统计不传参数
    那么 统计结果应为:
      | ordersCreated | ordersCancelled | ordersCompleted | paymentAttempts | paymentSuccess | paymentFailed | paymentExpired | stockReserved | stockReleased |
      | 2             | 1               | 0               | 3               | 1              | 1             | 1              | 1             | 0             |

  场景: 按起止日期查询统计
    假如 存在以下活动记录:
      | eventId                              | eventType        | topic             | orderId | occurredAt           |
      | s0000002-0000-0000-0000-000000000001 | OrderCreated     | order.created     | 601     | 2025-01-15T10:00:00Z |
      | s0000002-0000-0000-0000-000000000002 | OrderCreated     | order.created     | 602     | 2025-01-16T10:00:00Z |
      | s0000002-0000-0000-0000-000000000003 | PaymentCompleted | payment.completed | 603     | 2025-01-17T10:00:00Z |
      | s0000002-0000-0000-0000-000000000004 | OrderCreated     | order.created     | 604     | 2025-02-01T10:00:00Z |
    当 查询统计 from "2025-01-15" to "2025-01-17"
    那么 统计结果中 ordersCreated 应为 2
    并且 统计结果中 paymentSuccess 应为 1
    并且 统计结果中 ordersCompleted 应为 0
    并且 统计结果应回显 from "2025-01-15" to "2025-01-17"

  场景: 按快捷周期查询统计
    假如 昨日存在以下活动记录:
      | eventId                              | eventType        | topic           | orderId |
      | s0000003-0000-0000-0000-000000000001 | OrderCreated     | order.created   | 701     |
      | s0000003-0000-0000-0000-000000000002 | OrderCompleted   | order.completed | 702     |
    当 查询统计 period "last7"
    那么 统计结果中 ordersCreated 应为 1
    并且 统计结果中 ordersCompleted 应为 1
