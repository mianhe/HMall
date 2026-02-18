package com.hmall.payment.api.dto;

import java.time.Instant;

public record PaymentDto(
    Long paymentId,
    Long orderId,
    Long amountCents,
    String status,
    String payUrl,
    Instant createdAt,
    Instant updatedAt,
    Instant expiredAt
) {}
