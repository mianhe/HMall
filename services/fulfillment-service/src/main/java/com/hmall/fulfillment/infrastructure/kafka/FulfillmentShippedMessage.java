package com.hmall.fulfillment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.fulfillment.domain.FulfillmentShipped;

import java.time.Instant;

public record FulfillmentShippedMessage(
    String eventType,
    long orderId,
    long fulfillmentOrderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {
    public static FulfillmentShippedMessage from(FulfillmentShipped event) {
        return new FulfillmentShippedMessage(
            "FulfillmentShipped", event.orderId(), event.fulfillmentOrderId(), event.occurredAt());
    }
}
