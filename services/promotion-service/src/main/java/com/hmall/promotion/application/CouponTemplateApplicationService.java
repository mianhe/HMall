package com.hmall.promotion.application;

import com.hmall.promotion.domain.CouponTemplate;
import com.hmall.promotion.domain.CouponTemplateRepository;
import com.hmall.promotion.domain.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CouponTemplateApplicationService {

    private final CouponTemplateRepository repository;

    public CouponTemplateApplicationService(CouponTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CouponTemplate create(CreateCouponTemplateCommand cmd) {
        validate(cmd);
        CouponTemplate template = new CouponTemplate(
                cmd.name(),
                cmd.type(),
                cmd.thresholdCents(),
                cmd.discountCents(),
                cmd.discountRate(),
                cmd.maxDiscountCents(),
                cmd.totalQuantity(),
                cmd.perUserLimit(),
                cmd.validDays(),
                cmd.targetingRule()
        );
        return repository.save(template);
    }

    @Transactional(readOnly = true)
    public Page<CouponTemplate> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public CouponTemplate getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("券模板不存在: " + id));
    }

    @Transactional
    public CouponTemplate deactivate(Long id) {
        CouponTemplate template = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("券模板不存在: " + id));
        template.deactivate();
        return repository.save(template);
    }

    private void validate(CreateCouponTemplateCommand cmd) {
        if (cmd.thresholdCents() < 0) {
            throw new PromotionBadRequestException("门槛金额不能为负数");
        }
        if (cmd.type() == CouponType.AMOUNT_OFF) {
            if (cmd.discountCents() == null || cmd.discountCents() <= 0) {
                throw new PromotionBadRequestException("满减券的优惠金额必须大于0");
            }
        }
        if (cmd.type() == CouponType.PERCENTAGE_OFF) {
            if (cmd.discountRate() == null
                    || cmd.discountRate().compareTo(BigDecimal.ZERO) <= 0
                    || cmd.discountRate().compareTo(BigDecimal.ONE) >= 0) {
                throw new PromotionBadRequestException("折扣率必须在 (0, 1) 范围内");
            }
        }
        if (cmd.totalQuantity() <= 0) {
            throw new PromotionBadRequestException("发放总量必须大于0");
        }
        if (cmd.perUserLimit() <= 0) {
            throw new PromotionBadRequestException("每人限领数量必须大于0");
        }
        if (cmd.validDays() <= 0) {
            throw new PromotionBadRequestException("有效天数必须大于0");
        }
    }

    public record CreateCouponTemplateCommand(
            String name,
            CouponType type,
            long thresholdCents,
            Long discountCents,
            BigDecimal discountRate,
            Long maxDiscountCents,
            int totalQuantity,
            int perUserLimit,
            int validDays,
            CouponTemplate.TargetingRule targetingRule
    ) {
    }
}
