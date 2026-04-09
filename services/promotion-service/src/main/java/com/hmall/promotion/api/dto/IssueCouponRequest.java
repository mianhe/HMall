package com.hmall.promotion.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record IssueCouponRequest(
        @NotNull Long userId,
        @NotNull @Min(1) Integer quantity
) {}
