package com.hmall.order.application.event;

import java.util.List;

/**
 * 订单取消领域事件。发布时机：CancelOrder 成功执行后。payload 含 userId、总金额、行快照（智能运营 Step 1）。
 */
public record OrderCancelledEvent(Long orderId, Long userId, Long totalAmountCents, List<ItemSnapshot> items) {}
