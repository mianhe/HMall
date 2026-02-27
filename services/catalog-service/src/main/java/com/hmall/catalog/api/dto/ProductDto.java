package com.hmall.catalog.api.dto;

import java.util.List;

public record ProductDto(
    Long id,
    Long categoryId,
    String name,
    String description,
    String productType,
    String coverImageUrl,
    List<ProductImageDto> defaultDisplayImages
) {}
