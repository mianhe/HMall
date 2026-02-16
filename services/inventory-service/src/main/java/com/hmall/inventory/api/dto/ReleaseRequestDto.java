package com.hmall.inventory.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 释放库存请求，与 api.yaml ReleaseRequest 一致。
 */
public record ReleaseRequestDto(
    @NotNull(message = "orderId 不能为空")
    Long orderId
) {}
