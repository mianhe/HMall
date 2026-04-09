package com.hmall.promotion.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class CouponTemplate {

    private Long id;
    private final String name;
    private final CouponType type;
    private final long thresholdCents;
    private final Long discountCents;
    private final BigDecimal discountRate;
    private final Long maxDiscountCents;
    private final int totalQuantity;
    private int issuedQuantity;
    private final int perUserLimit;
    private final int validDays;
    private final TargetingRule targetingRule;
    private TemplateStatus status;
    private final Instant createdAt;

    public CouponTemplate(
            String name,
            CouponType type,
            long thresholdCents,
            Long discountCents,
            BigDecimal discountRate,
            Long maxDiscountCents,
            int totalQuantity,
            int perUserLimit,
            int validDays,
            TargetingRule targetingRule
    ) {
        this.id = null;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.thresholdCents = thresholdCents;
        this.discountCents = discountCents;
        this.discountRate = discountRate;
        this.maxDiscountCents = maxDiscountCents;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.perUserLimit = perUserLimit;
        this.validDays = validDays;
        this.targetingRule = normalizeTargetingRule(targetingRule);
        this.status = TemplateStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public CouponTemplate(
            Long id, String name, CouponType type, long thresholdCents,
            Long discountCents, BigDecimal discountRate, Long maxDiscountCents,
            int totalQuantity, int issuedQuantity, int perUserLimit, int validDays,
            TargetingRule targetingRule,
            TemplateStatus status, Instant createdAt
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.thresholdCents = thresholdCents;
        this.discountCents = discountCents;
        this.discountRate = discountRate;
        this.maxDiscountCents = maxDiscountCents;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.perUserLimit = perUserLimit;
        this.validDays = validDays;
        this.targetingRule = normalizeTargetingRule(targetingRule);
        this.status = status;
        this.createdAt = createdAt;
    }

    public void deactivate() {
        this.status = TemplateStatus.INACTIVE;
    }

    public boolean hasStock(int quantity) {
        return issuedQuantity + quantity <= totalQuantity;
    }

    public void incrementIssuedQuantity(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("库存不足");
        }
        this.issuedQuantity += quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public CouponType getType() { return type; }
    public long getThresholdCents() { return thresholdCents; }
    public Long getDiscountCents() { return discountCents; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public Long getMaxDiscountCents() { return maxDiscountCents; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getIssuedQuantity() { return issuedQuantity; }
    public int getPerUserLimit() { return perUserLimit; }
    public int getValidDays() { return validDays; }
    public TargetingRule getTargetingRule() { return targetingRule; }
    public TemplateStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    private static TargetingRule normalizeTargetingRule(TargetingRule input) {
        if (input == null) {
            return null;
        }
        return new TargetingRule(
                normalizeSet(input.levelsIn()),
                normalizeSet(input.tagsAny()),
                normalizeSet(input.tagsAll()),
                normalizeSet(input.excludeTags())
        );
    }

    private static Set<String> normalizeSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toSet()));
    }

    public record TargetingRule(
            Set<String> levelsIn,
            Set<String> tagsAny,
            Set<String> tagsAll,
            Set<String> excludeTags
    ) {
    }
}
