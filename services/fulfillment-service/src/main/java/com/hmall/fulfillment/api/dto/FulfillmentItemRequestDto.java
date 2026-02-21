package com.hmall.fulfillment.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FulfillmentItemRequestDto(
    @NotNull(message = "skuId 不能为空")
    Long skuId,
    @NotNull(message = "quantity 不能为空")
    @Min(value = 1, message = "quantity 必须大于 0")
    Integer quantity
) {}
