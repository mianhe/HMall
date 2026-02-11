package com.hmall.catalog.api.dto;

/**
 * 修改类别请求，与 catalog-api.yaml CategoryUpdate 一致。
 */
public record CategoryUpdateDto(String name, String description) {}
