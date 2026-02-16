package com.hmall.catalog.api.dto;

import java.util.List;

/**
 * 规格选项响应，与 catalog-api.yaml SpecOption 一致。
 */
public record SpecOptionDto(
    Long id,
    Long specDimensionId,
    String optionValue,
    Integer sortOrder,
    List<OptionImageDto> images
) {}
