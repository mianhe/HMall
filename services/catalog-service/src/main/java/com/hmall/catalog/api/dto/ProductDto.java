package com.hmall.catalog.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ProductDto(
    Long id,
    Long categoryId,
    String name,
    String description,
    String productType,
    String serviceKind,
    String coverImageUrl,
    List<ProductImageDto> defaultDisplayImages,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<SkuDto> skus
) {
    public ProductDto(Long id, Long categoryId, String name, String description,
                      String productType, String serviceKind,
                      String coverImageUrl, List<ProductImageDto> defaultDisplayImages) {
        this(id, categoryId, name, description, productType, serviceKind,
             coverImageUrl, defaultDisplayImages, null);
    }
}
