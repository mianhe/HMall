package com.hmall.promotion.api.dto;

import java.util.List;

public record PreviewSkuPriceResponse(
        List<SkuPricePreview> items
) {
    public record SkuPricePreview(
            Long skuId,
            long originalPriceCents,
            long activityDiscountCents,
            long activityPriceCents,
            String activityLabel,
            String ineligibleReason
    ) {
    }
}
