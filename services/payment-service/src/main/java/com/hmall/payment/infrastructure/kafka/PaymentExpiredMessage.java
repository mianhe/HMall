package com.hmall.payment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record PaymentExpiredMessage(
    String eventType,
    long orderId,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "PaymentExpired";

    public static PaymentExpiredMessage from(long orderId, Instant occurredAt) {
        return new PaymentExpiredMessage(EVENT_TYPE, orderId, occurredAt);
    }
}
