package com.hmall.promotion.api.dto;

import com.hmall.promotion.domain.Coupon;
import com.hmall.promotion.domain.CouponStatus;
import com.hmall.promotion.domain.CouponType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponDto(
        Long id,
        long templateId,
        long userId,
        String name,
        CouponType type,
        long thresholdCents,
        Long discountCents,
        BigDecimal discountRate,
        Long maxDiscountCents,
        CouponStatus status,
        Long orderId,
        Instant issuedAt,
        Instant expiresAt,
        Instant usedAt
) {
    public static CouponDto from(Coupon c) {
        return new CouponDto(
                c.getId(), c.getTemplateId(), c.getUserId(), c.getName(),
                c.getType(), c.getThresholdCents(), c.getDiscountCents(),
                c.getDiscountRate(), c.getMaxDiscountCents(), c.getStatus(),
                c.getOrderId(), c.getIssuedAt(), c.getExpiresAt(), c.getUsedAt()
        );
    }
}
