package com.hmall.order.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Kafka 消息体：OrderCompleted，供进程外消费者订阅。
 */
public record OrderCompletedMessage(
    String eventType,
    long orderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "OrderCompleted";

    public static OrderCompletedMessage from(long orderId) {
        return new OrderCompletedMessage(EVENT_TYPE, orderId, Instant.now());
    }
}
