package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.FulfillmentItem;
import com.hmall.fulfillment.domain.FulfillmentItemType;
import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderAllocated;
import com.hmall.fulfillment.domain.FulfillmentOrderCreated;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.FulfillmentType;
import com.hmall.fulfillment.domain.ServiceActivated;
import com.hmall.fulfillment.domain.ShippingAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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

        List<FulfillmentItem> physicalItems = new ArrayList<>();
        List<FulfillmentItem> serviceItems = new ArrayList<>();
        for (CreateFulfillmentItem item : items) {
            FulfillmentItemType type = item.itemType() == null
                ? FulfillmentItemType.PHYSICAL
                : FulfillmentItemType.valueOf(item.itemType());
            FulfillmentItem domainItem = new FulfillmentItem(item.skuId(), item.quantity(), type);
            if (type == FulfillmentItemType.SERVICE) {
                serviceItems.add(domainItem);
            } else {
                physicalItems.add(domainItem);
            }
        }

        if (physicalItems.isEmpty() && serviceItems.isEmpty()) {
            throw new FulfillmentBadRequestException("items 不能为空");
        }

        ShippingAddress shippingAddress = new ShippingAddress(
            address.recipientName(), address.phone(),
            address.province(), address.city(), address.district(), address.detail()
        );

        List<Long> ids = new ArrayList<>();
        if (!physicalItems.isEmpty()) {
            FulfillmentOrder physicalOrder = new FulfillmentOrder(
                orderId, FulfillmentType.PHYSICAL, physicalItems, shippingAddress
            );
            FulfillmentOrder saved = repository.save(physicalOrder);
            saved.allocate();
            saved = repository.save(saved);
            ids.add(saved.getFulfillmentOrderId());
            eventPublisher.publish(new FulfillmentOrderAllocated(
                orderId, saved.getFulfillmentOrderId(), Instant.now()));
        }

        if (!serviceItems.isEmpty()) {
            FulfillmentOrder virtualOrder = new FulfillmentOrder(
                orderId, FulfillmentType.VIRTUAL, serviceItems, shippingAddress
            );
            FulfillmentOrder savedVirtual = repository.save(virtualOrder);
            savedVirtual.activate();
            savedVirtual = repository.save(savedVirtual);
            ids.add(savedVirtual.getFulfillmentOrderId());
            long serviceSkuId = serviceItems.get(0).getSkuId();
            Instant now = Instant.now();
            eventPublisher.publish(new ServiceActivated(
                orderId,
                savedVirtual.getFulfillmentOrderId(),
                serviceSkuId,
                now,
                null,
                now
            ));
        }

        eventPublisher.publish(new FulfillmentOrderCreated(orderId, ids, Instant.now()));

        return new CreateFulfillmentResult(orderId, ids);
    }

    public record CreateFulfillmentItem(Long skuId, int quantity, String itemType) {}

    public record CreateShippingAddress(String recipientName, String phone,
                                        String province, String city, String district, String detail) {}

    public record CreateFulfillmentResult(Long orderId, List<Long> fulfillmentOrderIds) {}
}
