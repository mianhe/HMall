package com.hmall.order.application.event;

import java.util.List;

/** 订单创建成功且库存占用成功。Order 发布，供审计、未来消费者使用。 */
public record OrderCreatedEvent(Long orderId, List<ItemQuantity> items) {

    public record ItemQuantity(long skuId, int quantity) {}
}
