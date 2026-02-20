package com.hmall.activity.application;

import com.hmall.activity.domain.ActivityRepository;
import com.hmall.activity.domain.ActivityStats;
import com.hmall.activity.domain.BusinessActivity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class ActivityApplicationService {

    private final ActivityRepository repository;

    public ActivityApplicationService(ActivityRepository repository) {
        this.repository = repository;
    }

    public List<BusinessActivity> listByOrderId(Long orderId, int limit) {
        return repository.findByOrderId(orderId, limit);
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

        return new ActivityStats(
            counts.getOrDefault("OrderCreated", 0L).intValue(),
            counts.getOrDefault("OrderCancelled", 0L).intValue(),
            counts.getOrDefault("OrderCompleted", 0L).intValue(),
            (int) ps, (int) pf, (int) pe,
            counts.getOrDefault("StockReserved", 0L).intValue(),
            counts.getOrDefault("StockReleased", 0L).intValue()
        );
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
            command.payload(),
            command.occurredAt(),
            Instant.now()
        );
        repository.save(activity);
    }
}
