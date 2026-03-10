package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.EngravingCompleted;
import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.ServiceActivated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class FulfillmentCompleteEngravingApplicationService {

    private final FulfillmentOrderRepository repository;
    private final DomainEventPublisher eventPublisher;

    public FulfillmentCompleteEngravingApplicationService(FulfillmentOrderRepository repository,
                                                          DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void completeEngraving(Long fulfillmentOrderId) {
        FulfillmentOrder order = repository.findById(fulfillmentOrderId)
            .orElseThrow(() -> new FulfillmentNotFoundException("履约单不存在: " + fulfillmentOrderId));

        try {
            order.completeEngraving();
        } catch (IllegalStateException e) {
            throw new FulfillmentBadRequestException(e.getMessage());
        }

        repository.save(order);

        Instant occurredAt = order.getEngravingCompletedAt() != null ? order.getEngravingCompletedAt() : Instant.now();
        eventPublisher.publish(new EngravingCompleted(order.getOrderId(), fulfillmentOrderId, occurredAt));
        eventPublisher.publish(new ServiceActivated(
                order.getOrderId(), fulfillmentOrderId, 0L, occurredAt, null, occurredAt));
    }
}
