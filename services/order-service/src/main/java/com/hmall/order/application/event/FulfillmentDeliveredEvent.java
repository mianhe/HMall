package com.hmall.order.application.event;

/** 履约已签收事件。由 Fulfillment BC 发布，Order 订阅后置 DELIVERED 并发布 OrderCompleted。 */
public record FulfillmentDeliveredEvent(Long orderId) {}
