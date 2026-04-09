package com.hmall.promotion.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PromotionActivity {

    private Long id;
    private final String name;
    private final PromotionActivityType type;
    private final Set<Long> targetSkuIds;
    private final Long thresholdCents;
    private final long discountCents;
    private final String mutexGroupCode;
    private final int priority;
    private final Instant startAt;
    private final Instant endAt;
    private final TargetingRule targetingRule;
    private final PieceRule pieceRule;
    private PromotionActivityStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public PromotionActivity(
            String name,
            PromotionActivityType type,
            Set<Long> targetSkuIds,
            Long thresholdCents,
            long discountCents,
            String mutexGroupCode,
            int priority,
            Instant startAt,
            Instant endAt
    ) {
        this(
                null, name, type, targetSkuIds, thresholdCents, discountCents, mutexGroupCode, priority,
                startAt, endAt, null, null, PromotionActivityStatus.DRAFT, Instant.now(), Instant.now());
    }

    public PromotionActivity(
            String name,
            PromotionActivityType type,
            Set<Long> targetSkuIds,
            Long thresholdCents,
            long discountCents,
            String mutexGroupCode,
            int priority,
            Instant startAt,
            Instant endAt,
            TargetingRule targetingRule,
            PieceRule pieceRule
    ) {
        this(
                null, name, type, targetSkuIds, thresholdCents, discountCents, mutexGroupCode, priority,
                startAt, endAt, targetingRule, pieceRule, PromotionActivityStatus.DRAFT, Instant.now(), Instant.now());
    }

    public PromotionActivity(
            Long id,
            String name,
            PromotionActivityType type,
            Set<Long> targetSkuIds,
            Long thresholdCents,
            long discountCents,
            String mutexGroupCode,
            int priority,
            Instant startAt,
            Instant endAt,
            TargetingRule targetingRule,
            PieceRule pieceRule,
            PromotionActivityStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.targetSkuIds = targetSkuIds == null ? Set.of() : Set.copyOf(targetSkuIds);
        this.thresholdCents = thresholdCents;
        this.discountCents = discountCents;
        this.mutexGroupCode = mutexGroupCode;
        this.priority = priority;
        this.startAt = Objects.requireNonNull(startAt, "startAt");
        this.endAt = Objects.requireNonNull(endAt, "endAt");
        this.targetingRule = targetingRule;
        this.pieceRule = pieceRule;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        validate();
    }

    private void validate() {
        if (name.isBlank()) {
            throw new IllegalArgumentException("活动名称不能为空");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("活动结束时间必须晚于开始时间");
        }
        if (discountCents <= 0) {
            throw new IllegalArgumentException("优惠金额必须大于 0");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("优先级不能小于 0");
        }
        if (type == PromotionActivityType.SKU_AMOUNT_OFF && targetSkuIds.isEmpty()) {
            throw new IllegalArgumentException("单品活动必须指定目标 SKU");
        }
        if (type == PromotionActivityType.ORDER_AMOUNT_OFF) {
            if (thresholdCents == null || thresholdCents < 0) {
                throw new IllegalArgumentException("订单满减活动必须指定有效门槛");
            }
        }
        if (pieceRule != null) {
            if (pieceRule.minQuantity() == null || pieceRule.minQuantity() <= 0) {
                throw new IllegalArgumentException("满件规则门槛必须大于 0");
            }
        }
    }

    public void activate() {
        this.status = PromotionActivityStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = PromotionActivityStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean isActiveAt(Instant now) {
        return status == PromotionActivityStatus.ACTIVE
                && !now.isBefore(startAt)
                && now.isBefore(endAt);
    }

    public boolean targetsSku(Long skuId) {
        if (type != PromotionActivityType.SKU_AMOUNT_OFF) {
            return false;
        }
        return targetSkuIds.contains(skuId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public PromotionActivityType getType() {
        return type;
    }

    public Set<Long> getTargetSkuIds() {
        return Collections.unmodifiableSet(new HashSet<>(targetSkuIds));
    }

    public Long getThresholdCents() {
        return thresholdCents;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public String getMutexGroupCode() {
        return mutexGroupCode;
    }

    public int getPriority() {
        return priority;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public TargetingRule getTargetingRule() {
        return targetingRule;
    }

    public PieceRule getPieceRule() {
        return pieceRule;
    }

    public PromotionActivityStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public record TargetingRule(
            Set<String> levelsIn,
            Set<String> tagsAny,
            Set<String> tagsAll,
            Set<String> excludeTags
    ) {
        public TargetingRule {
            levelsIn = levelsIn == null ? Set.of() : Set.copyOf(levelsIn);
            tagsAny = tagsAny == null ? Set.of() : Set.copyOf(tagsAny);
            tagsAll = tagsAll == null ? Set.of() : Set.copyOf(tagsAll);
            excludeTags = excludeTags == null ? Set.of() : Set.copyOf(excludeTags);
        }
    }

    public record PieceRule(
            String scopeType,
            Set<Long> scopeIds,
            Integer minQuantity,
            String discountType,
            Long discountValue,
            Long maxDiscountCents
    ) {
        public PieceRule {
            scopeIds = scopeIds == null ? Set.of() : Set.copyOf(scopeIds);
        }
    }
}
