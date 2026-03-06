package com.hmall.catalog.api.dto;

/**
 * 修改商品请求，与 catalog-api.yaml ProductUpdate 一致。
 * categoryId 可选，传则修改所属类目（须为叶子类目），不传则保持原类目。
 */
public record ProductUpdateDto(String name, String description, Long categoryId) {}
