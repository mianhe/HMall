package com.hmall.catalog.api.dto;

/**
 * 创建商品请求，与 catalog-api.yaml ProductCreate 一致。
 */
public record ProductCreateDto(Long categoryId, String name, String description) {}
