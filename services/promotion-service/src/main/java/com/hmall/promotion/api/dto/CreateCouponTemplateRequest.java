package com.hmall.promotion.api.dto;

import com.hmall.promotion.domain.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record CreateCouponTemplateRequest(
        @NotBlank String name,
        @NotNull CouponType type,
        @NotNull Long thresholdCents,
        Long discountCents,
        BigDecimal discountRate,
        Long maxDiscountCents,
        @NotNull Integer totalQuantity,
        @NotNull Integer perUserLimit,
        @NotNull Integer validDays,
        TargetingRule targetingRule
) {
    public record TargetingRule(
            Set<String> levelsIn,
            Set<String> tagsAny,
            Set<String> tagsAll,
            Set<String> excludeTags
    ) {
    }
}
