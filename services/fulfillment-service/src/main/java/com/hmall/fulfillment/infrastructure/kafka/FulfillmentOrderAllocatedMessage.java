package com.hmall.fulfillment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.fulfillment.domain.FulfillmentOrderAllocated;

import java.time.Instant;

public record FulfillmentOrderAllocatedMessage(
    String eventType,
    long orderId,
    long fulfillmentOrderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {
    public static FulfillmentOrderAllocatedMessage from(FulfillmentOrderAllocated event) {
        return new FulfillmentOrderAllocatedMessage(
            "FulfillmentOrderAllocated", event.orderId(), event.fulfillmentOrderId(), event.occurredAt());
    }
}
