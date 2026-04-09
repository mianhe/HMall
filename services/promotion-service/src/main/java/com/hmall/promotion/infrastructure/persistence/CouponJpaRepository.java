package com.hmall.promotion.infrastructure.persistence;

import com.hmall.promotion.domain.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {

    List<CouponEntity> findByUserId(long userId);

    List<CouponEntity> findByUserIdAndStatus(long userId, CouponStatus status);

    long countByTemplateIdAndUserId(long templateId, long userId);

    @Query("SELECT c.templateId, COUNT(c) FROM CouponEntity c " +
           "WHERE c.userId = :userId AND c.templateId IN :templateIds " +
           "GROUP BY c.templateId")
    List<Object[]> countByUserIdAndTemplateIdIn(
            @Param("userId") long userId,
            @Param("templateIds") List<Long> templateIds);

    List<CouponEntity> findByStatusAndExpiresAtBefore(CouponStatus status, Instant cutoff);
}
