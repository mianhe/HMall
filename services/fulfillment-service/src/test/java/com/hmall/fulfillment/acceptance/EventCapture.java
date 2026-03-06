package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.EngravingCompleted;
import com.hmall.fulfillment.domain.FulfillmentDelivered;
import com.hmall.fulfillment.domain.FulfillmentOrderAllocated;
import com.hmall.fulfillment.domain.FulfillmentOrderCreated;
import com.hmall.fulfillment.domain.FulfillmentShipped;
import com.hmall.fulfillment.domain.ServiceActivated;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventCapture implements DomainEventPublisher {

    private final List<FulfillmentOrderCreated> createdEvents = new CopyOnWriteArrayList<>();
    private final List<FulfillmentOrderAllocated> allocatedEvents = new CopyOnWriteArrayList<>();
    private final List<FulfillmentShipped> shippedEvents = new CopyOnWriteArrayList<>();
    private final List<FulfillmentDelivered> deliveredEvents = new CopyOnWriteArrayList<>();
    private final List<ServiceActivated> serviceActivatedEvents = new CopyOnWriteArrayList<>();
    private final List<EngravingCompleted> engravingCompletedEvents = new CopyOnWriteArrayList<>();

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

    @Override
    public void publish(ServiceActivated event) {
        serviceActivatedEvents.add(event);
    }

    @Override
    public void publish(EngravingCompleted event) {
        engravingCompletedEvents.add(event);
    }

    public List<FulfillmentOrderCreated> getCreatedEvents() { return new ArrayList<>(createdEvents); }
    public List<FulfillmentOrderAllocated> getAllocatedEvents() { return new ArrayList<>(allocatedEvents); }
    public List<FulfillmentShipped> getShippedEvents() { return new ArrayList<>(shippedEvents); }
    public List<FulfillmentDelivered> getDeliveredEvents() { return new ArrayList<>(deliveredEvents); }
    public List<ServiceActivated> getServiceActivatedEvents() { return new ArrayList<>(serviceActivatedEvents); }
    public List<EngravingCompleted> getEngravingCompletedEvents() { return new ArrayList<>(engravingCompletedEvents); }

    public void clear() {
        createdEvents.clear();
        allocatedEvents.clear();
        shippedEvents.clear();
        deliveredEvents.clear();
        serviceActivatedEvents.clear();
        engravingCompletedEvents.clear();
    }
}
