package com.hmall.activity.domain;

import java.time.LocalDate;
import java.util.List;

public record ProductRanking(
    String rankBy,
    List<ProductRankingItem> items,
    LocalDate from,
    LocalDate to
) {
    public record ProductRankingItem(
        Long spuId,
        Long skuId,
        int totalQuantity,
        long totalRevenueCents,
        int orderCount,
        int cancelledOrderCount
    ) {}
}
