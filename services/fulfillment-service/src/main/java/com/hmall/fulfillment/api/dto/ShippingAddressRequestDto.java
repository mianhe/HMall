package com.hmall.fulfillment.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressRequestDto(
    @NotBlank(message = "recipientName 不能为空")
    String recipientName,
    @NotBlank(message = "phone 不能为空")
    String phone,
    @NotBlank(message = "province 不能为空")
    String province,
    @NotBlank(message = "city 不能为空")
    String city,
    @NotBlank(message = "district 不能为空")
    String district,
    @NotBlank(message = "detail 不能为空")
    String detail
) {}
