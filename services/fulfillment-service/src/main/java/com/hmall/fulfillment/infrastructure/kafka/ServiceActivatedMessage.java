package com.hmall.fulfillment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmall.fulfillment.domain.ServiceActivated;

import java.time.Instant;

public record ServiceActivatedMessage(
    String eventType,
    long orderId,
    long fulfillmentOrderId,
    long serviceSkuId,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant activatedAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {
    public static ServiceActivatedMessage from(ServiceActivated event) {
        return new ServiceActivatedMessage(
            "ServiceActivated",
            event.orderId(),
            event.fulfillmentOrderId(),
            event.serviceSkuId(),
            event.activatedAt(),
            event.expiresAt(),
            event.occurredAt()
        );
    }
}
