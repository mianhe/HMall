package com.hmall.activity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ActivityRepository {

    BusinessActivity save(BusinessActivity activity);

    boolean existsByEventId(String eventId);

    List<BusinessActivity> findByOrderId(Long orderId, int limit);

    List<BusinessActivity> findByUserId(Long userId, int limit);

    List<BusinessActivity> findByCorrelationKey(String keyName, Long keyValue, int limit);

    List<BusinessActivity> findRecent(int limit);

    Map<String, Long> countByEventTypeInRange(Instant from, Instant to);

    List<String> findPayloadsByEventTypeInRange(String eventType, Instant from, Instant to);

    Map<LocalDate, Map<String, Long>> countByEventTypeGroupByDay(Instant from, Instant to);

    Map<LocalDate, List<String>> findPaymentPayloadsByDay(Instant from, Instant to);

    long countDistinctBuyers(Instant from, Instant to);

    Map<LocalDate, Long> countDistinctBuyersByDay(Instant from, Instant to);

    void deleteByOrderId(Long orderId);

    void deleteAll();

    void deleteBySeedBatch(String seedBatch);

    void deleteAllSeedData();

    void deleteSeedDataInRange(Instant from, Instant to);

    List<Object[]> findSeedBatchSummaries();

    List<Long> findDistinctOrderIds();
}
