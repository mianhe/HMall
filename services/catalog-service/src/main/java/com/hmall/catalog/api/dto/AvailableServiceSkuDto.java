package com.hmall.catalog.api.dto;

import java.util.List;

public record AvailableServiceSkuDto(
    Long bindingId,
    Long serviceSkuId,
    Long priceCents,
    List<SkuSpecValueDto> specValues
) {}
