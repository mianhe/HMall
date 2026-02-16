package com.hmall.order.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * Kafka 消息体：OrderCreated，供进程外消费者订阅。
 */
public record OrderCreatedMessage(
    String eventType,
    long orderId,
    List<ItemQuantity> items,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "OrderCreated";

    public record ItemQuantity(long skuId, int quantity) {}

    public static OrderCreatedMessage from(long orderId, List<com.hmall.order.application.event.OrderCreatedEvent.ItemQuantity> items) {
        List<ItemQuantity> list = items.stream()
            .map(iq -> new ItemQuantity(iq.skuId(), iq.quantity()))
            .toList();
        return new OrderCreatedMessage(EVENT_TYPE, orderId, list, Instant.now());
    }
}
