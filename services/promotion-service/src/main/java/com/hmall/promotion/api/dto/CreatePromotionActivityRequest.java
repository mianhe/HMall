package com.hmall.promotion.api.dto;

import com.hmall.promotion.domain.PromotionActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record CreatePromotionActivityRequest(
        @NotBlank String name,
        @NotNull PromotionActivityType type,
        List<Long> targetSkuIds,
        Long thresholdCents,
        @NotNull @Positive Long discountCents,
        String mutexGroupCode,
        Integer priority,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        TargetingRule targetingRule,
        PieceRule pieceRule
) {
    public record TargetingRule(
            Set<String> levelsIn,
            Set<String> tagsAny,
            Set<String> tagsAll,
            Set<String> excludeTags
    ) {}

    public record PieceRule(
            String scopeType,
            Set<Long> scopeIds,
            Integer minQuantity,
            String discountType,
            Long discountValue,
            Long maxDiscountCents
    ) {}
}
