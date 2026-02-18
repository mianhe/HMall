package com.hmall.payment.api.dto;

import jakarta.validation.constraints.NotNull;

public record RefundRequestDto(
    @NotNull(message = "orderId 必填") Long orderId
) {}
