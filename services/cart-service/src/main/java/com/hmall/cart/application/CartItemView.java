package com.hmall.cart.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 购物车项视图（含 SKU 展示信息和可用性标记）。
 */
public record CartItemView(
    Long cartItemId,
    Long skuId,
    Long relatedSkuId,
    int quantity,
    Instant addedAt,
    String skuName,
    BigDecimal skuPrice,
    String skuImageUrl,
    boolean available,
    String productType,
    Long spuId,
    List<AvailableServiceView> availableServices
) {}
