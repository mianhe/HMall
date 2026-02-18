package com.hmall.payment.application.event;

import java.time.Instant;

/** 支付失败。网关回调失败时发布，Order 等可订阅。 */
public record PaymentFailedEvent(Long orderId, Instant occurredAt) {}
