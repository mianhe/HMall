package com.hmall.inventory.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 占用库存请求，与 api.yaml OccupyRequest 一致。
 */
public record OccupyRequestDto(
    @NotNull(message = "orderId 不能为空")
    Long orderId,
    @NotNull(message = "items 不能为空")
    @Size(min = 1, message = "items 不能为空")
    @Valid
    List<OccupyItemDto> items
) {}
