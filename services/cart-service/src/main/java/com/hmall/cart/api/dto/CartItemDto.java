package com.hmall.cart.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 购物车项响应，与 api.yaml CartItemDto 一致。
 * 查询时包含 SKU 展示信息（skuName, skuPrice, skuImageUrl, available）。
 */
public record CartItemDto(
    Long cartItemId,
    Long skuId,
    Long relatedSkuId,
    int quantity,
    Instant addedAt,
    String skuName,
    BigDecimal skuPrice,
    String skuImageUrl,
    Boolean available,
    String productType,
    Long spuId,
    List<AvailableServiceDto> availableServices
) {
    public record AvailableServiceDto(
        Long serviceSpuId,
        String name,
        List<AvailableServiceSkuDto> bindings
    ) {}

    public record AvailableServiceSkuDto(Long bindingId, Long serviceSkuId, BigDecimal price) {}
}
