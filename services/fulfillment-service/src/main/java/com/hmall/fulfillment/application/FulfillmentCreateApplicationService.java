package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.FulfillmentItem;
import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderCreated;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.ShippingAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class FulfillmentCreateApplicationService {

    private final FulfillmentOrderRepository repository;
    private final DomainEventPublisher eventPublisher;

    public FulfillmentCreateApplicationService(FulfillmentOrderRepository repository,
                                               DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateFulfillmentResult create(Long orderId, List<CreateFulfillmentItem> items,
                                          CreateShippingAddress address) {
        if (orderId == null) {
            throw new FulfillmentBadRequestException("orderId 不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new FulfillmentBadRequestException("items 不能为空");
        }
        if (address == null) {
            throw new FulfillmentBadRequestException("shippingAddress 不能为空");
        }

        List<FulfillmentOrder> existing = repository.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            List<Long> existingIds = existing.stream()
                .map(FulfillmentOrder::getFulfillmentOrderId)
                .toList();
            return new CreateFulfillmentResult(orderId, existingIds);
        }

        List<FulfillmentItem> domainItems = items.stream()
            .map(i -> new FulfillmentItem(i.skuId(), i.quantity()))
            .toList();
        ShippingAddress shippingAddress = new ShippingAddress(
            address.recipientName(), address.phone(),
            address.province(), address.city(), address.district(), address.detail()
        );

        FulfillmentOrder order = new FulfillmentOrder(orderId, domainItems, shippingAddress);
        FulfillmentOrder saved = repository.save(order);

        List<Long> ids = List.of(saved.getFulfillmentOrderId());
        eventPublisher.publish(new FulfillmentOrderCreated(orderId, ids, Instant.now()));

        return new CreateFulfillmentResult(orderId, ids);
    }

    public record CreateFulfillmentItem(Long skuId, int quantity) {}

    public record CreateShippingAddress(String recipientName, String phone,
                                        String province, String city, String district, String detail) {}

    public record CreateFulfillmentResult(Long orderId, List<Long> fulfillmentOrderIds) {}
}
