package com.hmall.cart.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算预览：选中的购物车项摘要及总价。
 */
public record CheckoutPreview(List<Item> items, List<Group> groups, BigDecimal totalPrice) {

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
