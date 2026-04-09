package com.hmall.promotion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PreviewSkuPricesRequest(
        @NotEmpty @Valid List<SkuItem> items,
        Long userId
) {
    public record SkuItem(
            @NotNull Long skuId,
            @NotNull Long unitPriceCents
    ) {
    }
}
