package com.hmall.activity.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderFactProjection {

    private static final Logger log = LoggerFactory.getLogger(OrderFactProjection.class);

    private final ActivityRepository activityRepository;
    private final OrderFactRepository orderFactRepository;
    private final ObjectMapper objectMapper;

    public OrderFactProjection(ActivityRepository activityRepository,
                               OrderFactRepository orderFactRepository,
                               ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.orderFactRepository = orderFactRepository;
        this.objectMapper = objectMapper;
    }

    public void projectOrder(Long orderId) {
        List<BusinessActivity> events = activityRepository.findByOrderId(orderId, 200);
        if (events.isEmpty()) return;

        BusinessActivity orderCreated = events.stream()
            .filter(e -> "OrderCreated".equals(e.eventType()))
            .findFirst()
            .orElse(null);

        if (orderCreated == null) return;

        Set<String> eventTypes = events.stream()
            .map(BusinessActivity::eventType)
            .collect(Collectors.toSet());

        try {
            JsonNode payload = objectMapper.readTree(orderCreated.payload());
            projectFromEvents(orderId, orderCreated, payload, events, eventTypes);
        } catch (Exception e) {
            log.warn("投影订单 {} 失败: {}", orderId, e.getMessage());
        }
    }

    private void projectFromEvents(Long orderId, BusinessActivity orderCreated,
                                   JsonNode payload, List<BusinessActivity> events,
                                   Set<String> eventTypes) {
        Long userId = orderCreated.userId();
        long totalAmountCents = payload.path("totalAmountCents").asLong();
        JsonNode itemsNode = payload.path("items");

        int itemCount = itemsNode.size();
        int totalQuantity = 0;
        for (JsonNode item : itemsNode) {
            totalQuantity += item.path("quantity").asInt();
        }

        String currentStage = deriveStage(eventTypes);
        boolean hasEngraving = eventTypes.contains("EngravingCompleted");
        boolean hasWarranty = eventTypes.contains("ServiceActivated");

        String cancelReason = null;
        if ("CANCELLED".equals(currentStage)) {
            cancelReason = eventTypes.contains("PaymentExpired") ? "TIMEOUT" : "MANUAL";
        }

        boolean isAbnormal = eventTypes.contains("PaymentFailed")
            || eventTypes.contains("PaymentExpired");

        Instant createdAt = findOccurredAt(events, "OrderCreated");
        Instant paidAt = findOccurredAt(events, "PaymentCompleted");
        Instant shippedAt = findOccurredAt(events, "FulfillmentShipped");
        Instant deliveredAt = findOccurredAt(events, "FulfillmentDelivered");
        Instant completedAt = findOccurredAt(events, "OrderCompleted");
        Instant cancelledAt = findOccurredAt(events, "OrderCancelled");

        Long paymentDurationSec = (paidAt != null && createdAt != null)
            ? Duration.between(createdAt, paidAt).getSeconds() : null;
        Long fulfillmentDurationSec = (deliveredAt != null && paidAt != null)
            ? Duration.between(paidAt, deliveredAt).getSeconds() : null;

        LocalDate createdDate = createdAt.atZone(ZoneId.systemDefault()).toLocalDate();
        int createdHour = createdAt.atZone(ZoneId.systemDefault()).getHour();

        String seedBatch = events.stream()
            .map(BusinessActivity::seedBatch)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        OrderFact fact = new OrderFact(orderId, userId, totalAmountCents, itemCount, totalQuantity,
            hasEngraving, hasWarranty, currentStage, cancelReason, isAbnormal,
            createdAt, paidAt, shippedAt, deliveredAt, completedAt, cancelledAt,
            paymentDurationSec, fulfillmentDurationSec, createdDate, createdHour, seedBatch);

        orderFactRepository.save(fact);

        List<OrderItemFact> items = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            long skuId = itemNode.path("skuId").asLong();
            Long spuId = itemNode.has("spuId") && !itemNode.path("spuId").isNull()
                ? itemNode.path("spuId").asLong() : null;
            int qty = itemNode.path("quantity").asInt();
            long unitPrice = itemNode.path("unitPriceCents").asLong();
            long lineTotal = (long) qty * unitPrice;

            items.add(new OrderItemFact(null, orderId, userId, skuId, spuId, qty, unitPrice,
                lineTotal, totalAmountCents, currentStage, hasEngraving, hasWarranty,
                createdDate, seedBatch));
        }

        orderFactRepository.deleteItemsByOrderId(orderId);
        if (!items.isEmpty()) {
            orderFactRepository.saveItems(items);
        }
    }

    public int rebuildAll() {
        List<Long> orderIds = activityRepository.findDistinctOrderIds();
        for (Long orderId : orderIds) {
            projectOrder(orderId);
        }
        return orderIds.size();
    }

    private String deriveStage(Set<String> eventTypes) {
        if (eventTypes.contains("OrderCancelled")) return "CANCELLED";
        if (eventTypes.contains("OrderCompleted")) return "COMPLETED";
        if (eventTypes.contains("FulfillmentDelivered")) return "DELIVERED";
        if (eventTypes.contains("FulfillmentShipped")) return "SHIPPED";
        if (eventTypes.contains("FulfillmentOrderCreated")
            || eventTypes.contains("FulfillmentOrderAllocated")) return "FULFILLING";
        if (eventTypes.contains("PaymentCompleted")) return "PAID";
        return "CREATED";
    }

    private Instant findOccurredAt(List<BusinessActivity> events, String eventType) {
        return events.stream()
            .filter(e -> eventType.equals(e.eventType()))
            .map(BusinessActivity::occurredAt)
            .findFirst()
            .orElse(null);
    }
}
