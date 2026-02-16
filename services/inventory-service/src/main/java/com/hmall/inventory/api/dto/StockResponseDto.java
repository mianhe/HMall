package com.hmall.inventory.api.dto;

/**
 * 库存查询/更新返回，与 api.yaml StockResponse 一致。
 */
public record StockResponseDto(long skuId, int available, int reserved) {}
