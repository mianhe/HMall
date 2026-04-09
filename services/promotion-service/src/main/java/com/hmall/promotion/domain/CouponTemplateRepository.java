package com.hmall.promotion.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CouponTemplateRepository {

    CouponTemplate save(CouponTemplate template);

    Optional<CouponTemplate> findById(Long id);

    Page<CouponTemplate> findAll(Pageable pageable);

    List<CouponTemplate> findByStatus(TemplateStatus status);
}
