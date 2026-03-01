package com.hmall.fulfillment.api.dto;

import com.hmall.fulfillment.domain.FulfillmentOrder;

import java.time.Instant;
import java.util.List;

public record FulfillmentOrderDto(
    Long fulfillmentOrderId,
    Long orderId,
    String fulfillmentType,
    String status,
    List<FulfillmentItemDto> items,
    ShippingAddressDto shippingAddress,
    ShippingInfoDto shippingInfo,
    Instant createdAt,
    Instant updatedAt
) {
    public static FulfillmentOrderDto from(FulfillmentOrder order) {
        List<FulfillmentItemDto> items = order.getItems().stream()
            .map(i -> new FulfillmentItemDto(
                i.getFulfillmentItemId(),
                i.getSkuId(),
                i.getQuantity(),
                i.getItemType().name()
            ))
            .toList();

        ShippingAddressDto address = new ShippingAddressDto(
            order.getShippingAddress().getRecipientName(),
            order.getShippingAddress().getPhone(),
            order.getShippingAddress().getProvince(),
            order.getShippingAddress().getCity(),
            order.getShippingAddress().getDistrict(),
            order.getShippingAddress().getDetail()
        );

        ShippingInfoDto shippingInfo = null;
        if (order.getShippingInfo() != null) {
            shippingInfo = new ShippingInfoDto(
                order.getShippingInfo().getCarrier(),
                order.getShippingInfo().getTrackingNumber(),
                order.getShippingInfo().getShippedAt(),
                order.getShippingInfo().getDeliveredAt()
            );
        }

        return new FulfillmentOrderDto(
            order.getFulfillmentOrderId(), order.getOrderId(),
            order.getFulfillmentType().name(),
            order.getStatus().name(), items, address, shippingInfo,
            order.getCreatedAt(), order.getUpdatedAt()
        );
    }

    public record FulfillmentItemDto(Long fulfillmentItemId, Long skuId, int quantity, String itemType) {}
    public record ShippingAddressDto(String recipientName, String phone,
                                     String province, String city, String district, String detail) {}
    public record ShippingInfoDto(String carrier, String trackingNumber,
                                  Instant shippedAt, Instant deliveredAt) {}
}
