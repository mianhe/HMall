package com.hmall.catalog.api.dto;

/**
 * 创建展示图请求，与 catalog-api.yaml OptionImageCreate 一致。
 */
public record OptionImageCreateDto(
    String imageUrl,
    Integer sortOrder
) {}
