package com.hmall.promotion.api.dto;

import com.hmall.promotion.domain.PromotionActivity;
import com.hmall.promotion.domain.PromotionActivityStatus;
import com.hmall.promotion.domain.PromotionActivityType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record PromotionActivityDto(
        Long id,
        String name,
        PromotionActivityType type,
        List<Long> targetSkuIds,
        Long thresholdCents,
        Long discountCents,
        String mutexGroupCode,
        Integer priority,
        Instant startAt,
        Instant endAt,
        TargetingRule targetingRule,
        PieceRule pieceRule,
        PromotionActivityStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PromotionActivityDto from(PromotionActivity activity) {
        return new PromotionActivityDto(
                activity.getId(),
                activity.getName(),
                activity.getType(),
                activity.getTargetSkuIds().stream().sorted().toList(),
                activity.getThresholdCents(),
                activity.getDiscountCents(),
                activity.getMutexGroupCode(),
                activity.getPriority(),
                activity.getStartAt(),
                activity.getEndAt(),
                activity.getTargetingRule() == null ? null : new TargetingRule(
                        activity.getTargetingRule().levelsIn(),
                        activity.getTargetingRule().tagsAny(),
                        activity.getTargetingRule().tagsAll(),
                        activity.getTargetingRule().excludeTags()
                ),
                activity.getPieceRule() == null ? null : new PieceRule(
                        activity.getPieceRule().scopeType(),
                        activity.getPieceRule().scopeIds(),
                        activity.getPieceRule().minQuantity(),
                        activity.getPieceRule().discountType(),
                        activity.getPieceRule().discountValue(),
                        activity.getPieceRule().maxDiscountCents()
                ),
                activity.getStatus(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }

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
