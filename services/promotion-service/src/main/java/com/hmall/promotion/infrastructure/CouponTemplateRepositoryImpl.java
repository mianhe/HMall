package com.hmall.promotion.infrastructure;

import com.hmall.promotion.domain.CouponTemplate;
import com.hmall.promotion.domain.CouponTemplateRepository;
import com.hmall.promotion.domain.TemplateStatus;
import com.hmall.promotion.infrastructure.persistence.CouponTemplateEntity;
import com.hmall.promotion.infrastructure.persistence.CouponTemplateJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CouponTemplateRepositoryImpl implements CouponTemplateRepository {

    private final CouponTemplateJpaRepository jpaRepository;

    public CouponTemplateRepositoryImpl(CouponTemplateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CouponTemplate save(CouponTemplate template) {
        CouponTemplateEntity entity = CouponTemplateEntity.fromDomain(template);
        CouponTemplateEntity saved = jpaRepository.save(entity);
        template.setId(saved.getId());
        return template;
    }

    @Override
    public Optional<CouponTemplate> findById(Long id) {
        return jpaRepository.findById(id).map(CouponTemplateEntity::toDomain);
    }

    @Override
    public Page<CouponTemplate> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(CouponTemplateEntity::toDomain);
    }

    @Override
    public List<CouponTemplate> findByStatus(TemplateStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(CouponTemplateEntity::toDomain)
                .toList();
    }
}
