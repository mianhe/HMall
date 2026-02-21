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

    @Query("SELECT e.eventType, COUNT(e) FROM BusinessActivityEntity e " +
           "WHERE e.occurredAt >= :from AND e.occurredAt < :to GROUP BY e.eventType")
    List<Object[]> countGroupByEventType(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT e.payload FROM BusinessActivityEntity e " +
           "WHERE e.eventType = :eventType AND e.occurredAt >= :from AND e.occurredAt < :to")
    List<String> findPayloadsByEventTypeInRange(@Param("eventType") String eventType,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    @Modifying
    @Query("DELETE FROM BusinessActivityEntity e WHERE e.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}
