package com.hmall.catalog.api.dto;

/**
 * 展示图响应，与 catalog-api.yaml OptionImage 一致。
 */
public record OptionImageDto(
    Long id,
    Long specOptionId,
    String imageUrl,
    Integer sortOrder
) {}
