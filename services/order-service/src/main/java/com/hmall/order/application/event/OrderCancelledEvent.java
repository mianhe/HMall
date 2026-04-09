package com.hmall.order.application.event;

import java.util.List;

/**
 * 订单取消领域事件。发布时机：CancelOrder 成功执行后。
 * payload 统一携带 couponId 与 pricingSnapshot，增强交易回溯能力。
 */
public record OrderCancelledEvent(
    Long orderId,
    Long userId,
    Long totalAmountCents,
    Long couponId,
    PricingSnapshot pricingSnapshot,
    List<ItemSnapshot> items
) {}
