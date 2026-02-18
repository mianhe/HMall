package com.hmall.payment.application.event;

import java.time.Instant;

/** 支付超时。超时检测将未支付单置为 EXPIRED 时发布，Order 等可订阅。 */
public record PaymentExpiredEvent(Long orderId, Instant occurredAt) {}
