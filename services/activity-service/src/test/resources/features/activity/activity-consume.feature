# language: zh-CN
@activity
功能: 事件消费与记录
  收到已订阅的领域事件后落库为业务活动；重复事件（同一 eventId）不重复记录。

  场景大纲: 收到某类事件后应记录一条业务活动
    假如 当前无订单 <orderId> 的活动记录
    当 收到事件 eventId "<eventId>" eventType "<eventType>" topic "<topic>" orderId <orderId> occurredAt "<occurredAt>"
    那么 应存在一条活动记录 eventId "<eventId>" eventType "<eventType>" topic "<topic>" orderId <orderId>
    并且 该记录的 occurredAt 为 "<occurredAt>"

    例子:
      | eventId                              | eventType        | topic                     | orderId | occurredAt           |
      | a0000001-0000-0000-0000-000000000001 | OrderCreated     | order.created             | 101     | 2025-02-19T10:00:00Z |
      | a0000001-0000-0000-0000-000000000002 | OrderCancelled   | order.cancelled           | 102     | 2025-02-19T10:01:00Z |
      | a0000001-0000-0000-0000-000000000003 | OrderCompleted   | order.completed           | 103     | 2025-02-19T10:02:00Z |
      | a0000001-0000-0000-0000-000000000004 | PaymentCompleted | payment.completed         | 104     | 2025-02-19T10:03:00Z |
      | a0000001-0000-0000-0000-000000000005 | PaymentFailed    | payment.failed            | 105     | 2025-02-19T10:04:00Z |
      | a0000001-0000-0000-0000-000000000006 | PaymentExpired   | payment.expired           | 106     | 2025-02-19T10:05:00Z |
      | a0000001-0000-0000-0000-000000000007 | StockReserved    | inventory.stock.reserved  | 107     | 2025-02-19T10:06:00Z |
      | a0000001-0000-0000-0000-000000000008 | StockReleased    | inventory.stock.released  | 108     | 2025-02-19T10:07:00Z |

  场景: 重复事件不应重复记录
    假如 已存在活动记录 eventId "dup-event-001" eventType "OrderCreated" topic "order.created" orderId 201 occurredAt "2025-02-19T12:00:00Z"
    当 再次收到相同事件 eventId "dup-event-001" eventType "OrderCreated" topic "order.created" orderId 201 occurredAt "2025-02-19T12:00:00Z"
    那么 订单 201 的活动记录数应为 1
