package com.hmall.order.application.event;

import java.util.List;

/** 订单完成事件。Order 在 FulfillmentDelivered 后发布。payload 含 userId、总金额、行快照（智能运营 Step 1）。 */
public record OrderCompletedEvent(Long orderId, Long userId, Long totalAmountCents, List<ItemSnapshot> items) {}
