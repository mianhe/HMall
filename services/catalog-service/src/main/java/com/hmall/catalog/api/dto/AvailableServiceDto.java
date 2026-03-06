package com.hmall.catalog.api.dto;

import java.util.List;

public record AvailableServiceDto(
    Long serviceSpuId,
    String name,
    String description,
    String productType,
    String serviceKind,
    List<AvailableServiceSkuDto> bindings
) {}
