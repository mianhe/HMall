package com.hmall.order.application.port;

import com.hmall.order.domain.DiscountDetail;

import java.util.List;

public interface PromotionPricePort {

    PriceResult calculatePrice(List<LineItem> items, Long userId, Long couponId);

    record LineItem(Long skuId, long unitPriceCents, int quantity) {}

    record PriceResult(
            long originalAmountCents,
            long discountAmountCents,
            long payableAmountCents,
            List<LineDiscount> lineItems
    ) {}

    record LineDiscount(Long skuId, List<DiscountDetail> discounts) {}
}
