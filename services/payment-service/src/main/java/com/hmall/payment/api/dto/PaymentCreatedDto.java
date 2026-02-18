package com.hmall.payment.api.dto;

public record PaymentCreatedDto(
    Long paymentId,
    Long orderId,
    Long amountCents,
    String status,
    String payUrl
) {}
