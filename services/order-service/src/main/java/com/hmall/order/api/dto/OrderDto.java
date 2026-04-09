package com.hmall.order.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OrderDto(
    Long orderId,
    String status,
    Long totalAmountCents,
    Long couponId,
    List<OrderLineItemDto> items,
    OrderCreateDto.ShippingAddressDto shippingAddress,
    Instant createdAt,
    boolean serviceActivated
) {
    public record OrderLineItemDto(
        Long lineItemId,
        Long skuId,
        Integer quantity,
        Long unitPriceCents,
        Long totalPriceCents,
        String displayName,
        String itemType,
        Map<String, Object> serviceAttributes,
        List<DiscountDetailDto> discounts
    ) {}

    public record DiscountDetailDto(
        String type,
        Long sourceId,
        long amountCents,
        String description
    ) {}
}
