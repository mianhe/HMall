package com.hmall.fulfillment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.fulfillment.domain.FulfillmentOrderCreated;

import java.time.Instant;
import java.util.List;

public record FulfillmentOrderCreatedMessage(
    String eventType,
    long orderId,
    List<Long> fulfillmentOrderIds,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {
    public static FulfillmentOrderCreatedMessage from(FulfillmentOrderCreated event) {
        return new FulfillmentOrderCreatedMessage(
            "FulfillmentOrderCreated", event.orderId(), event.fulfillmentOrderIds(), event.occurredAt());
    }
}
