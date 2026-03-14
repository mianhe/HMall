package com.hmall.activity.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.activity.domain.ActivityRepository;
import com.hmall.activity.domain.ActivityStats;
import com.hmall.activity.domain.BusinessActivity;
import com.hmall.activity.domain.DailyActivityStats;
import com.hmall.activity.domain.OrderFact;
import com.hmall.activity.domain.OrderFactDailyStats;
import com.hmall.activity.domain.OrderFactProjection;
import com.hmall.activity.domain.OrderFactRepository;
import com.hmall.activity.domain.OrderFactStats;
import com.hmall.activity.domain.OrderItemFact;
import com.hmall.activity.domain.ProductRanking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ActivityApplicationService.class);

    private final ActivityRepository repository;
    private final OrderFactRepository orderFactRepository;
    private final OrderFactProjection orderFactProjection;
    private final ObjectMapper objectMapper;

    public ActivityApplicationService(ActivityRepository repository,
                                      OrderFactRepository orderFactRepository,
                                      OrderFactProjection orderFactProjection,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.orderFactRepository = orderFactRepository;
        this.orderFactProjection = orderFactProjection;
        this.objectMapper = objectMapper;
    }

    public List<BusinessActivity> listByOrderId(Long orderId, int limit) {
        return repository.findByOrderId(orderId, limit);
    }

    public List<BusinessActivity> listByUserId(Long userId, int limit) {
        return repository.findByUserId(userId, limit);
    }

    public List<BusinessActivity> listByCorrelationKey(String keyName, Long keyValue, int limit) {
        return repository.findByCorrelationKey(keyName, keyValue, limit);
    }

    public List<BusinessActivity> listRecent(int limit) {
        return repository.findRecent(limit);
    }

    public ActivityStats getStats(LocalDate fromDate, LocalDate toDate) {
        ZoneId zone = ZoneId.systemDefault();
        Instant from = fromDate.atStartOfDay(zone).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(zone).toInstant();

        Map<String, Long> counts = repository.countByEventTypeInRange(from, to);

        long ps = counts.getOrDefault("PaymentCompleted", 0L);
        long pf = counts.getOrDefault("PaymentFailed", 0L);
        long pe = counts.getOrDefault("PaymentExpired", 0L);

        long paymentTotalCents = sumAmountCentsFromPayloads(
            repository.findPayloadsByEventTypeInRange("PaymentCompleted", from, to));

        long distinctBuyers = repository.countDistinctBuyers(from, to);

        return new ActivityStats(
            counts.getOrDefault("OrderCreated", 0L).intValue(),
            counts.getOrDefault("OrderCancelled", 0L).intValue(),
            counts.getOrDefault("OrderCompleted", 0L).intValue(),
            (int) ps, (int) pf, (int) pe,
            paymentTotalCents,
            counts.getOrDefault("StockReserved", 0L).intValue(),
            counts.getOrDefault("StockReleased", 0L).intValue(),
            counts.getOrDefault("FulfillmentOrderCreated", 0L).intValue(),
            counts.getOrDefault("FulfillmentOrderAllocated", 0L).intValue(),
            counts.getOrDefault("FulfillmentShipped", 0L).intValue(),
            counts.getOrDefault("FulfillmentDelivered", 0L).intValue(),
            distinctBuyers
        );
    }

    public List<DailyActivityStats> getDailyStats(LocalDate fromDate, LocalDate toDate) {
        ZoneId zone = ZoneId.systemDefault();
        Instant from = fromDate.atStartOfDay(zone).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(zone).toInstant();

        Map<LocalDate, Map<String, Long>> dailyCounts = repository.countByEventTypeGroupByDay(from, to);
        Map<LocalDate, List<String>> dailyPayments = repository.findPaymentPayloadsByDay(from, to);
        Map<LocalDate, Long> dailyBuyers = repository.countDistinctBuyersByDay(from, to);

        List<DailyActivityStats> results = new ArrayList<>();
        for (LocalDate day = fromDate; !day.isAfter(toDate); day = day.plusDays(1)) {
            Map<String, Long> counts = dailyCounts.getOrDefault(day, Map.of());
            List<String> payloads = dailyPayments.getOrDefault(day, List.of());
            long paymentTotalCents = sumAmountCentsFromPayloads(payloads);

            long ps = counts.getOrDefault("PaymentCompleted", 0L);
            long pf = counts.getOrDefault("PaymentFailed", 0L);
            long pe = counts.getOrDefault("PaymentExpired", 0L);

            results.add(new DailyActivityStats(day, new ActivityStats(
                counts.getOrDefault("OrderCreated", 0L).intValue(),
                counts.getOrDefault("OrderCancelled", 0L).intValue(),
                counts.getOrDefault("OrderCompleted", 0L).intValue(),
                (int) ps, (int) pf, (int) pe,
                paymentTotalCents,
                counts.getOrDefault("StockReserved", 0L).intValue(),
                counts.getOrDefault("StockReleased", 0L).intValue(),
                counts.getOrDefault("FulfillmentOrderCreated", 0L).intValue(),
                counts.getOrDefault("FulfillmentOrderAllocated", 0L).intValue(),
                counts.getOrDefault("FulfillmentShipped", 0L).intValue(),
                counts.getOrDefault("FulfillmentDelivered", 0L).intValue(),
                dailyBuyers.getOrDefault(day, 0L)
            )));
        }
        return results;
    }

    private long sumAmountCentsFromPayloads(List<String> payloads) {
        long total = 0;
        for (String payload : payloads) {
            try {
                JsonNode node = objectMapper.readTree(payload);
                if (node.has("amountCents")) {
                    total += node.get("amountCents").asLong();
                }
            } catch (Exception e) {
                log.warn("解析 PaymentCompleted payload 失败", e);
            }
        }
        return total;
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    @Transactional
    public void deleteSeedByBatch(String batchTag) {
        repository.deleteBySeedBatch(batchTag);
    }

    @Transactional
    public void deleteAllSeedData() {
        repository.deleteAllSeedData();
    }

    @Transactional
    public void deleteSeedDataInRange(Instant from, Instant to) {
        repository.deleteSeedDataInRange(from, to);
    }

    public List<Object[]> getSeedBatchSummaries() {
        return repository.findSeedBatchSummaries();
    }

    @Transactional
    public void record(RecordActivityCommand command) {
        if (repository.existsByEventId(command.eventId())) {
            return;
        }
        BusinessActivity activity = BusinessActivity.of(
            command.eventId(),
            command.eventType(),
            command.topic(),
            command.orderId(),
            command.userId(),
            command.correlationKeys(),
            command.payload(),
            command.occurredAt(),
            Instant.now()
        );
        repository.save(activity);

        if (command.orderId() != null) {
            orderFactProjection.projectOrder(command.orderId());
        }
    }

    public java.util.Optional<OrderFact> getOrderFact(Long orderId) {
        return orderFactRepository.findByOrderId(orderId);
    }

    public List<OrderItemFact> getOrderItemFacts(Long orderId) {
        return orderFactRepository.findItemsByOrderId(orderId);
    }

    public int rebuildOrderFacts() {
        return orderFactProjection.rebuildAll();
    }

    public OrderFactStats getOrderFactStats(LocalDate from, LocalDate to) {
        List<OrderFact> facts = orderFactRepository.findByDateRange(from, to);
        return OrderFactStats.compute(facts, from, to);
    }

    public List<OrderFactDailyStats> getOrderFactDailyStats(LocalDate from, LocalDate to) {
        List<OrderFact> facts = orderFactRepository.findByDateRange(from, to);
        Map<LocalDate, List<OrderFact>> grouped = facts.stream()
            .collect(Collectors.groupingBy(OrderFact::createdDate, LinkedHashMap::new, Collectors.toList()));

        List<OrderFactDailyStats> result = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<OrderFact> dayFacts = grouped.getOrDefault(day, List.of());
            int total = dayFacts.size();
            int completed = (int) dayFacts.stream().filter(f -> "COMPLETED".equals(f.currentStage())).count();
            int cancelled = (int) dayFacts.stream().filter(f -> "CANCELLED".equals(f.currentStage())).count();
            int vas = (int) dayFacts.stream().filter(f -> f.hasEngraving() || f.hasWarranty()).count();
            int engraving = (int) dayFacts.stream().filter(OrderFact::hasEngraving).count();
            int warranty = (int) dayFacts.stream().filter(OrderFact::hasWarranty).count();
            long revenue = dayFacts.stream().mapToLong(OrderFact::totalAmountCents).sum();
            long avg = total > 0 ? revenue / total : 0;
            result.add(new OrderFactDailyStats(day, total, completed, cancelled, vas, engraving, warranty, revenue, avg));
        }
        return result;
    }

    public ProductRanking getProductRanking(LocalDate from, LocalDate to, String rankBy,
                                            Boolean hasEngraving, Boolean hasWarranty,
                                            String groupBy, int limit) {
        List<OrderItemFact> items = orderFactRepository.findItemsByDateRange(from, to);

        if (hasEngraving != null && hasEngraving) {
            items = items.stream().filter(OrderItemFact::orderHasEngraving).toList();
        }
        if (hasWarranty != null && hasWarranty) {
            items = items.stream().filter(OrderItemFact::orderHasWarranty).toList();
        }

        boolean bySku = "sku".equals(groupBy);
        Map<Long, ProductRanking.ProductRankingItem> grouped = new HashMap<>();

        for (OrderItemFact item : items) {
            Long key = bySku ? item.skuId() : item.spuId();
            if (key == null) continue;
            Long finalKey = key;
            grouped.merge(key,
                new ProductRanking.ProductRankingItem(
                    bySku ? null : finalKey, bySku ? finalKey : null,
                    item.quantity(), item.lineTotalCents(), 1,
                    "CANCELLED".equals(item.orderCurrentStage()) ? 1 : 0),
                (a, b) -> new ProductRanking.ProductRankingItem(
                    a.spuId(), a.skuId(),
                    a.totalQuantity() + b.totalQuantity(),
                    a.totalRevenueCents() + b.totalRevenueCents(),
                    a.orderCount() + b.orderCount(),
                    a.cancelledOrderCount() + b.cancelledOrderCount()));
        }

        Comparator<ProductRanking.ProductRankingItem> cmp = switch (rankBy != null ? rankBy : "revenue") {
            case "quantity" -> Comparator.comparingInt(ProductRanking.ProductRankingItem::totalQuantity).reversed();
            case "orderCount" -> Comparator.comparingInt(ProductRanking.ProductRankingItem::orderCount).reversed();
            default -> Comparator.comparingLong(ProductRanking.ProductRankingItem::totalRevenueCents).reversed();
        };

        List<ProductRanking.ProductRankingItem> ranked = grouped.values().stream()
            .sorted(cmp)
            .limit(limit)
            .toList();

        return new ProductRanking(rankBy != null ? rankBy : "revenue", ranked, from, to);
    }

    public List<OrderFact> listOrderFacts(LocalDate from, LocalDate to,
                                          Boolean hasEngraving, Boolean hasWarranty,
                                          String currentStage, Long userId, int limit) {
        return orderFactRepository.findByFilters(from, to, hasEngraving, hasWarranty, currentStage, userId, limit);
    }

    @Transactional
    public void deleteOrderFacts() {
        orderFactRepository.deleteAll();
    }
}
