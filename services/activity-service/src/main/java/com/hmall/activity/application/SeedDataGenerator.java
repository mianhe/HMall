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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成演示用种子数据。为智能运营仪表盘提供可视化的历史事件序列。
 * 仅用于开发/演示环境，不影响真实业务数据。
 *
 * Payload 结构严格对齐 docs/intelligent-ops-ontology.md 声明。
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
    /** 镭雕图案样例（与图案库一致），用于种子数据 items.serviceAttributes */
    private static final Object[][] SAMPLE_ENGRAVING = {
        new Object[]{1L, "山峦", "刻字"},
        new Object[]{2L, "莲花", "赠"},
        new Object[]{3L, "星座", "姓名"},
        new Object[]{4L, "浮世绘·浪", "定制"},
        new Object[]{5L, "墨竹", "祝福"},
        new Object[]{6L, "木竹2", "纪念"},
    };

    public SeedDataGenerator(ActivityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public record SeedResult(int ordersGenerated, int eventsGenerated, String timeRange) {}

    /**
     * @param maxOrders 最大订单数，≤0 表示不限制（按 days * ordersPerDay 自然生成）
     */
    @Transactional
    public SeedResult generate(int days, int ordersPerDay, int maxOrders) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        List<BusinessActivity> batch = new ArrayList<>();
        long orderIdBase = System.currentTimeMillis() % 100000 + 10000;
        int totalOrders = 0;
        boolean capped = maxOrders > 0;

        for (int d = days - 1; d >= 0; d--) {
            if (capped && totalOrders >= maxOrders) break;
            LocalDate date = today.minusDays(d);
            Instant dayStart = date.atStartOfDay(zone).toInstant();
            int count = ordersPerDay + rng.nextInt(-ordersPerDay / 3, ordersPerDay / 3 + 1);
            if (count < 1) count = 1;

            for (int o = 0; o < count; o++) {
                if (capped && totalOrders >= maxOrders) break;
                long orderId = orderIdBase++;
                long userId = SAMPLE_USER_IDS[rng.nextInt(SAMPLE_USER_IDS.length)];
                long spuId = SAMPLE_SPU_IDS[rng.nextInt(SAMPLE_SPU_IDS.length)];
                long skuId = SAMPLE_SKU_IDS[rng.nextInt(SAMPLE_SKU_IDS.length)];
                int priceCents = SAMPLE_PRICES_CENTS[rng.nextInt(SAMPLE_PRICES_CENTS.length)];
                int qty = rng.nextInt(1, 4);
                long totalCents = (long) priceCents * qty;
                long paymentId = orderId * 10 + 1;

                // 约 45% 订单带镭雕/虚拟服务（items 含 serviceAttributes，并产生 EngravingCompleted + ServiceActivated），使约 100 条事件带服务/镭雕
                boolean hasServiceOrEngraving = rng.nextDouble() < 0.45;
                List<Map<String, Object>> itemsList = buildItemsList(skuId, spuId, qty, priceCents, hasServiceOrEngraving, rng);

                Instant orderTime = dayStart.plusSeconds(rng.nextLong(0, 86400));
                String corrKeys = toJson(Map.of("spuIds", List.of(spuId), "skuIds", List.of(skuId)));

                double fate = rng.nextDouble();

                // === OrderCreated (Order BC → userId ✅) ===
                batch.add(orderEvent("OrderCreated", "order.created", orderId, userId, corrKeys, orderTime,
                    Map.of("eventType", "OrderCreated", "orderId", orderId, "userId", userId,
                           "totalAmountCents", totalCents, "items", itemsList)));

                // === StockReserved (Inventory BC → userId null) ===
                Instant t = orderTime.plusSeconds(rng.nextLong(1, 5));
                batch.add(nonOrderEvent("StockReserved", "inventory.stock.reserved", orderId, corrKeys, t,
                    Map.of("eventType", "StockReserved", "orderId", orderId,
                           "items", List.of(Map.of("skuId", skuId, "quantity", qty)))));

                if (fate < 0.08) {
                    // ~8% cancel before payment
                    t = t.plusSeconds(rng.nextLong(60, 600));
                    batch.add(orderEvent("OrderCancelled", "order.cancelled", orderId, userId, corrKeys, t,
                        Map.of("eventType", "OrderCancelled", "orderId", orderId, "userId", userId,
                               "totalAmountCents", totalCents, "items", itemsList)));
                    batch.add(nonOrderEvent("StockReleased", "inventory.stock.released", orderId, corrKeys, t.plusSeconds(1),
                        Map.of("eventType", "StockReleased", "orderId", orderId)));
                    totalOrders++;
                    continue;
                }

                if (fate < 0.12) {
                    // ~4% payment failed
                    t = t.plusSeconds(rng.nextLong(10, 300));
                    batch.add(nonOrderEvent("PaymentFailed", "payment.failed", orderId, corrKeys, t,
                        Map.of("eventType", "PaymentFailed", "orderId", orderId)));
                    // PaymentFailed 不取消订单，用户可重试；但种子数据中模拟用户放弃
                    batch.add(nonOrderEvent("StockReleased", "inventory.stock.released", orderId, corrKeys, t.plusSeconds(1),
                        Map.of("eventType", "StockReleased", "orderId", orderId)));
                    totalOrders++;
                    continue;
                }

                if (fate < 0.15) {
                    // ~3% payment expired → auto cancel
                    t = t.plus(Duration.ofMinutes(rng.nextLong(15, 30)));
                    batch.add(nonOrderEvent("PaymentExpired", "payment.expired", orderId, corrKeys, t,
                        Map.of("eventType", "PaymentExpired", "orderId", orderId)));
                    batch.add(orderEvent("OrderCancelled", "order.cancelled", orderId, userId, corrKeys, t.plusSeconds(1),
                        Map.of("eventType", "OrderCancelled", "orderId", orderId, "userId", userId,
                               "totalAmountCents", totalCents, "items", itemsList)));
                    batch.add(nonOrderEvent("StockReleased", "inventory.stock.released", orderId, corrKeys, t.plusSeconds(2),
                        Map.of("eventType", "StockReleased", "orderId", orderId)));
                    totalOrders++;
                    continue;
                }

                // === PaymentCompleted (Payment BC → userId null) ===
                t = t.plusSeconds(rng.nextLong(3, 30));
                batch.add(nonOrderEvent("PaymentCompleted", "payment.completed", orderId, corrKeys, t,
                    Map.of("eventType", "PaymentCompleted", "orderId", orderId,
                           "paymentId", paymentId, "amountCents", totalCents)));

                // === FulfillmentOrderCreated (Fulfillment BC → userId null) ===
                t = t.plusSeconds(rng.nextLong(1, 10));
                long fulfillmentOrderId = orderId * 10 + 2;
                batch.add(nonOrderEvent("FulfillmentOrderCreated", "fulfillment.order.created", orderId, corrKeys, t,
                    Map.of("eventType", "FulfillmentOrderCreated", "orderId", orderId,
                           "fulfillmentOrderIds", List.of(fulfillmentOrderId))));

                // === FulfillmentOrderAllocated ===
                t = t.plus(Duration.ofMinutes(rng.nextLong(30, 360)));
                batch.add(nonOrderEvent("FulfillmentOrderAllocated", "fulfillment.order.allocated", orderId, corrKeys, t,
                    Map.of("eventType", "FulfillmentOrderAllocated", "orderId", orderId,
                           "fulfillmentOrderId", fulfillmentOrderId)));

                // === 镭雕/虚拟服务：EngravingCompleted + ServiceActivated（仅 hasServiceOrEngraving 订单）===
                if (hasServiceOrEngraving) {
                    t = t.plus(Duration.ofMinutes(rng.nextLong(10, 120)));
                    String iso = t.toString();
                    batch.add(nonOrderEvent("EngravingCompleted", "fulfillment.engraving.completed", orderId, corrKeys, t,
                        Map.of("eventType", "EngravingCompleted", "orderId", orderId,
                               "fulfillmentOrderId", fulfillmentOrderId, "completedAt", iso, "occurredAt", iso)));
                    t = t.plusSeconds(rng.nextLong(1, 30));
                    long serviceSkuId = 300 + rng.nextLong(1, 5);
                    String actIso = t.toString();
                    String expIso = t.plus(Duration.ofDays(365)).toString();
                    batch.add(nonOrderEvent("ServiceActivated", "fulfillment.service.activated", orderId, corrKeys, t,
                        Map.of("eventType", "ServiceActivated", "orderId", orderId,
                               "fulfillmentOrderId", fulfillmentOrderId, "serviceSkuId", serviceSkuId,
                               "activatedAt", actIso, "expiresAt", expIso, "occurredAt", actIso)));
                }

                // === FulfillmentShipped ===
                t = t.plus(Duration.ofHours(rng.nextLong(2, 24)));
                if (d < 1 && rng.nextDouble() < 0.3) {
                    totalOrders++;
                    continue;
                }
                batch.add(nonOrderEvent("FulfillmentShipped", "fulfillment.shipped", orderId, corrKeys, t,
                    Map.of("eventType", "FulfillmentShipped", "orderId", orderId,
                           "fulfillmentOrderId", fulfillmentOrderId)));

                // === FulfillmentDelivered ===
                t = t.plus(Duration.ofDays(rng.nextLong(1, 5)));
                if (t.isAfter(Instant.now())) {
                    totalOrders++;
                    continue;
                }
                batch.add(nonOrderEvent("FulfillmentDelivered", "fulfillment.delivered", orderId, corrKeys, t,
                    Map.of("eventType", "FulfillmentDelivered", "orderId", orderId,
                           "fulfillmentOrderId", fulfillmentOrderId)));

                // === OrderCompleted (Order BC → userId ✅) ===
                t = t.plusSeconds(rng.nextLong(1, 60));
                batch.add(orderEvent("OrderCompleted", "order.completed", orderId, userId, corrKeys, t,
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

        String range = today.minusDays(days - 1) + " ~ " + today;
        log.info("种子数据生成完成：{} 订单，{} 事件，范围 {}", totalOrders, batch.size(), range);
        return new SeedResult(totalOrders, batch.size(), range);
    }

    /** Order BC 事件：携带 userId（与真实管道一致） */
    private BusinessActivity orderEvent(String eventType, String topic, long orderId, long userId,
                                         String corrKeys, Instant occurredAt, Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        return BusinessActivity.of(eventId, eventType, topic, orderId, userId, corrKeys,
            toJson(payload), occurredAt, occurredAt.plusSeconds(1));
    }

    /** 非 Order BC 事件：userId 为 null（与真实管道一致） */
    private BusinessActivity nonOrderEvent(String eventType, String topic, long orderId,
                                            String corrKeys, Instant occurredAt, Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        return BusinessActivity.of(eventId, eventType, topic, orderId, null, corrKeys,
            toJson(payload), occurredAt, occurredAt.plusSeconds(1));
    }

    /** 构建订单 items：无服务时为普通 item；有镭雕/服务时附加 serviceAttributes（对齐 ontology） */
    private List<Map<String, Object>> buildItemsList(long skuId, long spuId, int qty, int priceCents,
                                                      boolean hasServiceOrEngraving, ThreadLocalRandom rng) {
        if (!hasServiceOrEngraving) {
            return List.of(Map.<String, Object>of(
                "skuId", skuId, "spuId", spuId, "quantity", qty, "unitPriceCents", priceCents));
        }
        Object[] sample = SAMPLE_ENGRAVING[rng.nextInt(SAMPLE_ENGRAVING.length)];
        Map<String, Object> serviceAttributes = Map.of(
            "engravingPatternId", sample[0],
            "engravingPatternName", sample[1],
            "engravingText", sample[2]);
        return List.of(Map.<String, Object>of(
            "skuId", skuId, "spuId", spuId, "quantity", qty, "unitPriceCents", priceCents,
            "serviceAttributes", serviceAttributes));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
