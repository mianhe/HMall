package com.hmall.order.application.port;

import com.hmall.order.application.event.OrderCancelledEvent;
import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.event.OrderCreatedEvent;

/**
 * Order 出站领域事件发布端口。生产实现发往 Kafka；测试用替身记录供断言，无需真实 Kafka。
 */
public interface OrderOutboundEventPublisher {

    void publish(OrderCreatedEvent event);

    void publish(OrderCancelledEvent event);

    void publish(OrderCompletedEvent event);
}
