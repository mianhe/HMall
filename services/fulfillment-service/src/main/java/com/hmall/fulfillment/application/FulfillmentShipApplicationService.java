package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.FulfillmentShipped;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class FulfillmentShipApplicationService {

    private final FulfillmentOrderRepository repository;
    private final DomainEventPublisher eventPublisher;

    public FulfillmentShipApplicationService(FulfillmentOrderRepository repository,
                                             DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void ship(Long fulfillmentOrderId, String carrier, String trackingNumber) {
        FulfillmentOrder order = repository.findById(fulfillmentOrderId)
            .orElseThrow(() -> new FulfillmentNotFoundException("履约单不存在: " + fulfillmentOrderId));

        try {
            order.ship(carrier, trackingNumber);
        } catch (IllegalStateException e) {
            throw new FulfillmentBadRequestException(e.getMessage());
        }

        repository.save(order);
        eventPublisher.publish(new FulfillmentShipped(order.getOrderId(), fulfillmentOrderId, Instant.now()));
    }
}
