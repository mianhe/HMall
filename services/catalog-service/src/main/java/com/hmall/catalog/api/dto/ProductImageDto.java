package com.hmall.catalog.api.dto;

/**
 * 产品展示图响应，与 catalog-api.yaml ProductImage 一致。
 */
public record ProductImageDto(
    Long id,
    Long spuId,
    Long specOptionId,
    String imageUrl,
    Integer sortOrder
) {}
