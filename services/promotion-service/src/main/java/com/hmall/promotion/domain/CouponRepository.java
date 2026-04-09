package com.hmall.promotion.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    List<Coupon> saveAll(List<Coupon> coupons);

    Optional<Coupon> findById(Long id);

    List<Coupon> findByUserId(long userId);

    List<Coupon> findByUserIdAndStatus(long userId, CouponStatus status);

    long countByTemplateIdAndUserId(long templateId, long userId);

    /** 批量统计用户在指定模板集合中各自已领数量，避免 N+1 查询。 */
    Map<Long, Long> countByUserIdAndTemplateIds(long userId, List<Long> templateIds);

    List<Coupon> findByStatusAndExpiresAtBefore(CouponStatus status, Instant cutoff);
}
