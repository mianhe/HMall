# language: zh-CN
@activity
功能: 活动查询
  按订单查询事件时间线，或查询最近全局活动。

  背景:
    假如 清空所有活动记录

  场景: 按 orderId 查询返回事件时间线（occurredAt 正序）
    假如 订单 301 有以下活动记录:
      | eventId                              | eventType        | topic             | occurredAt           |
      | q0000001-0000-0000-0000-000000000001 | OrderCreated     | order.created     | 2025-02-19T10:00:00Z |
      | q0000001-0000-0000-0000-000000000002 | PaymentCompleted | payment.completed | 2025-02-19T10:05:00Z |
      | q0000001-0000-0000-0000-000000000003 | OrderCompleted   | order.completed   | 2025-02-19T10:10:00Z |
    当 按 orderId 301 查询活动
    那么 应依次返回以下活动:
      | eventType        |
      | OrderCreated     |
      | PaymentCompleted |
      | OrderCompleted   |

  场景: orderId 不存在时返回空列表
    当 按 orderId 99999 查询活动
    那么 应返回空列表

  场景: 查询最近活动（occurredAt 倒序）
    假如 存在以下活动记录:
      | eventId                              | eventType        | topic             | orderId | occurredAt           |
      | q0000002-0000-0000-0000-000000000001 | OrderCreated     | order.created     | 401     | 2025-02-19T09:00:00Z |
      | q0000002-0000-0000-0000-000000000002 | PaymentCompleted | payment.completed | 402     | 2025-02-19T10:00:00Z |
      | q0000002-0000-0000-0000-000000000003 | OrderCancelled   | order.cancelled   | 403     | 2025-02-19T11:00:00Z |
    当 查询最近活动 limit 2
    那么 应依次返回以下活动:
      | eventType        |
      | OrderCancelled   |
      | PaymentCompleted |
