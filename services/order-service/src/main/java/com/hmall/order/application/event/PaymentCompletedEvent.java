package com.hmall.order.application.event;

/** 支付完成事件。由 Payment BC 发布，Order 订阅后置 PAID 并创建履约单。 */
public record PaymentCompletedEvent(Long orderId, Long paymentId) {}
