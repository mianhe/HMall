package com.hmall.catalog.api.dto;

public record ServiceBindingDto(Long id, Long serviceSkuId, Long targetSpuId, Long priceCents, String targetSpuName) {}
