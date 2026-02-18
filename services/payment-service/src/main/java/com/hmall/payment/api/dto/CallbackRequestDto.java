package com.hmall.payment.api.dto;

import jakarta.validation.constraints.NotNull;

public record CallbackRequestDto(
    @NotNull(message = "paymentId 必填") Long paymentId,
    @NotNull(message = "success 必填") Boolean success
) {}
