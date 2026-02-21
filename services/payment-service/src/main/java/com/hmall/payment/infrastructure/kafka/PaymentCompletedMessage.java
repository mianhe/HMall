package com.hmall.payment.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record PaymentCompletedMessage(
    String eventType,
    long orderId,
    long paymentId,
    long amountCents,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant occurredAt
) {
    public static final String EVENT_TYPE = "PaymentCompleted";

    public static PaymentCompletedMessage from(long orderId, long paymentId, long amountCents, Instant occurredAt) {
        return new PaymentCompletedMessage(EVENT_TYPE, orderId, paymentId, amountCents, occurredAt);
    }
}
