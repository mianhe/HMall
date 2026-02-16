package com.hmall.order.application.event;

/** 订单完成事件。Order 在 FulfillmentDelivered 后发布。 */
public record OrderCompletedEvent(Long orderId) {}
