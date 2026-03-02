package com.hmall.order.api.dto;

public record PurchasableServiceDto(
    Long serviceSkuId,
    String serviceName,
    long priceCents,
    Long relatedSkuId
) {}
