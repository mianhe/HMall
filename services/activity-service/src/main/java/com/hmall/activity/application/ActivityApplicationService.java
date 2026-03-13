package com.hmall.activity.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.activity.domain.ActivityRepository;
import com.hmall.activity.domain.ActivityStats;
import com.hmall.activity.domain.BusinessActivity;
import com.hmall.activity.domain.DailyActivityStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ActivityApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ActivityApplicationService.class);

    private final ActivityRepository repository;
    private final ObjectMapper objectMapper;

    public ActivityApplicationService(ActivityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
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
    }
}
