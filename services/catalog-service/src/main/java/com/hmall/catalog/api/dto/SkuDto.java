package com.hmall.catalog.api.dto;

import java.util.List;

/**
 * SKU 响应，与 catalog-api.yaml Sku 一致。
 */
public record SkuDto(
    Long id,
    Long spuId,
    Long priceCents,
    String displayName,
    List<SkuSpecValueDto> specValues
) {}
