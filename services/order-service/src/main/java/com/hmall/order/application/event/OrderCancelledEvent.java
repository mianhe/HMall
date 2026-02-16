package com.hmall.order.application.event;

/**
 * 订单取消领域事件。发布时机：CancelOrder 成功执行后。
 */
public record OrderCancelledEvent(Long orderId) {}
