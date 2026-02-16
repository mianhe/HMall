package com.hmall.order.acceptance;

import com.hmall.order.application.event.OrderCancelledEvent;
import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.event.OrderCreatedEvent;
import com.hmall.order.application.port.OrderOutboundEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用 Order 出站事件捕获器，实现与 Inventory 的 EventCapture 一致：实现发布端口并记录事件，供 Step Definitions 断言，无需 Kafka。
 */
public class OrderEventCapture implements OrderOutboundEventPublisher {

    private final List<OrderCreatedEvent> orderCreatedEvents = new CopyOnWriteArrayList<>();
    private final List<OrderCancelledEvent> orderCancelledEvents = new CopyOnWriteArrayList<>();
    private final List<OrderCompletedEvent> orderCompletedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(OrderCreatedEvent event) {
        orderCreatedEvents.add(event);
    }

    @Override
    public void publish(OrderCancelledEvent event) {
        orderCancelledEvents.add(event);
    }

    @Override
    public void publish(OrderCompletedEvent event) {
        orderCompletedEvents.add(event);
    }

    public List<OrderCreatedEvent> getOrderCreatedEvents() {
        return new ArrayList<>(orderCreatedEvents);
    }

    public List<OrderCancelledEvent> getOrderCancelledEvents() {
        return new ArrayList<>(orderCancelledEvents);
    }

    public List<OrderCompletedEvent> getOrderCompletedEvents() {
        return new ArrayList<>(orderCompletedEvents);
    }

    public boolean wasOrderCreatedPublished() {
        return !orderCreatedEvents.isEmpty();
    }

    public boolean wasOrderCancelledPublished() {
        return !orderCancelledEvents.isEmpty();
    }

    public boolean wasOrderCompletedPublished() {
        return !orderCompletedEvents.isEmpty();
    }

    public void clear() {
        orderCreatedEvents.clear();
        orderCancelledEvents.clear();
        orderCompletedEvents.clear();
    }
}
