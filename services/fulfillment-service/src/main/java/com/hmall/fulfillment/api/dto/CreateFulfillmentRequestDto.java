package com.hmall.fulfillment.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateFulfillmentRequestDto(
    @NotNull(message = "orderId 不能为空")
    Long orderId,
    @NotNull(message = "items 不能为空")
    @Size(min = 1, message = "items 不能为空")
    @Valid
    List<FulfillmentItemRequestDto> items,
    @NotNull(message = "shippingAddress 不能为空")
    @Valid
    ShippingAddressRequestDto shippingAddress
) {}
