package com.hmall.inventory.api.dto;

/**
 * 占用项：skuId + quantity，与 api.yaml OccupyItem 一致。
 */
public record OccupyItemDto(Long skuId, Integer quantity) {}
