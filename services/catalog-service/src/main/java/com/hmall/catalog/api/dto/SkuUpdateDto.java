package com.hmall.catalog.api.dto;

/**
 * 修改 SKU 请求，与 catalog-api.yaml SkuUpdate 一致。
 */
public record SkuUpdateDto(Long priceCents, String displayName) {}
