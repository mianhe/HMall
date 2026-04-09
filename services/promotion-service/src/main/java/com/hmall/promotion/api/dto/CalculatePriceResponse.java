package com.hmall.promotion.api.dto;

import java.util.List;

public record CalculatePriceResponse(
        long originalAmountCents,
        long activityDiscountAmountCents,
        long couponDiscountAmountCents,
        long discountAmountCents,
        long payableAmountCents,
        List<LineItemDiscount> lineItems,
        List<ActivityExplanation> activityExplanations
) {
    public record LineItemDiscount(
            Long skuId,
            long unitPriceCents,
            int quantity,
            long totalPriceCents,
            List<DiscountDetailDto> discounts
    ) {}

    public record DiscountDetailDto(
            String type,
            Long sourceId,
            long amountCents,
            String description
    ) {}

    public record ActivityExplanation(
            Long activityId,
            String activityName,
            boolean applied,
            String message
    ) {
    }
}
