package com.hmall.payment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequestDto(
    @NotNull(message = "orderId 必填") Long orderId,
    @NotNull(message = "amountCents 必填") @Positive(message = "amountCents 须大于 0") Long amountCents
) {}
