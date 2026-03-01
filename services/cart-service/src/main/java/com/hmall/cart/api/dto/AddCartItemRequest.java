package com.hmall.cart.api.dto;

/**
 * 添加商品到购物车请求，与 api.yaml AddCartItemRequest 一致。
 */
public record AddCartItemRequest(Long skuId, Integer quantity, Long relatedSkuId) {}
