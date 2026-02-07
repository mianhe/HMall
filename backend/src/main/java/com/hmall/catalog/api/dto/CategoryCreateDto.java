package com.hmall.catalog.api.dto;

/**
 * 创建类别请求，与 catalog-api.yaml CategoryCreate 一致。
 */
public record CategoryCreateDto(String name, String description, Long parentId) {}
