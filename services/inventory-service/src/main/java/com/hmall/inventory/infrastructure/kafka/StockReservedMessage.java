package com.hmall.inventory.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * Kafka 消息体：StockReserved，供进程外消费者订阅。
 */
public record StockReservedMessage(
    String eventType,
    long orderId,
    List<Item> items,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "StockReserved";

    public record Item(long skuId, int quantity) {}

    public static StockReservedMessage from(long orderId, List<com.hmall.inventory.domain.StockReserved.OccupyItemPayload> items, Instant occurredAt) {
        List<Item> list = items.stream()
            .map(p -> new Item(p.skuId(), p.quantity()))
            .toList();
        return new StockReservedMessage(EVENT_TYPE, orderId, list, occurredAt);
    }
}
