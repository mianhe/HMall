package com.hmall.payment.application.event;

import java.time.Instant;

/** 支付完成。网关回调成功时发布，Order 等可订阅。 */
public record PaymentCompletedEvent(Long orderId, Long paymentId, Long amountCents, Instant occurredAt) {}
