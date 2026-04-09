package com.hmall.promotion.api.dto;

import com.hmall.promotion.domain.CouponTemplate;
import com.hmall.promotion.domain.CouponType;
import com.hmall.promotion.domain.TemplateStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record CouponTemplateDto(
        Long id,
        String name,
        CouponType type,
        long thresholdCents,
        Long discountCents,
        BigDecimal discountRate,
        Long maxDiscountCents,
        int totalQuantity,
        int issuedQuantity,
        int perUserLimit,
        int validDays,
        TargetingRule targetingRule,
        TemplateStatus status,
        Instant createdAt
) {
    public static CouponTemplateDto from(CouponTemplate t) {
        return new CouponTemplateDto(
                t.getId(), t.getName(), t.getType(), t.getThresholdCents(),
                t.getDiscountCents(), t.getDiscountRate(), t.getMaxDiscountCents(),
                t.getTotalQuantity(), t.getIssuedQuantity(), t.getPerUserLimit(),
                t.getValidDays(), toTargetingRule(t.getTargetingRule()), t.getStatus(), t.getCreatedAt()
        );
    }

    private static TargetingRule toTargetingRule(CouponTemplate.TargetingRule source) {
        if (source == null) {
            return null;
        }
        return new TargetingRule(source.levelsIn(), source.tagsAny(), source.tagsAll(), source.excludeTags());
    }

    public record TargetingRule(
            Set<String> levelsIn,
            Set<String> tagsAny,
            Set<String> tagsAll,
            Set<String> excludeTags
    ) {
    }
}
