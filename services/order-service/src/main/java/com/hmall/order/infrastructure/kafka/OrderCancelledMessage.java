package com.hmall.order.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Kafka 消息体：OrderCancelled，供进程外消费者订阅。
 */
public record OrderCancelledMessage(
    String eventType,
    long orderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "OrderCancelled";

    public static OrderCancelledMessage from(long orderId) {
        return new OrderCancelledMessage(EVENT_TYPE, orderId, Instant.now());
    }
}
