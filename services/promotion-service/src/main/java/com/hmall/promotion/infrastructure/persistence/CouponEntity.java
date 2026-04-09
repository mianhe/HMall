package com.hmall.promotion.infrastructure.persistence;

import com.hmall.promotion.domain.Coupon;
import com.hmall.promotion.domain.CouponStatus;
import com.hmall.promotion.domain.CouponType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long templateId;

    @Column(nullable = false)
    private long userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @Column(nullable = false)
    private long thresholdCents;

    private Long discountCents;
    private BigDecimal discountRate;
    private Long maxDiscountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    private Long orderId;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    protected CouponEntity() {}

    public static CouponEntity fromDomain(Coupon c) {
        CouponEntity e = new CouponEntity();
        e.id = c.getId();
        e.templateId = c.getTemplateId();
        e.userId = c.getUserId();
        e.name = c.getName();
        e.type = c.getType();
        e.thresholdCents = c.getThresholdCents();
        e.discountCents = c.getDiscountCents();
        e.discountRate = c.getDiscountRate();
        e.maxDiscountCents = c.getMaxDiscountCents();
        e.status = c.getStatus();
        e.orderId = c.getOrderId();
        e.issuedAt = c.getIssuedAt();
        e.expiresAt = c.getExpiresAt();
        e.usedAt = c.getUsedAt();
        return e;
    }

    public Coupon toDomain() {
        return new Coupon(
                id, templateId, userId, name, type, thresholdCents,
                discountCents, discountRate, maxDiscountCents,
                status, orderId, issuedAt, expiresAt, usedAt
        );
    }

    public Long getId() { return id; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
