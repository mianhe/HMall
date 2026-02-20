package com.hmall.cart.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 购物车项响应，与 api.yaml CartItemDto 一致。
 * 查询时包含 SKU 展示信息（skuName, skuPrice, skuImageUrl, available）。
 */
public record CartItemDto(
    Long cartItemId,
    Long skuId,
    int quantity,
    Instant addedAt,
    String skuName,
    BigDecimal skuPrice,
    String skuImageUrl,
    Boolean available
) {}
