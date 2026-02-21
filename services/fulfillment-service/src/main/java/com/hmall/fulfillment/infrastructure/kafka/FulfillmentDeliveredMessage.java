package com.hmall.fulfillment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.fulfillment.domain.FulfillmentDelivered;

import java.time.Instant;

public record FulfillmentDeliveredMessage(
    String eventType,
    long orderId,
    long fulfillmentOrderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {
    public static FulfillmentDeliveredMessage from(FulfillmentDelivered event) {
        return new FulfillmentDeliveredMessage(
            "FulfillmentDelivered", event.orderId(), event.fulfillmentOrderId(), event.occurredAt());
    }
}
