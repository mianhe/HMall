package com.hmall.order.infrastructure.promotion;

import com.hmall.order.application.port.PromotionPricePort;

import java.util.List;

public class NoOpPromotionPriceAdapter implements PromotionPricePort {

    @Override
    public PriceResult calculatePrice(List<LineItem> items, Long userId, Long couponId) {
        long total = items.stream().mapToLong(i -> i.unitPriceCents() * i.quantity()).sum();
        List<LineDiscount> lineDiscounts = items.stream()
                .map(i -> new LineDiscount(i.skuId(), List.of()))
                .toList();
        return new PriceResult(total, 0, total, lineDiscounts);
    }
}
