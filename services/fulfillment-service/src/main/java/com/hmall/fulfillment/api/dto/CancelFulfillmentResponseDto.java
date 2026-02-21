package com.hmall.fulfillment.api.dto;

public record CancelFulfillmentResponseDto(
    Long orderId,
    int cancelledCount
) {}
