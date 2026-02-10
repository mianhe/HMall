package com.hmall.catalog.api.dto;

import java.util.List;

/**
 * 维度及选项列表项，与 catalog-api.yaml SpecDimensionWithOptions 一致。
 */
public record SpecDimensionWithOptionsDto(
    Long id,
    Long spuId,
    String name,
    Boolean required,
    Integer sortOrder,
    Boolean affectsAppearance,
    List<SpecOptionDto> options
) {}
