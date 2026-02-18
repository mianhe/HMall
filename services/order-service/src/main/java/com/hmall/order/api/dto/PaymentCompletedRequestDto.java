package com.hmall.order.api.dto;

/** Payment 回调成功后通知 Order 的请求体。 */
public record PaymentCompletedRequestDto(Long orderId, Long paymentId) {}
