package com.hmall.order.application.event;

/** 支付超时事件。由 Payment BC 发布，Order 订阅后取消订单。 */
public record PaymentExpiredEvent(Long orderId) {}
