package com.hmall.catalog.api.dto;

/**
 * 类别响应，与 catalog-api.yaml Category 一致。
 */
public record CategoryDto(Long id, Long parentId, String name, String description) {}
