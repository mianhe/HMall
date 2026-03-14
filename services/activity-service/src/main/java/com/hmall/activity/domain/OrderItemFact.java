package com.hmall.activity.domain;

import java.time.LocalDate;

public record OrderItemFact(
    Long id,
    Long orderId,
    Long userId,
    Long skuId,
    Long spuId,
    int quantity,
    long unitPriceCents,
    long lineTotalCents,
    long orderTotalAmountCents,
    String orderCurrentStage,
    boolean orderHasEngraving,
    boolean orderHasWarranty,
    LocalDate createdDate,
    String seedBatch
) {}
