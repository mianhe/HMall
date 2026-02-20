package com.hmall.cart.api.dto;

/**
 * 修改购物车项数量请求，与 api.yaml UpdateCartItemRequest 一致。
 */
public record UpdateCartItemRequest(Integer quantity) {}
