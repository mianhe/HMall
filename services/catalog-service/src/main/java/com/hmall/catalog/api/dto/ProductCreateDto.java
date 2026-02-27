package com.hmall.catalog.api.dto;

public record ProductCreateDto(
    Long categoryId,
    String name,
    String description,
    String productType
) {}
