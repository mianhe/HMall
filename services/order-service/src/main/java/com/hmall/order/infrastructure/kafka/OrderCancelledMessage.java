package com.hmall.order.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.order.application.event.OrderCancelledEvent;

import java.time.Instant;
import java.util.List;

/**
 * Kafka 消息体：OrderCancelled，供进程外消费者订阅。含 userId、totalAmountCents、items 快照（智能运营 Step 1）。
 */
public record OrderCancelledMessage(
    String eventType,
    long orderId,
    long userId,
    long totalAmountCents,
    List<OrderCreatedMessage.ItemSnapshotPayload> items,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "OrderCancelled";

    public static OrderCancelledMessage from(OrderCancelledEvent event) {
        List<OrderCreatedMessage.ItemSnapshotPayload> list = event.items().stream()
            .map(i -> new OrderCreatedMessage.ItemSnapshotPayload(i.skuId(), i.spuId(), i.quantity(), i.unitPriceCents()))
            .toList();
        return new OrderCancelledMessage(
            EVENT_TYPE,
            event.orderId(),
            event.userId(),
            event.totalAmountCents(),
            list,
            Instant.now()
        );
    }
}
