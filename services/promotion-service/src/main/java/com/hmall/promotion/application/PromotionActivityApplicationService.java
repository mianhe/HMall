package com.hmall.promotion.application;

import com.hmall.promotion.domain.PromotionActivity;
import com.hmall.promotion.domain.PromotionActivityRepository;
import com.hmall.promotion.domain.PromotionActivityStatus;
import com.hmall.promotion.domain.PromotionActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class PromotionActivityApplicationService {

    private final PromotionActivityRepository repository;

    public PromotionActivityApplicationService(PromotionActivityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PromotionActivity create(CreatePromotionActivityCommand command) {
        Set<Long> targetSkuIds = command.targetSkuIds() == null ? Set.of() : new HashSet<>(command.targetSkuIds());
        int priority = command.priority() == null ? 0 : command.priority();
        PromotionActivity activity = new PromotionActivity(
                command.name(),
                command.type(),
                targetSkuIds,
                command.thresholdCents(),
                command.discountCents(),
                command.mutexGroupCode(),
                priority,
                command.startAt(),
                command.endAt(),
                command.targetingRule(),
                command.pieceRule());
        return repository.save(activity);
    }

    @Transactional(readOnly = true)
    public Page<PromotionActivity> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PromotionActivity getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PromotionBadRequestException("促销活动不存在: " + id));
    }

    @Transactional
    public PromotionActivity activate(Long id) {
        PromotionActivity activity = getById(id);
        activity.activate();
        return repository.save(activity);
    }

    @Transactional
    public PromotionActivity deactivate(Long id) {
        PromotionActivity activity = getById(id);
        activity.deactivate();
        return repository.save(activity);
    }

    public record CreatePromotionActivityCommand(
            String name,
            PromotionActivityType type,
            Set<Long> targetSkuIds,
            Long thresholdCents,
            long discountCents,
            String mutexGroupCode,
            Integer priority,
            Instant startAt,
            Instant endAt,
            PromotionActivity.TargetingRule targetingRule,
            PromotionActivity.PieceRule pieceRule
    ) {
    }
}
