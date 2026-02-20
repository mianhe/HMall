package com.hmall.activity.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ActivityRepository {

    BusinessActivity save(BusinessActivity activity);

    boolean existsByEventId(String eventId);

    List<BusinessActivity> findByOrderId(Long orderId, int limit);

    List<BusinessActivity> findRecent(int limit);

    Map<String, Long> countByEventTypeInRange(Instant from, Instant to);

    void deleteByOrderId(Long orderId);

    void deleteAll();
}
