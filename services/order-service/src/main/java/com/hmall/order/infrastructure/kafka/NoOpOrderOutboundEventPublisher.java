package com.hmall.order.infrastructure.kafka;

import com.hmall.order.application.event.OrderCancelledEvent;
import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.event.OrderCreatedEvent;
import com.hmall.order.application.port.OrderOutboundEventPublisher;

/** Kafka 未启用时的占位实现，事件不发送。由 PortStubConfig 注册。 */
public class NoOpOrderOutboundEventPublisher implements OrderOutboundEventPublisher {

    @Override
    public void publish(OrderCreatedEvent event) { }

    @Override
    public void publish(OrderCancelledEvent event) { }

    @Override
    public void publish(OrderCompletedEvent event) { }
}
