package com.hmall.fulfillment.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ShipRequestDto(
    @NotBlank(message = "carrier 不能为空")
    String carrier,
    @NotBlank(message = "trackingNumber 不能为空")
    String trackingNumber
) {}
