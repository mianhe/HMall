package com.hmall.order.application.event;

/**
 * 订单价格快照（用于出站事件 payload）。
 * 统一表达原价、活动优惠、券优惠、总优惠与实付金额，便于跨流程回溯。
 */
public record PricingSnapshot(
    long originalAmountCents,
    long activityDiscountAmountCents,
    long couponDiscountAmountCents,
    long discountAmountCents,
    long payableAmountCents
) {}
