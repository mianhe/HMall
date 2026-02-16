package com.hmall.order.application.event;

import java.util.List;

/** 履约单已创建事件。由 Fulfillment BC 发布，Order 订阅后更新 fulfillmentRef 并置 FULFILLING。 */
public record FulfillmentOrderCreatedEvent(Long orderId, List<Long> fulfillmentOrderIds) {}
