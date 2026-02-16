package com.hmall.order.api.dto;

import java.util.List;

public record OrderDto(
    Long orderId,
    String status,
    Long totalAmountCents,
    List<OrderLineItemDto> items,
    OrderCreateDto.ShippingAddressDto shippingAddress
) {
    public record OrderLineItemDto(
        Long lineItemId,
        Long skuId,
        Integer quantity,
        Long unitPriceCents,
        Long totalPriceCents,
        String displayName
    ) {}
}
