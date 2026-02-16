package com.hmall.catalog.api.dto;

/**
 * 创建规格维度请求，与 catalog-api.yaml SpecDimensionCreate 一致。
 */
public record SpecDimensionCreateDto(
    String name,
    Boolean required,
    Integer sortOrder
) {}
