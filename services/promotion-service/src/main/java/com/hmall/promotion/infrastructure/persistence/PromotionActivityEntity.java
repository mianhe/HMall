package com.hmall.promotion.infrastructure.persistence;

import com.hmall.promotion.domain.PromotionActivity;
import com.hmall.promotion.domain.PromotionActivityStatus;
import com.hmall.promotion.domain.PromotionActivityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "promotion_activities")
public class PromotionActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionActivityType type;

    @Column(length = 1000)
    private String targetSkuIdsCsv;

    private Long thresholdCents;

    @Column(nullable = false)
    private Long discountCents;

    private String mutexGroupCode;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Column(length = 255)
    private String targetingLevelsCsv;

    @Column(length = 1000)
    private String targetingTagsAnyCsv;

    @Column(length = 1000)
    private String targetingTagsAllCsv;

    @Column(length = 1000)
    private String targetingExcludeTagsCsv;

    @Column(length = 32)
    private String pieceScopeType;

    @Column(length = 1000)
    private String pieceScopeIdsCsv;

    private Integer pieceMinQuantity;

    @Column(length = 32)
    private String pieceDiscountType;

    private Long pieceDiscountValue;

    private Long pieceMaxDiscountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionActivityStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PromotionActivityEntity() {
    }

    public static PromotionActivityEntity fromDomain(PromotionActivity activity) {
        PromotionActivityEntity e = new PromotionActivityEntity();
        e.id = activity.getId();
        e.name = activity.getName();
        e.type = activity.getType();
        e.targetSkuIdsCsv = activity.getTargetSkuIds().isEmpty()
                ? null
                : activity.getTargetSkuIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        e.thresholdCents = activity.getThresholdCents();
        e.discountCents = activity.getDiscountCents();
        e.mutexGroupCode = activity.getMutexGroupCode();
        e.priority = activity.getPriority();
        e.startAt = activity.getStartAt();
        e.endAt = activity.getEndAt();
        e.targetingLevelsCsv = toCsv(activity.getTargetingRule() == null ? Set.of() : activity.getTargetingRule().levelsIn());
        e.targetingTagsAnyCsv = toCsv(activity.getTargetingRule() == null ? Set.of() : activity.getTargetingRule().tagsAny());
        e.targetingTagsAllCsv = toCsv(activity.getTargetingRule() == null ? Set.of() : activity.getTargetingRule().tagsAll());
        e.targetingExcludeTagsCsv = toCsv(activity.getTargetingRule() == null ? Set.of() : activity.getTargetingRule().excludeTags());
        if (activity.getPieceRule() != null) {
            e.pieceScopeType = activity.getPieceRule().scopeType();
            e.pieceScopeIdsCsv = toCsv(activity.getPieceRule().scopeIds());
            e.pieceMinQuantity = activity.getPieceRule().minQuantity();
            e.pieceDiscountType = activity.getPieceRule().discountType();
            e.pieceDiscountValue = activity.getPieceRule().discountValue();
            e.pieceMaxDiscountCents = activity.getPieceRule().maxDiscountCents();
        }
        e.status = activity.getStatus();
        e.createdAt = activity.getCreatedAt();
        e.updatedAt = activity.getUpdatedAt();
        return e;
    }

    public PromotionActivity toDomain() {
        Set<Long> targetSkuIds = (targetSkuIdsCsv == null || targetSkuIdsCsv.isBlank())
                ? Set.of()
                : Arrays.stream(targetSkuIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        PromotionActivity.TargetingRule targetingRule = null;
        Set<String> levels = toStringSet(targetingLevelsCsv);
        Set<String> tagsAny = toStringSet(targetingTagsAnyCsv);
        Set<String> tagsAll = toStringSet(targetingTagsAllCsv);
        Set<String> excludeTags = toStringSet(targetingExcludeTagsCsv);
        if (!levels.isEmpty() || !tagsAny.isEmpty() || !tagsAll.isEmpty() || !excludeTags.isEmpty()) {
            targetingRule = new PromotionActivity.TargetingRule(levels, tagsAny, tagsAll, excludeTags);
        }

        PromotionActivity.PieceRule pieceRule = null;
        if (pieceScopeType != null || pieceMinQuantity != null || pieceDiscountType != null || pieceDiscountValue != null) {
            pieceRule = new PromotionActivity.PieceRule(
                    pieceScopeType,
                    toLongSet(pieceScopeIdsCsv),
                    pieceMinQuantity,
                    pieceDiscountType,
                    pieceDiscountValue,
                    pieceMaxDiscountCents
            );
        }

        return new PromotionActivity(
                id,
                name,
                type,
                targetSkuIds,
                thresholdCents,
                discountCents,
                mutexGroupCode,
                priority,
                startAt,
                endAt,
                targetingRule,
                pieceRule,
                status,
                createdAt,
                updatedAt
        );
    }

    public Long getId() {
        return id;
    }

    private static String toCsv(Set<?> values) {
        return values == null || values.isEmpty()
                ? null
                : values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static Set<String> toStringSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private static Set<Long> toLongSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }
}
