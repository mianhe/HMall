package com.hmall.promotion.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PromotionActivityRepository {

    PromotionActivity save(PromotionActivity activity);

    Optional<PromotionActivity> findById(Long id);

    Page<PromotionActivity> findAll(Pageable pageable);

    List<PromotionActivity> findByStatus(PromotionActivityStatus status);
}
