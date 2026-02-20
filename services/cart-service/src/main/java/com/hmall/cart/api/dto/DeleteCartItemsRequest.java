package com.hmall.cart.api.dto;

import java.util.List;

/**
 * 批量删除购物车项请求，与 api.yaml DeleteCartItemsRequest 一致。
 */
public record DeleteCartItemsRequest(List<Long> cartItemIds) {}
