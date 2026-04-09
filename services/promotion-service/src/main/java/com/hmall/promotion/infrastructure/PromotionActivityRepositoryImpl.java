package com.hmall.promotion.infrastructure;

import com.hmall.promotion.domain.PromotionActivity;
import com.hmall.promotion.domain.PromotionActivityRepository;
import com.hmall.promotion.domain.PromotionActivityStatus;
import com.hmall.promotion.infrastructure.persistence.PromotionActivityEntity;
import com.hmall.promotion.infrastructure.persistence.PromotionActivityJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PromotionActivityRepositoryImpl implements PromotionActivityRepository {

    private final PromotionActivityJpaRepository jpaRepository;

    public PromotionActivityRepositoryImpl(PromotionActivityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PromotionActivity save(PromotionActivity activity) {
        PromotionActivityEntity entity = PromotionActivityEntity.fromDomain(activity);
        PromotionActivityEntity saved = jpaRepository.save(entity);
        activity.setId(saved.getId());
        return activity;
    }

    @Override
    public Optional<PromotionActivity> findById(Long id) {
        return jpaRepository.findById(id).map(PromotionActivityEntity::toDomain);
    }

    @Override
    public Page<PromotionActivity> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(PromotionActivityEntity::toDomain);
    }

    @Override
    public List<PromotionActivity> findByStatus(PromotionActivityStatus status) {
        return jpaRepository.findByStatus(status).stream().map(PromotionActivityEntity::toDomain).toList();
    }
}
