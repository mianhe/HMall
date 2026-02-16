package com.hmall.catalog.api.dto;

import java.util.List;

/**
 * 创建 SKU 请求，与 catalog-api.yaml SkuCreate 一致。
 */
public record SkuCreateDto(
    List<Long> specOptionIds,
    Long priceCents,
    String displayName
) {}
