package com.hmall.order.application;

import com.hmall.order.application.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListeners {

    private final OrderEventService orderEventService;

    public OrderEventListeners(OrderEventService orderEventService) {
        this.orderEventService = orderEventService;
    }

    @EventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orderEventService.onPaymentCompleted(event.orderId(), event.paymentId());
    }

    @EventListener
    public void onPaymentFailed(PaymentFailedEvent event) {
        orderEventService.onPaymentFailed(event.orderId());
    }

    @EventListener
    public void onPaymentExpired(PaymentExpiredEvent event) {
        orderEventService.onPaymentExpired(event.orderId());
    }

    @EventListener
    public void onFulfillmentOrderCreated(FulfillmentOrderCreatedEvent event) {
        orderEventService.onFulfillmentOrderCreated(event.orderId(), event.fulfillmentOrderIds());
    }

    @EventListener
    public void onFulfillmentShipped(FulfillmentShippedEvent event) {
        orderEventService.onFulfillmentShipped(event.orderId());
    }

    @EventListener
    public void onFulfillmentDelivered(FulfillmentDeliveredEvent event) {
        orderEventService.onFulfillmentDelivered(event.orderId());
    }
}
