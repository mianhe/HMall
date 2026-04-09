package com.hmall.order.application.event;

import java.util.List;

/**
 * 订单创建成功且库存占用成功。Order 发布，供审计、多流程分析使用。
 * payload 统一携带 couponId 与 pricingSnapshot，增强交易回溯能力。
 */
public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    Long totalAmountCents,
    Long couponId,
    PricingSnapshot pricingSnapshot,
    List<ItemSnapshot> items
) {}
