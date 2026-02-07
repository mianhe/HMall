package com.hmall.catalog.api.dto;

/**
 * 商品响应，与 catalog-api.yaml Product 一致。
 */
public record ProductDto(Long id, Long categoryId, String name, String description) {}
