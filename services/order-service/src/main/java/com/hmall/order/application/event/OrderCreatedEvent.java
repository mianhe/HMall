package com.hmall.order.application.event;

import java.util.List;

/** 订单创建成功且库存占用成功。Order 发布，供审计、多流程分析使用。payload 含 userId、总金额、行快照（智能运营 Step 1）。 */
public record OrderCreatedEvent(Long orderId, Long userId, Long totalAmountCents, List<ItemSnapshot> items) {}
