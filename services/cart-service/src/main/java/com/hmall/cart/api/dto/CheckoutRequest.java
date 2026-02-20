package com.hmall.cart.api.dto;

import java.util.List;

/**
 * 结算预览请求：选中的购物车项 ID 列表。
 */
public record CheckoutRequest(List<Long> cartItemIds) {}
