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
import java.util.HashMap;
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
    @Transactional
    public void deleteByOrderId(Long orderId) {
        jpaRepository.deleteByOrderId(orderId);
    }

    @Override
    @Transactional
    public void deleteAll() {
        jpaRepository.deleteAllInBatch();
    }
}
