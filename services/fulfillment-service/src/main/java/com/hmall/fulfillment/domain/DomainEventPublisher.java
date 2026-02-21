package com.hmall.fulfillment.domain;

public interface DomainEventPublisher {

    void publish(FulfillmentOrderCreated event);

    void publish(FulfillmentOrderAllocated event);

    void publish(FulfillmentShipped event);

    void publish(FulfillmentDelivered event);
}
