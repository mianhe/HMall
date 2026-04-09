package com.hmall.promotion.infrastructure.persistence;

import com.hmall.promotion.domain.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponTemplateJpaRepository extends JpaRepository<CouponTemplateEntity, Long> {

    List<CouponTemplateEntity> findByStatus(TemplateStatus status);
}
