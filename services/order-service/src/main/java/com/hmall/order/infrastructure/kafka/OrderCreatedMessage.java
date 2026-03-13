package com.hmall.order.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.order.application.event.ItemSnapshot;
import com.hmall.order.application.event.OrderCreatedEvent;

import java.time.Instant;
import java.util.List;

/**
 * Kafka 消息体：OrderCreated，供进程外消费者订阅。含 userId、totalAmountCents、items 快照（智能运营 Step 1）。
 */
public record OrderCreatedMessage(
    String eventType,
    long orderId,
    long userId,
    long totalAmountCents,
    List<ItemSnapshotPayload> items,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "OrderCreated";

    public record ItemSnapshotPayload(long skuId, Long spuId, int quantity, long unitPriceCents) {}

    public static OrderCreatedMessage from(OrderCreatedEvent event) {
        List<ItemSnapshotPayload> list = event.items().stream()
            .map(i -> new ItemSnapshotPayload(i.skuId(), i.spuId(), i.quantity(), i.unitPriceCents()))
            .toList();
        return new OrderCreatedMessage(
            EVENT_TYPE,
            event.orderId(),
            event.userId(),
            event.totalAmountCents(),
            list,
            Instant.now()
        );
    }
}
