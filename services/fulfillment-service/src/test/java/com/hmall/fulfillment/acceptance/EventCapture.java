package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.FulfillmentDelivered;
import com.hmall.fulfillment.domain.FulfillmentOrderAllocated;
import com.hmall.fulfillment.domain.FulfillmentOrderCreated;
import com.hmall.fulfillment.domain.FulfillmentShipped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventCapture implements DomainEventPublisher {

    private final List<FulfillmentOrderCreated> createdEvents = new CopyOnWriteArrayList<>();
    private final List<FulfillmentOrderAllocated> allocatedEvents = new CopyOnWriteArrayList<>();
    private final List<FulfillmentShipped> shippedEvents = new CopyOnWriteArrayList<>();
    private final List<FulfillmentDelivered> deliveredEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(FulfillmentOrderCreated event) {
        createdEvents.add(event);
    }

    @Override
    public void publish(FulfillmentOrderAllocated event) {
        allocatedEvents.add(event);
    }

    @Override
    public void publish(FulfillmentShipped event) {
        shippedEvents.add(event);
    }

    @Override
    public void publish(FulfillmentDelivered event) {
        deliveredEvents.add(event);
    }

    public List<FulfillmentOrderCreated> getCreatedEvents() { return new ArrayList<>(createdEvents); }
    public List<FulfillmentOrderAllocated> getAllocatedEvents() { return new ArrayList<>(allocatedEvents); }
    public List<FulfillmentShipped> getShippedEvents() { return new ArrayList<>(shippedEvents); }
    public List<FulfillmentDelivered> getDeliveredEvents() { return new ArrayList<>(deliveredEvents); }

    public void clear() {
        createdEvents.clear();
        allocatedEvents.clear();
        shippedEvents.clear();
        deliveredEvents.clear();
    }
}
