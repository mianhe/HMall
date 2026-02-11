package com.hmall.catalog.api.dto;

/**
 * 修改商品请求，与 catalog-api.yaml ProductUpdate 一致。
 */
public record ProductUpdateDto(String name, String description) {}
