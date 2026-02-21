package com.hmall.catalog.api.dto;

import java.util.List;

/**
 * 商品响应，与 catalog-api.yaml Product 一致。
 */
public record ProductDto(
    Long id,
    Long categoryId,
    String name,
    String description,
    String coverImageUrl,
    List<ProductImageDto> defaultDisplayImages
) {}
