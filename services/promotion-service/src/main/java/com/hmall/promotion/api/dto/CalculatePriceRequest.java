package com.hmall.promotion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CalculatePriceRequest(
        @NotEmpty @Valid List<LineItem> items,
        @NotNull Long userId,
        Long couponId
) {
    public record LineItem(
            @NotNull Long skuId,
            @NotNull Long unitPriceCents,
            @NotNull Integer quantity
    ) {}
}
