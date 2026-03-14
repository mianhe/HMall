package com.hmall.activity.infrastructure;

import com.hmall.activity.domain.ActivityRepository;
import com.hmall.activity.domain.BusinessActivity;
import com.hmall.activity.infrastructure.persistence.BusinessActivityEntity;
import com.hmall.activity.infrastructure.persistence.BusinessActivityJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ActivityRepositoryImpl implements ActivityRepository {

    private final BusinessActivityJpaRepository jpaRepository;

    public ActivityRepositoryImpl(BusinessActivityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BusinessActivity save(BusinessActivity activity) {
        BusinessActivityEntity entity = BusinessActivityEntity.from(activity);
        entity = jpaRepository.save(entity);
        return entity.toDomain();
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public List<BusinessActivity> findByOrderId(Long orderId, int limit) {
        return jpaRepository.findByOrderIdOrderByOccurredAtAsc(orderId, PageRequest.of(0, limit))
            .stream()
            .map(BusinessActivityEntity::toDomain)
            .toList();
    }

    @Override
    public List<BusinessActivity> findByUserId(Long userId, int limit) {
        return jpaRepository.findByUserIdOrderByOccurredAtAsc(userId, PageRequest.of(0, limit))
            .stream()
            .map(BusinessActivityEntity::toDomain)
            .toList();
    }

    @Override
    public List<BusinessActivity> findByCorrelationKey(String keyName, Long keyValue, int limit) {
        String v = keyValue.toString();
        String p1 = "%\"" + keyName + "\":[" + v + "]%";
        String p2 = "%\"" + keyName + "\":[" + v + ",%";
        String p3 = "%," + v + ",%";
        String p4 = "%," + v + "]%";
        return jpaRepository.findByCorrelationKeyPatterns(p1, p2, p3, p4, PageRequest.of(0, limit))
            .stream()
            .map(BusinessActivityEntity::toDomain)
            .toList();
    }

    @Override
    public List<BusinessActivity> findRecent(int limit) {
        return jpaRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "occurredAt")))
            .getContent()
            .stream()
            .map(BusinessActivityEntity::toDomain)
            .toList();
    }

    @Override
    public Map<String, Long> countByEventTypeInRange(Instant from, Instant to) {
        List<Object[]> rows = jpaRepository.countGroupByEventType(from, to);
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public List<String> findPayloadsByEventTypeInRange(String eventType, Instant from, Instant to) {
        return jpaRepository.findPayloadsByEventTypeInRange(eventType, from, to);
    }

    @Override
    public Map<LocalDate, Map<String, Long>> countByEventTypeGroupByDay(Instant from, Instant to) {
        List<Object[]> rows = jpaRepository.countGroupByDayAndEventType(from, to);
        Map<LocalDate, Map<String, Long>> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = row[0] instanceof java.sql.Date
                ? ((java.sql.Date) row[0]).toLocalDate()
                : (LocalDate) row[0];
            String eventType = (String) row[1];
            long count = ((Number) row[2]).longValue();
            result.computeIfAbsent(day, k -> new HashMap<>()).put(eventType, count);
        }
        return result;
    }

    @Override
    public Map<LocalDate, List<String>> findPaymentPayloadsByDay(Instant from, Instant to) {
        List<Object[]> rows = jpaRepository.findPaymentPayloadsByDay(from, to);
        Map<LocalDate, List<String>> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = row[0] instanceof java.sql.Date
                ? ((java.sql.Date) row[0]).toLocalDate()
                : (LocalDate) row[0];
            String payload = (String) row[1];
            result.computeIfAbsent(day, k -> new ArrayList<>()).add(payload);
        }
        return result;
    }

    private static final List<String> BUYER_EVENT_TYPES = List.of("OrderCreated");

    @Override
    public long countDistinctBuyers(Instant from, Instant to) {
        return jpaRepository.countDistinctUserIdByEventTypesInRange(BUYER_EVENT_TYPES, from, to);
    }

    @Override
    public Map<LocalDate, Long> countDistinctBuyersByDay(Instant from, Instant to) {
        List<Object[]> rows = jpaRepository.countDistinctUserIdByEventTypesGroupByDay(BUYER_EVENT_TYPES, from, to);
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = row[0] instanceof java.sql.Date
                ? ((java.sql.Date) row[0]).toLocalDate()
                : (LocalDate) row[0];
            long count = ((Number) row[1]).longValue();
            result.put(day, count);
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteByOrderId(Long orderId) {
        jpaRepository.deleteByOrderId(orderId);
    }

    @Override
    @Transactional
    public void deleteAll() {
        jpaRepository.deleteAllInBatch();
    }

    @Override
    @Transactional
    public void deleteBySeedBatch(String seedBatch) {
        jpaRepository.deleteBySeedBatch(seedBatch);
    }

    @Override
    @Transactional
    public void deleteAllSeedData() {
        jpaRepository.deleteAllSeedData();
    }

    @Override
    @Transactional
    public void deleteSeedDataInRange(Instant from, Instant to) {
        jpaRepository.deleteSeedDataInRange(from, to);
    }

    @Override
    public List<Object[]> findSeedBatchSummaries() {
        return jpaRepository.findSeedBatchSummaries();
    }

    @Override
    public List<Long> findDistinctOrderIds() {
        return jpaRepository.findDistinctOrderIds();
    }
}
