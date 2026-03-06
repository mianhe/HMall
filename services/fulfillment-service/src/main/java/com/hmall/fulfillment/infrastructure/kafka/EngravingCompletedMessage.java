package com.hmall.fulfillment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.fulfillment.domain.EngravingCompleted;

import java.time.Instant;

public record EngravingCompletedMessage(
    String eventType,
    long orderId,
    long fulfillmentOrderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {
    public static EngravingCompletedMessage from(EngravingCompleted event) {
        return new EngravingCompletedMessage(
            "EngravingCompleted", event.orderId(), event.fulfillmentOrderId(), event.occurredAt());
    }
}
