package com.hmall.order.application.event;

/** 履约已发货事件。由 Fulfillment BC 发布，Order 订阅后置 fulfillmentStatus SHIPPED。 */
public record FulfillmentShippedEvent(Long orderId) {}
