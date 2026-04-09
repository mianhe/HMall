package com.hmall.activity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.activity.domain.ActivityRepository;
import com.hmall.activity.domain.BusinessActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成演示用种子数据。为智能运营仪表盘提供可视化的历史事件序列。
 * 仅用于开发/演示环境，不影响真实业务数据。
 *
 * Payload 结构严格对齐 docs/ontology/events-and-processes.md 声明。
 * userId 覆盖范围与真实 Kafka 管道一致：仅 Order BC 事件携带 userId。
 */
@Service
public class SeedDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(SeedDataGenerator.class);

    private final ActivityRepository repository;
    private final ObjectMapper objectMapper;

    private static final long[] SAMPLE_USER_IDS = {1, 2, 3, 5, 8, 13, 21, 42, 100, 200};
    private static final long[] SAMPLE_SPU_IDS = {1, 2, 3, 5, 8};
    private static final long[] SAMPLE_SKU_IDS = {1, 2, 3, 4, 5, 6, 7, 8, 10, 12};
    private static final int[] SAMPLE_PRICES_CENTS = {9900, 19900, 29900, 49900, 59900, 99900, 199900, 299900};

    private static final Object[][] SAMPLE_ENGRAVING = {
        {1L, "山峦", "刻字"},
        {2L, "莲花", "赠"},
        {3L, "星座", "姓名"},
        {4L, "浮世绘·浪", "定制"},
        {5L, "墨竹", "祝福"},
        {6L, "木竹2", "纪念"},
    };

    private static final String[][] SAMPLE_WARRANTIES = {
        {"华为 Care+ 一年期", "1年"},
        {"华为 Care+ 两年期", "2年"},
        {"碎屏险 一年期", "1年"},
        {"碎屏险 两年期", "2年"},
    };

    public SeedDataGenerator(ActivityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public record SeedResult(int ordersGenerated, int eventsGenerated, String timeRange, String batchTag) {}

    /** 向后兼容的旧入口：从今天往前推 days 天 */
    @Transactional
    public SeedResult generate(int days, int ordersPerDay, int maxOrders) {
        LocalDate today = LocalDate.now();
        SeedRequest request = new SeedRequest(
            today.minusDays(days - 1), today,
            ordersPerDay, maxOrders, null,
            null, null, null, null, null, null
        );
        return generate(request);
    }

    @Transactional
    public SeedResult generate(SeedRequest request) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        ZoneId zone = ZoneId.systemDefault();

        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now().minusDays(29);
        LocalDate endDate = request.endDate() != null ? request.endDate() : LocalDate.now();
        int ordersPerDay = request.ordersPerDay() > 0 ? request.ordersPerDay() : 5;
        int maxOrders = request.maxOrders();
        String batchTag = request.batchTag() != null && !request.batchTag().isBlank()
            ? request.batchTag()
            : "seed-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 6);

        double cancelThreshold = request.cancelRatioOrDefault();
        double paymentExpiredThreshold = cancelThreshold + request.paymentExpiredRatioOrDefault();
        double paymentFailedThreshold = paymentExpiredThreshold + request.paymentFailedRatioOrDefault();
        double engravingRatio = request.engravingRatioOrDefault();
        double warrantyRatio = request.warrantyRatioOrDefault();
        double multiItemRatio = request.multiItemRatioOrDefault();

        List<BusinessActivity> batch = new ArrayList<>();
        long orderIdBase = System.currentTimeMillis() % 100000 + 10000;
        int totalOrders = 0;
        boolean capped = maxOrders > 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (capped && totalOrders >= maxOrders) break;
            Instant dayStart = date.atStartOfDay(zone).toInstant();
            int count = ordersPerDay + rng.nextInt(-ordersPerDay / 3, ordersPerDay / 3 + 1);
            if (count < 1) count = 1;

            long daysUntilEnd = endDate.toEpochDay() - date.toEpochDay();

            for (int o = 0; o < count; o++) {
                if (capped && totalOrders >= maxOrders) break;
                long orderId = orderIdBase++;
                long userId = SAMPLE_USER_IDS[rng.nextInt(SAMPLE_USER_IDS.length)];
                int itemCount = rng.nextDouble() < multiItemRatio ? rng.nextInt(2, 4) : 1;

                boolean hasEngraving = rng.nextDouble() < engravingRatio;
                boolean hasWarranty = rng.nextDouble() < warrantyRatio;

                List<Map<String, Object>> itemsList = new ArrayList<>();
                long totalCents = 0;
                List<Long> spuIds = new ArrayList<>();
                List<Long> skuIds = new ArrayList<>();
                List<Map<String, Object>> stockItems = new ArrayList<>();

                for (int i = 0; i < itemCount; i++) {
                    long spuId = SAMPLE_SPU_IDS[rng.nextInt(SAMPLE_SPU_IDS.length)];
                    long skuId = SAMPLE_SKU_IDS[rng.nextInt(SAMPLE_SKU_IDS.length)];
                    int priceCents = SAMPLE_PRICES_CENTS[rng.nextInt(SAMPLE_PRICES_CENTS.length)];
                    int qty = rng.nextInt(1, 4);
                    totalCents += (long) priceCents * qty;
                    spuIds.add(spuId);
                    skuIds.add(skuId);
                    stockItems.add(Map.of("skuId", skuId, "quantity", qty));

                    Map<String, Object> item = new HashMap<>();
                    item.put("skuId", skuId);
                    item.put("spuId", spuId);
                    item.put("quantity", qty);
                    item.put("unitPriceCents", priceCents);
                    if (i == 0 && hasEngraving) {
                        item.put("serviceAttributes", buildEngravingAttributes(rng));
                    }
                    if (i == 0 && hasWarranty) {
                        item.put("warrantyAttributes", buildWarrantyAttributes(rng));
                    }
                    itemsList.add(item);
                }

                long paymentId = orderId * 10 + 1;
                Instant orderTime = dayStart.plusSeconds(rng.nextLong(0, 86400));
                String corrKeys = toJson(Map.of("spuIds", spuIds, "skuIds", skuIds));

                double fate = rng.nextDouble();

                batch.add(seedOrderEvent("OrderCreated", "order.created", orderId, userId, corrKeys, orderTime, batchTag,
                    Map.of("eventType", "OrderCreated", "orderId", orderId, "userId", userId,
                           "totalAmountCents", totalCents, "items", itemsList)));

                Instant t = orderTime.plusSeconds(rng.nextLong(1, 5));
                batch.add(seedNonOrderEvent("StockReserved", "inventory.stock.reserved", orderId, corrKeys, t, batchTag,
                    Map.of("eventType", "StockReserved", "orderId", orderId, "items", stockItems)));

                if (fate < cancelThreshold) {
                    t = t.plusSeconds(rng.nextLong(60, 600));
                    batch.add(seedOrderEvent("OrderCancelled", "order.cancelled", orderId, userId, corrKeys, t, batchTag,
                        Map.of("eventType", "OrderCancelled", "orderId", orderId, "userId", userId,
                               "totalAmountCents", totalCents, "items", itemsList)));
                    batch.add(seedNonOrderEvent("StockReleased", "inventory.stock.released", orderId, corrKeys, t.plusSeconds(1), batchTag,
                        Map.of("eventType", "StockReleased", "orderId", orderId)));
                    totalOrders++;
                    continue;
                }

                if (fate < paymentExpiredThreshold) {
                    t = t.plus(Duration.ofMinutes(rng.nextLong(15, 30)));
                    batch.add(seedNonOrderEvent("PaymentExpired", "payment.expired", orderId, corrKeys, t, batchTag,
                        Map.of("eventType", "PaymentExpired", "orderId", orderId)));
                    batch.add(seedOrderEvent("OrderCancelled", "order.cancelled", orderId, userId, corrKeys, t.plusSeconds(1), batchTag,
                        Map.of("eventType", "OrderCancelled", "orderId", orderId, "userId", userId,
                               "totalAmountCents", totalCents, "items", itemsList)));
                    batch.add(seedNonOrderEvent("StockReleased", "inventory.stock.released", orderId, corrKeys, t.plusSeconds(2), batchTag,
                        Map.of("eventType", "StockReleased", "orderId", orderId)));
                    totalOrders++;
                    continue;
                }

                if (fate < paymentFailedThreshold) {
                    t = t.plusSeconds(rng.nextLong(10, 300));
                    batch.add(seedNonOrderEvent("PaymentFailed", "payment.failed", orderId, corrKeys, t, batchTag,
                        Map.of("eventType", "PaymentFailed", "orderId", orderId)));
                    t = t.plus(Duration.ofMinutes(rng.nextLong(15, 30)));
                    batch.add(seedNonOrderEvent("PaymentExpired", "payment.expired", orderId, corrKeys, t, batchTag,
                        Map.of("eventType", "PaymentExpired", "orderId", orderId)));
                    batch.add(seedOrderEvent("OrderCancelled", "order.cancelled", orderId, userId, corrKeys, t.plusSeconds(1), batchTag,
                        Map.of("eventType", "OrderCancelled", "orderId", orderId, "userId", userId,
                               "totalAmountCents", totalCents, "items", itemsList)));
                    batch.add(seedNonOrderEvent("StockReleased", "inventory.stock.released", orderId, corrKeys, t.plusSeconds(2), batchTag,
                        Map.of("eventType", "StockReleased", "orderId", orderId)));
                    totalOrders++;
                    continue;
                }

                // === Happy Path: PaymentCompleted ===
                t = t.plusSeconds(rng.nextLong(3, 30));
                batch.add(seedNonOrderEvent("PaymentCompleted", "payment.completed", orderId, corrKeys, t, batchTag,
                    Map.of("eventType", "PaymentCompleted", "orderId", orderId,
                           "paymentId", paymentId, "amountCents", totalCents)));

                t = t.plusSeconds(rng.nextLong(1, 10));
                long fulfillmentOrderId = orderId * 10 + 2;
                batch.add(seedNonOrderEvent("FulfillmentOrderCreated", "fulfillment.order.created", orderId, corrKeys, t, batchTag,
                    Map.of("eventType", "FulfillmentOrderCreated", "orderId", orderId,
                           "fulfillmentOrderIds", List.of(fulfillmentOrderId))));

                t = t.plus(Duration.ofMinutes(rng.nextLong(30, 360)));
                batch.add(seedNonOrderEvent("FulfillmentOrderAllocated", "fulfillment.order.allocated", orderId, corrKeys, t, batchTag,
                    Map.of("eventType", "FulfillmentOrderAllocated", "orderId", orderId,
                           "fulfillmentOrderId", fulfillmentOrderId)));

                if (hasEngraving) {
                    t = t.plus(Duration.ofMinutes(rng.nextLong(10, 120)));
                    batch.add(seedNonOrderEvent("EngravingCompleted", "fulfillment.engraving.completed", orderId, corrKeys, t, batchTag,
                        Map.of("eventType", "EngravingCompleted", "orderId", orderId,
                               "fulfillmentOrderId", fulfillmentOrderId, "completedAt", t.toString())));
                }

                if (hasWarranty) {
                    t = t.plusSeconds(rng.nextLong(1, 60));
                    long serviceSkuId = 300 + rng.nextLong(1, 5);
                    String actIso = t.toString();
                    String expIso = t.plus(Duration.ofDays(365)).toString();
                    batch.add(seedNonOrderEvent("ServiceActivated", "fulfillment.service.activated", orderId, corrKeys, t, batchTag,
                        Map.of("eventType", "ServiceActivated", "orderId", orderId,
                               "fulfillmentOrderId", fulfillmentOrderId, "serviceSkuId", serviceSkuId,
                               "activatedAt", actIso, "expiresAt", expIso)));
                }

                // === FulfillmentShipped ===
                t = t.plus(Duration.ofHours(rng.nextLong(2, 24)));
                if (daysUntilEnd < 2 && rng.nextDouble() < 0.4) {
                    totalOrders++;
                    continue;
                }
                batch.add(seedNonOrderEvent("FulfillmentShipped", "fulfillment.shipped", orderId, corrKeys, t, batchTag,
                    Map.of("eventType", "FulfillmentShipped", "orderId", orderId,
                           "fulfillmentOrderId", fulfillmentOrderId)));

                // === FulfillmentDelivered ===
                t = t.plus(Duration.ofDays(rng.nextLong(1, 5)));
                Instant endBoundary = endDate.plusDays(1).atStartOfDay(zone).toInstant();
                if (t.isAfter(endBoundary)) {
                    totalOrders++;
                    continue;
                }
                batch.add(seedNonOrderEvent("FulfillmentDelivered", "fulfillment.delivered", orderId, corrKeys, t, batchTag,
                    Map.of("eventType", "FulfillmentDelivered", "orderId", orderId,
                           "fulfillmentOrderId", fulfillmentOrderId)));

                // === OrderCompleted (Order BC → userId ✅) ===
                t = t.plusSeconds(rng.nextLong(1, 60));
                batch.add(seedOrderEvent("OrderCompleted", "order.completed", orderId, userId, corrKeys, t, batchTag,
                    Map.of("eventType", "OrderCompleted", "orderId", orderId, "userId", userId,
                           "totalAmountCents", totalCents, "items", itemsList)));

                totalOrders++;
            }
        }

        for (BusinessActivity a : batch) {
            if (!repository.existsByEventId(a.eventId())) {
                repository.save(a);
            }
        }

        String range = startDate + " ~ " + endDate;
        log.info("种子数据生成完成：{} 订单，{} 事件，范围 {}，批次 {}", totalOrders, batch.size(), range, batchTag);
        return new SeedResult(totalOrders, batch.size(), range, batchTag);
    }

    private BusinessActivity seedOrderEvent(String eventType, String topic, long orderId, long userId,
                                             String corrKeys, Instant occurredAt, String batchTag,
                                             Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        return BusinessActivity.seed(eventId, eventType, topic, orderId, userId, corrKeys,
            toJson(payload), occurredAt, occurredAt.plusSeconds(1), batchTag);
    }

    private BusinessActivity seedNonOrderEvent(String eventType, String topic, long orderId,
                                                String corrKeys, Instant occurredAt, String batchTag,
                                                Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        return BusinessActivity.seed(eventId, eventType, topic, orderId, null, corrKeys,
            toJson(payload), occurredAt, occurredAt.plusSeconds(1), batchTag);
    }

    private Map<String, Object> buildEngravingAttributes(ThreadLocalRandom rng) {
        Object[] sample = SAMPLE_ENGRAVING[rng.nextInt(SAMPLE_ENGRAVING.length)];
        return Map.of(
            "engravingPatternId", sample[0],
            "engravingPatternName", sample[1],
            "engravingText", sample[2]);
    }

    private Map<String, Object> buildWarrantyAttributes(ThreadLocalRandom rng) {
        String[] sample = SAMPLE_WARRANTIES[rng.nextInt(SAMPLE_WARRANTIES.length)];
        return Map.of(
            "warrantyName", sample[0],
            "warrantyDuration", sample[1]);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
