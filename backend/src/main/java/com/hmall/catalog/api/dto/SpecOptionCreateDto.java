package com.hmall.catalog.api.dto;

/**
 * 创建规格选项请求，与 catalog-api.yaml SpecOptionCreate 一致。
 */
public record SpecOptionCreateDto(
    String optionValue,
    Integer sortOrder,
    String image
) {}
