package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.FulfillmentDelivered;
import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class FulfillmentDeliverApplicationService {

    private final FulfillmentOrderRepository repository;
    private final DomainEventPublisher eventPublisher;

    public FulfillmentDeliverApplicationService(FulfillmentOrderRepository repository,
                                                DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void deliver(Long fulfillmentOrderId) {
        FulfillmentOrder order = repository.findById(fulfillmentOrderId)
            .orElseThrow(() -> new FulfillmentNotFoundException("履约单不存在: " + fulfillmentOrderId));

        try {
            order.confirmDelivery();
        } catch (IllegalStateException e) {
            throw new FulfillmentBadRequestException(e.getMessage());
        }

        repository.save(order);
        eventPublisher.publish(new FulfillmentDelivered(order.getOrderId(), fulfillmentOrderId, Instant.now()));
    }
}
