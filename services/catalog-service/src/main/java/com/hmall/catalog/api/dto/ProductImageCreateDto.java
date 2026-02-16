package com.hmall.catalog.api.dto;

/**
 * 创建展示图请求，与 catalog-api.yaml ProductImageCreate 一致。
 */
public record ProductImageCreateDto(
    String imageUrl,
    Integer sortOrder
) {}
