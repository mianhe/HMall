package com.hmall.promotion.infrastructure.persistence;

import com.hmall.promotion.domain.PromotionActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionActivityJpaRepository extends JpaRepository<PromotionActivityEntity, Long> {

    List<PromotionActivityEntity> findByStatus(PromotionActivityStatus status);
}
