package com.hmall.inventory.api.dto;

import jakarta.validation.constraints.Min;

/**
 * 初始化/更新库存请求，与 api.yaml StockUpdateRequest 一致。
 */
public record StockUpdateRequestDto(
    @Min(value = 0, message = "available 不能为负")
    int available
) {}
