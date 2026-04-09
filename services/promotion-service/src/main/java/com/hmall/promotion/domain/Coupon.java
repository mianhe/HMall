package com.hmall.promotion.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public class Coupon {

    private Long id;
    private final long templateId;
    private final long userId;
    private final String name;
    private final CouponType type;
    private final long thresholdCents;
    private final Long discountCents;
    private final BigDecimal discountRate;
    private final Long maxDiscountCents;
    private CouponStatus status;
    private Long orderId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant usedAt;

    public static Coupon issueFromTemplate(CouponTemplate template, long userId) {
        return new Coupon(
                null,
                template.getId(),
                userId,
                template.getName(),
                template.getType(),
                template.getThresholdCents(),
                template.getDiscountCents(),
                template.getDiscountRate(),
                template.getMaxDiscountCents(),
                CouponStatus.AVAILABLE,
                null,
                Instant.now(),
                Instant.now().plus(Duration.ofDays(template.getValidDays())),
                null
        );
    }

    public Coupon(
            Long id, long templateId, long userId, String name,
            CouponType type, long thresholdCents, Long discountCents,
            BigDecimal discountRate, Long maxDiscountCents,
            CouponStatus status, Long orderId,
            Instant issuedAt, Instant expiresAt, Instant usedAt
    ) {
        this.id = id;
        this.templateId = templateId;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.thresholdCents = thresholdCents;
        this.discountCents = discountCents;
        this.discountRate = discountRate;
        this.maxDiscountCents = maxDiscountCents;
        this.status = status;
        this.orderId = orderId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public void lock(Long orderId) {
        if (this.status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("只有 AVAILABLE 状态的券可被锁定，当前状态: " + this.status);
        }
        this.status = CouponStatus.LOCKED;
        this.orderId = orderId;
    }

    public void redeem() {
        if (this.status != CouponStatus.LOCKED) {
            throw new IllegalStateException("只有 LOCKED 状态的券可被核销，当前状态: " + this.status);
        }
        this.status = CouponStatus.USED;
        this.usedAt = Instant.now();
    }

    public void release() {
        if (this.status != CouponStatus.LOCKED && this.status != CouponStatus.USED) {
            throw new IllegalStateException("只有 LOCKED 或 USED 状态的券可被释放，当前状态: " + this.status);
        }
        this.status = CouponStatus.AVAILABLE;
        this.orderId = null;
        this.usedAt = null;
    }

    public void expire() {
        if (this.status != CouponStatus.AVAILABLE) {
            throw new IllegalStateException("只有 AVAILABLE 状态的券可被过期，当前状态: " + this.status);
        }
        this.status = CouponStatus.EXPIRED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getTemplateId() { return templateId; }
    public long getUserId() { return userId; }
    public String getName() { return name; }
    public CouponType getType() { return type; }
    public long getThresholdCents() { return thresholdCents; }
    public Long getDiscountCents() { return discountCents; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public Long getMaxDiscountCents() { return maxDiscountCents; }
    public CouponStatus getStatus() { return status; }
    public Long getOrderId() { return orderId; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
}
