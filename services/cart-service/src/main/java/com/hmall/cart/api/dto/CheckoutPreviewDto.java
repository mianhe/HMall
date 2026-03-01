package com.hmall.cart.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算预览响应。
 */
public record CheckoutPreviewDto(List<Item> items, List<Group> groups, BigDecimal totalPrice) {

    public record Item(
        Long cartItemId,
        Long skuId,
        Long relatedSkuId,
        String productType,
        String skuName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
    ) {}

    public record Group(
        Long primaryCartItemId,
        Long primarySkuId,
        String primarySkuName,
        List<Item> serviceItems,
        BigDecimal groupSubtotal
    ) {}
}
