package com.hmall.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record OrderCreateDto(
    @NotNull Long userId,
    @NotEmpty(message = "商品明细不能为空") @Valid List<LineItemCreateDto> items,
    @Valid ShippingAddressDto shippingAddress
) {
    public record LineItemCreateDto(
        @NotNull Long skuId,
        @NotNull Integer quantity,
        Long relatedSkuId,
        Map<String, Object> serviceAttributes
    ) {}

    public record ShippingAddressDto(
        @NotBlank(message = "收货人不能为空") String recipientName,
        @NotBlank(message = "电话不能为空") String phone,
        @NotBlank(message = "省份不能为空") String province,
        @NotBlank(message = "城市不能为空") String city,
        @NotBlank(message = "区县不能为空") String district,
        @NotBlank(message = "详细地址不能为空") String detail
    ) {}
}
