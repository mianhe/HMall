package com.hmall.fulfillment.api.dto;

import java.util.List;

public record CreateFulfillmentResponseDto(
    Long orderId,
    List<Long> fulfillmentOrderIds
) {}
