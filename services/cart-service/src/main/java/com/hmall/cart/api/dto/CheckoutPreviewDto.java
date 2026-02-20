package com.hmall.cart.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算预览响应。
 */
public record CheckoutPreviewDto(List<Item> items, BigDecimal totalPrice) {

    public record Item(
        Long cartItemId,
        Long skuId,
        String skuName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
    ) {}
}
