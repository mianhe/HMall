package com.hmall.order.application.event;

/** 支付失败事件。由 Payment BC 发布，Order 订阅后取消订单。 */
public record PaymentFailedEvent(Long orderId) {}
