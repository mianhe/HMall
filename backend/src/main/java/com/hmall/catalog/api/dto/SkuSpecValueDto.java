package com.hmall.catalog.api.dto;

/**
 * SKU 规格展示项，与 catalog-api.yaml Sku.specValues[]. 一致。
 */
public record SkuSpecValueDto(String dimensionName, String optionValue) {}
