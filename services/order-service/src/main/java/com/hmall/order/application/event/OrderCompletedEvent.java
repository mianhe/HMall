package com.hmall.order.application.event;

import java.util.List;

/** 订单完成事件。Order 在 FulfillmentDelivered 后发布。payload 统一携带 couponId 与 pricingSnapshot。 */
public record OrderCompletedEvent(
    Long orderId,
    Long userId,
    Long totalAmountCents,
    Long couponId,
    PricingSnapshot pricingSnapshot,
    List<ItemSnapshot> items
) {}
