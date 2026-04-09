package com.hmall.promotion.infrastructure;

import com.hmall.promotion.domain.Coupon;
import com.hmall.promotion.domain.CouponRepository;
import com.hmall.promotion.domain.CouponStatus;
import com.hmall.promotion.infrastructure.persistence.CouponEntity;
import com.hmall.promotion.infrastructure.persistence.CouponJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository jpaRepository;

    public CouponRepositoryImpl(CouponJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity entity = CouponEntity.fromDomain(coupon);
        CouponEntity saved = jpaRepository.save(entity);
        coupon.setId(saved.getId());
        return coupon;
    }

    @Override
    public List<Coupon> saveAll(List<Coupon> coupons) {
        List<CouponEntity> entities = coupons.stream()
                .map(CouponEntity::fromDomain)
                .toList();
        List<CouponEntity> saved = jpaRepository.saveAll(entities);
        for (int i = 0; i < coupons.size(); i++) {
            coupons.get(i).setId(saved.get(i).getId());
        }
        return coupons;
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return jpaRepository.findById(id).map(CouponEntity::toDomain);
    }

    @Override
    public List<Coupon> findByUserId(long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(CouponEntity::toDomain)
                .toList();
    }

    @Override
    public List<Coupon> findByUserIdAndStatus(long userId, CouponStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status).stream()
                .map(CouponEntity::toDomain)
                .toList();
    }

    @Override
    public long countByTemplateIdAndUserId(long templateId, long userId) {
        return jpaRepository.countByTemplateIdAndUserId(templateId, userId);
    }

    @Override
    public Map<Long, Long> countByUserIdAndTemplateIds(long userId, List<Long> templateIds) {
        List<Object[]> rows = jpaRepository.countByUserIdAndTemplateIdIn(userId, templateIds);
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public List<Coupon> findByStatusAndExpiresAtBefore(CouponStatus status, Instant cutoff) {
        return jpaRepository.findByStatusAndExpiresAtBefore(status, cutoff).stream()
                .map(CouponEntity::toDomain)
                .toList();
    }
}
