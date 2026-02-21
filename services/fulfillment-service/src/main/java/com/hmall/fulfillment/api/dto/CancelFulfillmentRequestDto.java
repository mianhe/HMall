package com.hmall.fulfillment.api.dto;

import jakarta.validation.constraints.NotNull;

public record CancelFulfillmentRequestDto(
    @NotNull(message = "orderId 不能为空")
    Long orderId
) {}
