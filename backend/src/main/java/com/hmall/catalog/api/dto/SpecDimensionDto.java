package com.hmall.catalog.api.dto;

/**
 * 规格维度响应，与 catalog-api.yaml SpecDimension 一致。
 */
public record SpecDimensionDto(
    Long id,
    Long spuId,
    String name,
    Boolean required,
    Integer sortOrder,
    Boolean affectsAppearance
) {}
