package com.hmall.activity.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BusinessActivityJpaRepository extends JpaRepository<BusinessActivityEntity, Long> {

    boolean existsByEventId(String eventId);

    List<BusinessActivityEntity> findByOrderIdOrderByOccurredAtAsc(Long orderId, Pageable pageable);

    List<BusinessActivityEntity> findByUserIdOrderByOccurredAtAsc(Long userId, Pageable pageable);

    @Query("SELECT e FROM BusinessActivityEntity e WHERE e.correlationKeys IS NOT NULL AND " +
        "(e.correlationKeys LIKE :p1 OR e.correlationKeys LIKE :p2 OR e.correlationKeys LIKE :p3 OR e.correlationKeys LIKE :p4) " +
        "ORDER BY e.occurredAt ASC")
    List<BusinessActivityEntity> findByCorrelationKeyPatterns(@Param("p1") String p1, @Param("p2") String p2,
                                                             @Param("p3") String p3, @Param("p4") String p4,
                                                             Pageable pageable);

    @Query("SELECT e.eventType, COUNT(e) FROM BusinessActivityEntity e " +
           "WHERE e.occurredAt >= :from AND e.occurredAt < :to GROUP BY e.eventType")
    List<Object[]> countGroupByEventType(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT e.payload FROM BusinessActivityEntity e " +
           "WHERE e.eventType = :eventType AND e.occurredAt >= :from AND e.occurredAt < :to")
    List<String> findPayloadsByEventTypeInRange(@Param("eventType") String eventType,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    @Query(value = "SELECT CAST(occurred_at AS DATE) as d, event_type, COUNT(*) as cnt " +
           "FROM business_activity " +
           "WHERE occurred_at >= :from AND occurred_at < :to " +
           "GROUP BY CAST(occurred_at AS DATE), event_type " +
           "ORDER BY d", nativeQuery = true)
    List<Object[]> countGroupByDayAndEventType(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT CAST(occurred_at AS DATE) as d, payload " +
           "FROM business_activity " +
           "WHERE event_type = 'PaymentCompleted' AND occurred_at >= :from AND occurred_at < :to " +
           "ORDER BY d", nativeQuery = true)
    List<Object[]> findPaymentPayloadsByDay(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM BusinessActivityEntity e " +
           "WHERE e.eventType IN :eventTypes AND e.occurredAt >= :from AND e.occurredAt < :to AND e.userId IS NOT NULL")
    long countDistinctUserIdByEventTypesInRange(@Param("eventTypes") List<String> eventTypes,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    @Query(value = "SELECT CAST(occurred_at AS DATE) as d, COUNT(DISTINCT user_id) as cnt " +
           "FROM business_activity " +
           "WHERE event_type IN :eventTypes AND occurred_at >= :from AND occurred_at < :to AND user_id IS NOT NULL " +
           "GROUP BY CAST(occurred_at AS DATE) ORDER BY d", nativeQuery = true)
    List<Object[]> countDistinctUserIdByEventTypesGroupByDay(@Param("eventTypes") List<String> eventTypes,
                                                              @Param("from") Instant from,
                                                              @Param("to") Instant to);

    @Modifying
    @Query("DELETE FROM BusinessActivityEntity e WHERE e.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    @Modifying
    @Query("DELETE FROM BusinessActivityEntity e WHERE e.seedBatch = :seedBatch")
    void deleteBySeedBatch(@Param("seedBatch") String seedBatch);

    @Modifying
    @Query("DELETE FROM BusinessActivityEntity e WHERE e.seedBatch IS NOT NULL")
    void deleteAllSeedData();

    @Modifying
    @Query("DELETE FROM BusinessActivityEntity e WHERE e.seedBatch IS NOT NULL AND e.occurredAt >= :from AND e.occurredAt < :to")
    void deleteSeedDataInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT e.seedBatch, COUNT(e), COUNT(DISTINCT e.orderId), MIN(e.occurredAt), MAX(e.occurredAt) " +
           "FROM BusinessActivityEntity e WHERE e.seedBatch IS NOT NULL GROUP BY e.seedBatch ORDER BY MAX(e.occurredAt) DESC")
    List<Object[]> findSeedBatchSummaries();

    @Query("SELECT DISTINCT e.orderId FROM BusinessActivityEntity e WHERE e.orderId IS NOT NULL")
    List<Long> findDistinctOrderIds();
}
