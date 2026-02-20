package com.hmall.payment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record PaymentFailedMessage(
    String eventType,
    long orderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "PaymentFailed";

    public static PaymentFailedMessage from(long orderId, Instant occurredAt) {
        return new PaymentFailedMessage(EVENT_TYPE, orderId, occurredAt);
    }
}
