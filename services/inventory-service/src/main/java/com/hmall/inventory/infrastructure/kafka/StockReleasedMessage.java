package com.hmall.inventory.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Kafka 消息体：StockReleased，供进程外消费者订阅。
 */
public record StockReleasedMessage(
    String eventType,
    long orderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "StockReleased";

    public static StockReleasedMessage from(long orderId, Instant occurredAt) {
        return new StockReleasedMessage(EVENT_TYPE, orderId, occurredAt);
    }
}
